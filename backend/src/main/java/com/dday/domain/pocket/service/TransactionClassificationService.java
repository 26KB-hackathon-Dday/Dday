package com.dday.domain.pocket.service;

import com.dday.domain.mydata.entity.*;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.pocket.dto.PocketErrorCode;
import com.dday.domain.pocket.dto.request.TransactionClassificationRequest;
import com.dday.domain.pocket.dto.response.*;
import com.dday.domain.pocket.entity.*;
import com.dday.domain.pocket.repository.*;
import com.dday.domain.user.dto.UserErrorCode;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 금융 거래의 자동 분류와 사용자 수동 분류를 담당한다.
 *
 * <p>자동 분류는 사용자가 저장한 가맹점 규칙을 최우선으로 적용한다. 현재 MyData 원본에는
 * 업종 카테고리가 없고 내부 가맹점 기준정보도 없으므로, 근거를 추측하지 않고 규칙이 없으면
 * 자유 포켓으로 보낸다. 이후 해당 데이터가 추가되면 사용자 규칙과 기본값 사이에 단계를 끼운다.
 */
@Service
@RequiredArgsConstructor
public class TransactionClassificationService {

    private final UserRepository userRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final PocketRepository pocketRepository;
    private final CategoryRepository categoryRepository;
    private final UserMerchantRuleRepository merchantRuleRepository;
    private final TransactionClassificationHistoryRepository historyRepository;

    /**
     * 로그인 사용자의 미분류 정상 소비 거래를 일괄 분류한다.
     * Repository 쿼리 자체가 수입·본인 이체·취소·환불·수동 분류 거래를 제외한다.
     */
    @Transactional
    public AutoClassificationResponse classifyUnclassified(Long userId) {
        requireActiveUser(userId);
        Pocket freePocket = pocketRepository.findByUserUserIdAndPocketType(userId, PocketType.FREE)
                .orElseThrow(() -> new BusinessException(PocketErrorCode.POCKET_NOT_FOUND));
        List<FinancialTransaction> targets = transactionRepository.findUnclassifiedExpenses(userId);
        int userRuleCount = 0;
        int defaultFreeCount = 0;

        for (FinancialTransaction transaction : targets) {
            Optional<UserMerchantRule> matched = findRule(userId, transaction);
            if (matched.isPresent() && matched.get().getCategory().isActive()) {
                UserMerchantRule rule = matched.get();
                applyAutomatic(transaction, rule.getCategory(), rule.getPocket(),
                        ClassificationSource.USER_RULE);
                userRuleCount++;
            } else {
                // 근거 없는 카테고리를 만들어내지 않고 포켓만 자유로 지정한다.
                applyAutomatic(transaction, null, freePocket, ClassificationSource.DEFAULT_FREE);
                defaultFreeCount++;
            }
        }
        return AutoClassificationResponse.builder()
                .targetCount(targets.size())
                .userRuleCount(userRuleCount)
                .defaultFreeCount(defaultFreeCount)
                .remainingUnclassifiedCount(0)
                .build();
    }

    /**
     * 거래 한 건을 사용자가 선택한 포켓·카테고리로 변경한다.
     * 거래 잠금, 값 검증, 변경 이력, 향후 규칙 저장이 하나의 트랜잭션에서 처리된다.
     */
    @Transactional
    public TransactionClassificationResponse classifyManually(
            Long userId, Long transactionId, TransactionClassificationRequest request) {
        User user = requireActiveUser(userId);
        requireSpendingPocket(request.getPocketType());
        FinancialTransaction transaction = transactionRepository
                .findForClassification(userId, transactionId)
                .orElseThrow(() -> new BusinessException(PocketErrorCode.TRANSACTION_NOT_FOUND));
        validateTarget(transaction);
        Pocket pocket = pocketRepository.findByUserUserIdAndPocketType(userId, request.getPocketType())
                .orElseThrow(() -> new BusinessException(PocketErrorCode.POCKET_NOT_FOUND));
        Category category = categoryRepository.findByCategoryIdAndActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(PocketErrorCode.CATEGORY_NOT_FOUND));
        if (category.getDefaultPocketType() != request.getPocketType()) {
            throw new BusinessException(PocketErrorCode.CATEGORY_POCKET_MISMATCH);
        }

        Pocket previousPocket = transaction.getPocket();
        Category previousCategory = transaction.getCategory();
        // 값이 우연히 같아도 사용자가 확인한 순간부터 MANUAL_CLASSIFIED로 보호해야 한다.
        transaction.classifyManually(category, pocket);
        historyRepository.save(TransactionClassificationHistory.builder()
                .financialTransaction(transaction)
                .previousCategory(previousCategory)
                .changedCategory(category)
                .previousPocket(previousPocket)
                .changedPocket(pocket)
                .changedBy(ClassificationChangedBy.USER)
                .applyFutureRule(request.isApplyFutureRule())
                .build());
        boolean futureRuleCreated = request.isApplyFutureRule()
                && saveFutureRule(userId, user, transaction, category, pocket);
        return TransactionClassificationResponse.builder()
                .transactionId(transactionId)
                .previous(ClassificationValueResponse.of(previousPocket, previousCategory))
                .current(ClassificationValueResponse.of(pocket, category))
                .futureRuleCreated(futureRuleCreated)
                .classifiedAt(LocalDateTime.now())
                .build();
    }

    private void applyAutomatic(FinancialTransaction transaction, Category category, Pocket pocket,
                                ClassificationSource source) {
        Category previousCategory = transaction.getCategory();
        Pocket previousPocket = transaction.getPocket();
        transaction.classifyAutomatically(category, pocket, source);
        historyRepository.save(TransactionClassificationHistory.builder()
                .financialTransaction(transaction)
                .previousCategory(previousCategory)
                .changedCategory(category)
                .previousPocket(previousPocket)
                .changedPocket(pocket)
                .changedBy(ClassificationChangedBy.SYSTEM)
                .applyFutureRule(false)
                .build());
    }

    private Optional<UserMerchantRule> findRule(Long userId, FinancialTransaction transaction) {
        String regnoKey = merchantKey(transaction.getMerchantRegno(), null);
        if (regnoKey != null) {
            Optional<UserMerchantRule> byRegno = merchantRuleRepository.findRule(userId, regnoKey);
            if (byRegno.isPresent()) return byRegno;
        }
        String nameKey = merchantKey(null, transaction.getMerchantName());
        return nameKey == null ? Optional.empty() : merchantRuleRepository.findRule(userId, nameKey);
    }

    private boolean saveFutureRule(Long userId, User user, FinancialTransaction transaction,
                                   Category category, Pocket pocket) {
        String key = merchantKey(transaction.getMerchantRegno(), transaction.getMerchantName());
        if (key == null) return false;
        UserMerchantRule rule = merchantRuleRepository.findRule(userId, key)
                .orElseGet(() -> UserMerchantRule.builder()
                        .user(user)
                        .merchantKey(key)
                        .merchantRegno(blankToNull(transaction.getMerchantRegno()))
                        .merchantName(blankToNull(transaction.getMerchantName()))
                        .category(category)
                        .pocket(pocket)
                        .build());
        rule.redirectTo(category, pocket);
        merchantRuleRepository.save(rule);
        return true;
    }

    /** 사업자번호가 있으면 이름보다 안정적이므로 언제나 우선해서 키로 사용한다. */
    String merchantKey(String registrationNumber, String merchantName) {
        String regno = blankToNull(registrationNumber);
        if (regno != null) {
            String digits = regno.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) return "REGNO:" + digits;
        }
        String name = blankToNull(merchantName);
        if (name == null) return null;
        return "NAME:" + name.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private void validateTarget(FinancialTransaction transaction) {
        if (transaction.getTransactionType() != TransactionType.EXPENSE
                || transaction.getTransactionStatus() != TransactionStatus.NORMAL) {
            throw new BusinessException(PocketErrorCode.INVALID_CLASSIFICATION_TARGET);
        }
    }

    private void requireSpendingPocket(PocketType pocketType) {
        if (pocketType != PocketType.ESSENTIAL && pocketType != PocketType.FREE) {
            throw new BusinessException(PocketErrorCode.INVALID_POCKET_TYPE);
        }
    }

    private User requireActiveUser(Long userId) {
        return userRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}

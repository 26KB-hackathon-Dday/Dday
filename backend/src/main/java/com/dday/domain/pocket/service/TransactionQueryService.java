package com.dday.domain.pocket.service;

import com.dday.domain.mydata.entity.ClassificationStatus;
import com.dday.domain.mydata.entity.FinancialTransaction;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.pocket.dto.PocketErrorCode;
import com.dday.domain.pocket.dto.response.TransactionDetailResponse;
import com.dday.domain.pocket.dto.response.TransactionListItemResponse;
import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.repository.CategoryRepository;
import com.dday.domain.pocket.repository.PocketRepository;
import com.dday.global.common.dto.PageResponse;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** 포켓 소비 거래의 목록과 단건 상세를 읽기 전용으로 제공한다. */
@Service
@RequiredArgsConstructor
public class TransactionQueryService {

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM").withResolverStyle(ResolverStyle.STRICT);

    private final PocketRepository pocketRepository;
    private final CategoryRepository categoryRepository;
    private final FinancialTransactionRepository transactionRepository;

    /**
     * 필수·자유 포켓의 월별 거래를 선택 필터와 함께 페이지로 조회한다.
     *
     * <p>포켓은 반드시 로그인 사용자의 것이어야 하며, {@code categoryId}와
     * {@code classificationStatus}는 값이 있을 때만 조회 조건에 적용된다. 같은 시각의 거래가
     * 여러 건이어도 페이지 순서가 흔들리지 않도록 거래 시각 뒤에 PK 역순을 보조 정렬로 둔다.
     */
    @Transactional(readOnly = true)
    public PageResponse<TransactionListItemResponse> findAll(
            Long userId, PocketType pocketType, String month, Long categoryId,
            ClassificationStatus classificationStatus, int page, int size) {
        validatePocketType(pocketType);
        validatePage(page, size);
        YearMonth yearMonth = parseMonth(month);
        Pocket pocket = pocketRepository.findByUserUserIdAndPocketType(userId, pocketType)
                .orElseThrow(() -> new BusinessException(PocketErrorCode.POCKET_NOT_FOUND));

        // 삭제·비활성화된 카테고리 ID로 조회 조건을 만드는 것을 요청 단계에서 차단한다.
        if (categoryId != null && !categoryRepository.existsByCategoryIdAndActiveTrue(categoryId)) {
            throw new BusinessException(PocketErrorCode.CATEGORY_NOT_FOUND);
        }
        LocalDateTime from = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime to = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        // PK 보조 정렬은 동일 transactionAt을 가진 행 사이에서도 일관된 페이징을 보장한다.
        PageRequest pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("transactionAt"),
                Sort.Order.desc("financialTransactionId")));
        Page<FinancialTransaction> result = transactionRepository.findPocketPage(
                userId, pocket.getPocketId(), from, to, categoryId, classificationStatus, pageable);
        return PageResponse.of(result, TransactionListItemResponse::from);
    }

    /** 거래 ID뿐 아니라 소유 사용자까지 함께 조회해 다른 사용자의 거래 존재 여부를 감춘다. */
    @Transactional(readOnly = true)
    public TransactionDetailResponse findById(Long userId, Long transactionId) {
        if (transactionId == null || transactionId <= 0) {
            throw new BusinessException(PocketErrorCode.TRANSACTION_NOT_FOUND);
        }
        FinancialTransaction transaction = transactionRepository
                .findDetailByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new BusinessException(PocketErrorCode.TRANSACTION_NOT_FOUND));
        return TransactionDetailResponse.from(transaction);
    }

    private void validatePocketType(PocketType pocketType) {
        // 비상금과 미래자산은 소비 거래 목록의 대상이 아니다.
        if (pocketType != PocketType.ESSENTIAL && pocketType != PocketType.FREE) {
            throw new BusinessException(PocketErrorCode.INVALID_POCKET_TYPE);
        }
    }

    private void validatePage(int page, int size) {
        // 지나치게 큰 페이지 요청으로 인한 DB·응답 부하를 막는다.
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(PocketErrorCode.INVALID_PAGE_REQUEST);
        }
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month, MONTH_FORMATTER);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessException(PocketErrorCode.INVALID_MONTH);
        }
    }
}

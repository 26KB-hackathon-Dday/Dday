package com.dday.domain.pocket.service;

import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.pocket.dto.PocketErrorCode;
import com.dday.domain.pocket.dto.response.PocketCategoryUsageResponse;
import com.dday.domain.pocket.entity.Category;
import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.repository.CategoryRepository;
import com.dday.domain.pocket.repository.PocketRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 필수·자유 포켓의 월간 정상 소비를 카테고리 기준으로 집계한다. */
@Service
@RequiredArgsConstructor
public class PocketCategoryUsageService {

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM").withResolverStyle(ResolverStyle.STRICT);

    private final PocketRepository pocketRepository;
    private final CategoryRepository categoryRepository;
    private final FinancialTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public PocketCategoryUsageResponse findMonthly(Long userId, PocketType pocketType, String month) {
        validatePocketType(pocketType);
        YearMonth yearMonth = parseMonth(month);
        Pocket pocket = pocketRepository.findByUserUserIdAndPocketType(userId, pocketType)
                .orElseThrow(() -> new BusinessException(PocketErrorCode.POCKET_NOT_FOUND));

        LocalDateTime from = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime to = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        Map<Long, Long> usedByCategory = new HashMap<>();
        long unclassifiedUsedAmount = 0L;

        // categoryId가 null인 집계 행은 분류 전 거래다. 활성 카테고리 목록에는 섞지 않고
        // 별도 합계로 반환해 화면에서 누락 여부를 명확히 알 수 있게 한다.
        for (Object[] row : transactionRepository.sumSpendingByCategory(
                userId, pocket.getPocketId(), from, to)) {
            Long categoryId = (Long) row[0];
            long usedAmount = ((Number) row[1]).longValue();
            if (categoryId == null) {
                unclassifiedUsedAmount = usedAmount;
            } else {
                usedByCategory.put(categoryId, usedAmount);
            }
        }

        List<PocketCategoryUsageResponse.CategoryUsageResponse> categories =
                categoryRepository.findAllActive(pocketType).stream()
                        .map(category -> response(category,
                                usedByCategory.getOrDefault(category.getCategoryId(), 0L)))
                        .toList();
        long totalUsedAmount = categories.stream()
                .mapToLong(PocketCategoryUsageResponse.CategoryUsageResponse::getUsedAmount)
                .sum() + unclassifiedUsedAmount;

        return PocketCategoryUsageResponse.builder()
                .month(yearMonth.toString())
                .pocketType(pocketType)
                .totalUsedAmount(totalUsedAmount)
                .categories(categories)
                .unclassifiedUsedAmount(unclassifiedUsedAmount)
                .build();
    }

    private PocketCategoryUsageResponse.CategoryUsageResponse response(
            Category category, Long usedAmount) {
        return PocketCategoryUsageResponse.CategoryUsageResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .usedAmount(usedAmount)
                .build();
    }

    private void validatePocketType(PocketType pocketType) {
        if (pocketType != PocketType.ESSENTIAL && pocketType != PocketType.FREE) {
            throw new BusinessException(PocketErrorCode.INVALID_POCKET_TYPE);
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

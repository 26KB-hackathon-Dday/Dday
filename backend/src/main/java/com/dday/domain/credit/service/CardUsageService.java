package com.dday.domain.credit.service;

import com.dday.domain.credit.dto.response.CardUsageItemResponse;
import com.dday.domain.credit.dto.response.CardUsageResponse;
import com.dday.domain.credit.dto.response.CardUsageTrendResponse;
import com.dday.domain.mydata.entity.UserCard;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.mydata.repository.UserCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 카드 한도 대비 이용률을 계산한다.
 *
 * <p><b>한도가 있는 카드만 다룬다.</b> 체크·선불카드는 한도라는 개념이 없어 이용률을 낼 수
 * 없다. 사용액만 합계에 섞으면 이용률이 실제보다 부풀려진다.
 *
 * <p>인터페이스를 두지 않는다 (AGENTS.md §7).
 */
@Service
@RequiredArgsConstructor
public class CardUsageService {

    /** 추이를 보여줄 개월 수. 이번 달을 포함해 거슬러 올라간다. */
    private static final int TREND_MONTHS = 6;

    /** 이용률 소수 자릿수. */
    private static final int RATE_SCALE = 1;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final UserCardRepository cardRepository;
    private final FinancialTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public CardUsageResponse findUsage(Long userId) {
        return findUsage(userId, YearMonth.now());
    }

    /** 기준월을 받는 쪽. 테스트가 "이번 달"에 흔들리지 않도록 밖에서 넣는다. */
    CardUsageResponse findUsage(Long userId, YearMonth baseMonth) {
        List<UserCard> creditCards = cardRepository.findAllByUserUserId(userId).stream()
                .filter(card -> card.getCreditLimit() != null)
                .toList();
        if (creditCards.isEmpty()) {
            return CardUsageResponse.empty();
        }

        YearMonth from = baseMonth.minusMonths(TREND_MONTHS - 1L);
        List<Object[]> rows = transactionRepository.sumCardSpendingByMonth(
                userId, from.atDay(1).atStartOfDay(), baseMonth.plusMonths(1).atDay(1).atStartOfDay());

        /*
         * 쿼리가 이미 한도 있는 카드만 걸러 오지만 여기서 한 번 더 확인한다.
         * 이 검사가 없으면 쿼리 조건이 바뀌는 순간 체크카드 사용액이 합계에 조용히 섞이고,
         * 이용률이 실제보다 부풀려진다. 화면에 나오는 숫자라 조용히 틀리면 안 된다.
         */
        Set<Long> creditCardIds = creditCards.stream()
                .map(UserCard::getCardId)
                .collect(Collectors.toSet());

        // [월][카드] 두 갈래로 쓰이므로 한 번 훑으면서 둘 다 모아둔다.
        Map<YearMonth, Long> usageByMonth = new HashMap<>();
        Map<Long, Long> currentUsageByCard = new HashMap<>();
        for (Object[] row : rows) {
            YearMonth month = YearMonth.of((Integer) row[0], (Integer) row[1]);
            Long cardId = ((Number) row[2]).longValue();
            long amount = ((Number) row[3]).longValue();
            if (!creditCardIds.contains(cardId)) {
                continue;
            }

            usageByMonth.merge(month, amount, Long::sum);
            if (month.equals(baseMonth)) {
                currentUsageByCard.merge(cardId, amount, Long::sum);
            }
        }

        long totalLimit = creditCards.stream().mapToLong(UserCard::getCreditLimit).sum();
        long currentUsage = usageByMonth.getOrDefault(baseMonth, 0L);

        return CardUsageResponse.builder()
                .months(TREND_MONTHS)
                .currentMonth(baseMonth.toString())
                .totalCreditLimit(totalLimit)
                .currentUsage(currentUsage)
                .currentUtilization(utilization(currentUsage, totalLimit))
                .cards(toCards(creditCards, currentUsageByCard))
                .trend(toTrend(from, usageByMonth, totalLimit))
                .build();
    }

    private List<CardUsageItemResponse> toCards(List<UserCard> creditCards,
                                                Map<Long, Long> usageByCard) {
        List<CardUsageItemResponse> cards = new ArrayList<>();
        for (UserCard card : creditCards) {
            long usage = usageByCard.getOrDefault(card.getCardId(), 0L);
            cards.add(CardUsageItemResponse.builder()
                    .cardId(card.getCardId())
                    .cardName(card.getCardName())
                    .creditLimit(card.getCreditLimit())
                    .usage(usage)
                    .utilization(utilization(usage, card.getCreditLimit()))
                    .build());
        }
        return cards;
    }

    /** 거래가 없는 달도 0으로 채운다 — 빼면 그래프의 가로축이 들쭉날쭉해진다. */
    private List<CardUsageTrendResponse> toTrend(YearMonth from, Map<YearMonth, Long> usageByMonth,
                                                 long totalLimit) {
        List<CardUsageTrendResponse> trend = new ArrayList<>();
        for (int i = 0; i < TREND_MONTHS; i++) {
            YearMonth month = from.plusMonths(i);
            long usage = usageByMonth.getOrDefault(month, 0L);
            trend.add(CardUsageTrendResponse.builder()
                    .month(month.toString())
                    .usage(usage)
                    .utilization(utilization(usage, totalLimit))
                    .build());
        }
        return trend;
    }

    /** 한도가 0이면 나눌 수 없다. 그런 카드는 이용률이 없는 것이지 0%가 아니다. */
    private BigDecimal utilization(long usage, long limit) {
        if (limit <= 0) {
            return null;
        }
        return BigDecimal.valueOf(usage)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(limit), RATE_SCALE, RoundingMode.HALF_UP);
    }
}

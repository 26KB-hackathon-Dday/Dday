package com.dday.domain.credit.service;

import com.dday.domain.credit.dto.response.CardUsageItemResponse;
import com.dday.domain.credit.dto.response.CardUsageResponse;
import com.dday.domain.credit.dto.response.CardUsageTrendResponse;
import com.dday.domain.mydata.entity.CardType;
import com.dday.domain.mydata.entity.UserCard;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.mydata.repository.UserCardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CardUsageServiceTest {

    private static final Long USER_ID = 1L;
    private static final YearMonth BASE = YearMonth.of(2026, 9);

    @Mock
    private UserCardRepository cardRepository;

    @Mock
    private FinancialTransactionRepository transactionRepository;

    @InjectMocks
    private CardUsageService cardUsageService;

    private UserCard card(long cardId, String name, CardType type, Long creditLimit) {
        UserCard card = UserCard.builder()
                .orgCode("0306")
                .cardIdentifier("CARD-" + cardId)
                .cardName(name)
                .cardType(type)
                .creditLimit(creditLimit)
                .build();
        ReflectionTestUtils.setField(card, "cardId", cardId);
        return card;
    }

    /** 레포지토리가 돌려주는 집계 행: [연, 월, 카드 ID, 합계]. */
    private Object[] row(int year, int month, long cardId, long amount) {
        return new Object[]{year, month, cardId, amount};
    }

    private void givenCards(UserCard... cards) {
        given(cardRepository.findAllByUserUserId(USER_ID)).willReturn(List.of(cards));
    }

    private void givenSpending(List<Object[]> rows) {
        given(transactionRepository.sumCardSpendingByMonth(
                eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(rows);
    }

    @Test
    void 한도가_있는_카드만_이용률_계산에_넣는다() {
        // 체크카드는 한도라는 개념이 없다. 0원 한도가 아니라 계산 대상이 아니다.
        givenCards(
                card(1L, "신한카드 Deep Dream", CardType.CREDIT, 3_000_000L),
                card(2L, "현대카드 ZERO (체크)", CardType.CHECK, null));
        givenSpending(List.<Object[]>of(row(2026, 9, 1L, 900_000L), row(2026, 9, 2L, 400_000L)));

        CardUsageResponse result = cardUsageService.findUsage(USER_ID, BASE);

        assertThat(result.getCards()).extracting(CardUsageItemResponse::getCardName)
                .containsExactly("신한카드 Deep Dream");
        assertThat(result.getTotalCreditLimit()).isEqualTo(3_000_000L);
        // 체크카드 사용액 40만원은 합계에 들어가지 않는다.
        assertThat(result.getCurrentUsage()).isEqualTo(900_000L);
    }

    @Test
    void 카드별_이번달_사용액과_이용률을_계산한다() {
        givenCards(card(1L, "신한카드 Deep Dream", CardType.CREDIT, 3_000_000L));
        givenSpending(List.<Object[]>of(row(2026, 9, 1L, 900_000L)));

        CardUsageResponse result = cardUsageService.findUsage(USER_ID, BASE);

        CardUsageItemResponse card = result.getCards().get(0);
        assertThat(card.getCreditLimit()).isEqualTo(3_000_000L);
        assertThat(card.getUsage()).isEqualTo(900_000L);
        assertThat(card.getUtilization()).isEqualByComparingTo("30.0");
        assertThat(result.getCurrentMonth()).isEqualTo("2026-09");
        assertThat(result.getCurrentUtilization()).isEqualByComparingTo("30.0");
    }

    @Test
    void 최근_6개월_추이를_오래된_달부터_준다() {
        givenCards(card(1L, "신한카드 Deep Dream", CardType.CREDIT, 3_000_000L));
        givenSpending(List.<Object[]>of(
                row(2026, 4, 1L, 1_050_000L),
                row(2026, 5, 1L, 780_000L),
                row(2026, 6, 1L, 1_320_000L),
                row(2026, 7, 1L, 1_650_000L),
                row(2026, 8, 1L, 2_100_000L),
                row(2026, 9, 1L, 900_000L)));

        CardUsageResponse result = cardUsageService.findUsage(USER_ID, BASE);

        assertThat(result.getTrend()).extracting(CardUsageTrendResponse::getMonth)
                .containsExactly("2026-04", "2026-05", "2026-06", "2026-07", "2026-08", "2026-09");
        assertThat(result.getTrend()).extracting(CardUsageTrendResponse::getUtilization)
                .extracting(Object::toString)
                .containsExactly("35.0", "26.0", "44.0", "55.0", "70.0", "30.0");
    }

    @Test
    void 거래가_없는_달도_0으로_채운다() {
        // 빈 달을 빼면 그래프의 가로축이 들쭉날쭉해진다.
        givenCards(card(1L, "신한카드 Deep Dream", CardType.CREDIT, 3_000_000L));
        givenSpending(List.<Object[]>of(row(2026, 9, 1L, 900_000L)));

        CardUsageResponse result = cardUsageService.findUsage(USER_ID, BASE);

        assertThat(result.getTrend()).hasSize(6);
        assertThat(result.getTrend().get(0).getUsage()).isZero();
        assertThat(result.getTrend().get(0).getUtilization()).isEqualByComparingTo("0.0");
    }

    @Test
    void 신용카드가_없으면_빈_응답을_준다() {
        givenCards(card(2L, "현대카드 ZERO (체크)", CardType.CHECK, null));

        CardUsageResponse result = cardUsageService.findUsage(USER_ID, BASE);

        assertThat(result.getCards()).isEmpty();
        assertThat(result.getTrend()).isEmpty();
        assertThat(result.getTotalCreditLimit()).isZero();
        assertThat(result.getCurrentUtilization()).isNull();
    }
}

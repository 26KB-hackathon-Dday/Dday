package com.dday.domain.credit.service;

import com.dday.domain.credit.dto.response.ExpectedRateResponse;
import com.dday.domain.credit.dto.response.LenderRateResponse;
import com.dday.domain.credit.dto.response.LenderType;
import com.dday.domain.credit.entity.CreditScore;
import com.dday.domain.credit.repository.CreditScoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ExpectedRateServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private CreditScoreRepository creditScoreRepository;

    @InjectMocks
    private ExpectedRateService expectedRateService;

    private void givenLatestScore(Integer score) {
        Optional<CreditScore> latest = score == null
                ? Optional.empty()
                : Optional.of(CreditScore.builder().agency("KCB").score(score).build());
        given(creditScoreRepository.findFirstByUserUserIdOrderByUpdatedAtDescCreditScoreIdDesc(USER_ID))
                .willReturn(latest);
    }

    private LenderRateResponse lender(ExpectedRateResponse response, LenderType type) {
        return response.getLenders().stream()
                .filter(it -> it.getLenderType() == type)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void 최신_점수로_업권별_예상금리와_연이자를_계산한다() {
        givenLatestScore(704);

        ExpectedRateResponse result = expectedRateService.findExpected(USER_ID);

        assertThat(result.getScore()).isEqualTo(704);
        assertThat(result.getPrincipal()).isEqualTo(10_000_000L);
        assertThat(lender(result, LenderType.BANK).getCurrentRate()).isEqualByComparingTo("6.99");
        // 1,000만원 x 6.99% = 699,000원
        assertThat(lender(result, LenderType.BANK).getCurrentAnnualInterest()).isEqualTo(699_000L);
        assertThat(lender(result, LenderType.CARD).getCurrentAnnualInterest()).isEqualTo(1_536_000L);
    }

    @Test
    void 목표는_현재_점수보다_25점_위다() {
        givenLatestScore(704);

        ExpectedRateResponse result = expectedRateService.findExpected(USER_ID);

        assertThat(result.getTargetScore()).isEqualTo(729);
        assertThat(result.getScoreGap()).isEqualTo(25);

        LenderRateResponse bank = lender(result, LenderType.BANK);
        assertThat(bank.getTargetRate()).isEqualByComparingTo("6.90");
        assertThat(bank.getTargetAnnualInterest()).isEqualTo(690_000L);
        assertThat(bank.getAnnualSaving()).isEqualTo(9_000L);
    }

    @Test
    void 세_업권_모두_절약액이_생긴다() {
        // 보간하지 않으면 캐피탈·카드사는 25점으로 구간을 못 넘어 0원이 된다.
        givenLatestScore(704);

        ExpectedRateResponse result = expectedRateService.findExpected(USER_ID);

        assertThat(lender(result, LenderType.CAPITAL).getAnnualSaving()).isEqualTo(25_000L);
        assertThat(lender(result, LenderType.CARD).getAnnualSaving()).isEqualTo(21_000L);
    }

    @Test
    void 목표_점수는_만점을_넘지_않는다() {
        givenLatestScore(990);

        ExpectedRateResponse result = expectedRateService.findExpected(USER_ID);

        assertThat(result.getTargetScore()).isEqualTo(1000);
        assertThat(result.getScoreGap()).isEqualTo(10);
    }

    @Test
    void 만점이면_목표와_절약액이_없다() {
        givenLatestScore(1000);

        ExpectedRateResponse result = expectedRateService.findExpected(USER_ID);

        assertThat(result.getTargetScore()).isNull();
        assertThat(result.getScoreGap()).isNull();
        assertThat(lender(result, LenderType.BANK).getTargetRate()).isNull();
        assertThat(lender(result, LenderType.BANK).getAnnualSaving()).isNull();
        assertThat(lender(result, LenderType.BANK).getCurrentRate()).isEqualByComparingTo("4.93");
    }

    @Test
    void 공시에_없는_구간이면_금리를_비워둔다() {
        // 카드사는 400점 이하 공시가 없다. 0%로 내려보내면 "무이자"로 읽힌다.
        givenLatestScore(400);

        ExpectedRateResponse result = expectedRateService.findExpected(USER_ID);

        LenderRateResponse card = lender(result, LenderType.CARD);
        assertThat(card.getCurrentRate()).isNull();
        assertThat(card.getCurrentAnnualInterest()).isNull();
        assertThat(card.getInstitutionCount()).isNull();
        // 목표 점수(425)도 여전히 공시 밖이다.
        assertThat(card.getTargetRate()).isNull();
        assertThat(card.getAnnualSaving()).isNull();
    }

    @Test
    void 평균에_들어간_회사_수를_함께_내려준다() {
        givenLatestScore(704);

        ExpectedRateResponse result = expectedRateService.findExpected(USER_ID);

        assertThat(lender(result, LenderType.BANK).getInstitutionCount()).isEqualTo(15);
        assertThat(lender(result, LenderType.CAPITAL).getInstitutionCount()).isEqualTo(8);
        assertThat(lender(result, LenderType.CARD).getInstitutionCount()).isEqualTo(5);
    }

    @Test
    void 신용점수_기록이_없으면_빈_응답을_준다() {
        givenLatestScore(null);

        ExpectedRateResponse result = expectedRateService.findExpected(USER_ID);

        assertThat(result.getScore()).isNull();
        assertThat(result.getTargetScore()).isNull();
        assertThat(result.getLenders()).isEmpty();
    }
}

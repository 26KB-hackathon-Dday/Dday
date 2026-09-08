package com.dday.domain.credit.service;

import com.dday.domain.credit.dto.response.CreditScoreHistoryResponse;
import com.dday.domain.credit.dto.response.CreditScoreItemResponse;
import com.dday.domain.credit.entity.CreditScore;
import com.dday.domain.credit.repository.CreditScoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CreditScoreServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 9, 1, 10, 0);

    @Mock
    private CreditScoreRepository creditScoreRepository;

    @InjectMocks
    private CreditScoreService creditScoreService;

    /**
     * 소유자는 이 테스트의 관심사가 아니라 비워둔다 — 조회 범위는 레포지토리 메서드가 건다.
     * {@code updatedAt}은 {@code @UpdateTimestamp}라 영속화 없이는 채워지지 않아 직접 넣는다.
     */
    private CreditScore score(long id, int value, LocalDateTime updatedAt) {
        CreditScore creditScore = CreditScore.builder()
                .agency("KCB")
                .score(value)
                .build();
        ReflectionTestUtils.setField(creditScore, "creditScoreId", id);
        ReflectionTestUtils.setField(creditScore, "updatedAt", updatedAt);
        return creditScore;
    }

    /** 레포지토리는 최신순으로 돌려준다. 서비스는 그 순서를 그대로 쓴다. */
    private void givenRecords(CreditScore... records) {
        given(creditScoreRepository.findTop6ByUserUserIdOrderByUpdatedAtDescCreditScoreIdDesc(USER_ID))
                .willReturn(List.of(records));
    }

    @Test
    void 기록이_여섯_건이어도_최신_다섯_건만_반환한다() {
        givenRecords(
                score(6L, 812, BASE),
                score(5L, 805, BASE.minusDays(30)),
                score(4L, 808, BASE.minusDays(60)),
                score(3L, 790, BASE.minusDays(90)),
                score(2L, 777, BASE.minusDays(120)),
                score(1L, 770, BASE.minusDays(150))
        );

        CreditScoreHistoryResponse result = creditScoreService.findRecent(USER_ID);

        assertThat(result.getItems()).extracting(CreditScoreItemResponse::getScore)
                .containsExactly(812, 805, 808, 790, 777);
    }

    @Test
    void 변동폭은_직전_기록과의_차이다() {
        givenRecords(
                score(3L, 812, BASE),
                score(2L, 805, BASE.minusDays(30)),
                score(1L, 808, BASE.minusDays(60))
        );

        CreditScoreHistoryResponse result = creditScoreService.findRecent(USER_ID);

        assertThat(result.getItems()).extracting(CreditScoreItemResponse::getDiff)
                .containsExactly(7, -3, null);
    }

    @Test
    void 여섯_번째_기록으로_마지막_항목의_변동폭을_채운다() {
        givenRecords(
                score(6L, 812, BASE),
                score(5L, 805, BASE.minusDays(30)),
                score(4L, 808, BASE.minusDays(60)),
                score(3L, 790, BASE.minusDays(90)),
                score(2L, 777, BASE.minusDays(120)),
                score(1L, 770, BASE.minusDays(150))
        );

        CreditScoreHistoryResponse result = creditScoreService.findRecent(USER_ID);

        // 다섯 번째 항목(777)의 변동폭은 목록에 없는 여섯 번째 기록(770)과의 차이다.
        assertThat(result.getItems()).last()
                .extracting(CreditScoreItemResponse::getDiff).isEqualTo(7);
    }

    @Test
    void 최신_점수와_변동폭은_첫_항목의_값이다() {
        givenRecords(
                score(2L, 812, BASE),
                score(1L, 805, BASE.minusDays(30))
        );

        CreditScoreHistoryResponse result = creditScoreService.findRecent(USER_ID);

        assertThat(result.getLatestScore()).isEqualTo(812);
        assertThat(result.getDiffFromPrevious()).isEqualTo(7);
    }

    @Test
    void 각_항목에_상위_퍼센트를_붙인다() {
        givenRecords(
                score(2L, 812, BASE),
                score(1L, 700, BASE.minusDays(30))
        );

        CreditScoreHistoryResponse result = creditScoreService.findRecent(USER_ID);

        assertThat(result.getItems()).extracting(CreditScoreItemResponse::getPercentile)
                .containsExactly(new BigDecimal("57.0"), new BigDecimal("85.1"));
    }

    @Test
    void 최신_상위_퍼센트는_첫_항목의_값이다() {
        givenRecords(
                score(2L, 812, BASE),
                score(1L, 700, BASE.minusDays(30))
        );

        CreditScoreHistoryResponse result = creditScoreService.findRecent(USER_ID);

        assertThat(result.getLatestPercentile()).isEqualByComparingTo("57.0");
    }

    @Test
    void 기록이_없으면_빈_응답을_준다() {
        givenRecords();

        CreditScoreHistoryResponse result = creditScoreService.findRecent(USER_ID);

        assertThat(result.getLatestScore()).isNull();
        assertThat(result.getLatestPercentile()).isNull();
        assertThat(result.getDiffFromPrevious()).isNull();
        assertThat(result.getItems()).isEmpty();
    }
}

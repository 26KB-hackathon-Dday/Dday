package com.dday.domain.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 최신 신용점수 기준 업권별 예상 금리 응답.
 *
 * <p>{@link #principal}은 절약액을 재는 자(尺)다. 실제 대출액이 아니라 "1,000만원을 빌린다면"
 * 이라는 가정이고, 화면 문구의 "1,000만원 기준"과 같은 값이다. 프론트가 따로 박지 않도록
 * 서버가 내려준다.
 *
 * <p>기록이 없는 회원은 404가 아니라 {@link #empty()}를 받는다 — 온보딩 직후 이력이 없는 것은
 * 정상 상태다 ({@code CreditScoreHistoryResponse}와 같은 이유).
 */
@Getter
@Builder
@AllArgsConstructor
public class ExpectedRateResponse {

    /** 계산에 쓴 신용점수. 기록이 없으면 {@code null}. */
    private final Integer score;

    /** 절약액 계산 기준 원금(원). */
    private final Long principal;

    /** 금리가 실제로 내려가는 다음 구간의 시작 점수. 이미 최고 구간이면 {@code null}. */
    private final Integer targetScore;

    /** 목표 점수까지 남은 점수. 목표가 없으면 {@code null}. */
    private final Integer scoreGap;

    /** 은행 → 캐피탈 → 카드사 순. */
    private final List<LenderRateResponse> lenders;

    /** 아직 신용점수 기록이 없는 회원. */
    public static ExpectedRateResponse empty() {
        return ExpectedRateResponse.builder()
                .lenders(List.of())
                .build();
    }
}

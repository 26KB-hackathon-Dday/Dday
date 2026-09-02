package com.dday.domain.pocket.dto.response;

import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 포켓 하나의 소진 현황.
 *
 * <p>{@code label}·{@code description}·{@code usageRate}는 엔티티에 없는 값이지만 서버가 내려준다.
 * 프론트가 enum 이름을 한글로 바꾸는 표를 따로 들고 있으면 반드시 어긋나고, 소진율을 각자
 * 계산하면 반올림 규칙이 화면마다 달라지기 때문이다.
 */
@Getter
@AllArgsConstructor
@Builder
public class PocketResponse {

    private Long pocketId;

    /** enum 상수 이름. 프론트가 분기해야 할 때 쓴다 (HOUSING, LIVING, EMERGENCY, ASSET) */
    private PocketType type;

    /** 화면에 그대로 띄울 이름 (주거, 생활, …) */
    private String label;

    /** 이 포켓이 무엇을 담는지 한 줄 설명 */
    private String description;

    /** 이번 달 배분액 */
    private BigDecimal monthlyBudget;

    /** 이번 달 소진액 */
    private BigDecimal spentThisMonth;

    /** 남은 금액. 초과 지출이면 음수다 */
    private BigDecimal remaining;

    /** 소진율(%). 소수점 1자리. 배분액이 0이면 0으로 준다 */
    private BigDecimal usageRate;

    public static PocketResponse from(Pocket pocket) {
        return PocketResponse.builder()
                .pocketId(pocket.getId())
                .type(pocket.getType())
                .label(pocket.getType().getLabel())
                .description(pocket.getType().getDescription())
                .monthlyBudget(pocket.getMonthlyBudget())
                .spentThisMonth(pocket.getSpentThisMonth())
                .remaining(pocket.remaining())
                .usageRate(calculateUsageRate(pocket))
                .build();
    }

    /** 배분액 0으로 나누면 ArithmeticException이 난다. 아직 예산을 확정하지 않은 포켓이 그 경우다. */
    private static BigDecimal calculateUsageRate(Pocket pocket) {
        if (pocket.getMonthlyBudget().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return pocket.getSpentThisMonth()
                .multiply(BigDecimal.valueOf(100))
                .divide(pocket.getMonthlyBudget(), 1, RoundingMode.HALF_UP);
    }
}

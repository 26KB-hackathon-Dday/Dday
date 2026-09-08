package com.dday.domain.pocket.dto.response;

import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 포켓 하나.
 *
 * <p><b>금액이 없다.</b> 이번 달 목표액은 {@code monthly_pocket_budget}(Budget 도메인)이 들고,
 * 소진액은 {@code financial_transaction}을 집계해서 구한다. 둘 다 이 포켓 행에는 없는 값이라
 * 예산·소비 조회 API가 붙을 때 별도 응답으로 내려간다.
 */
@Getter
@AllArgsConstructor
@Builder
public class PocketResponse {

    private Long pocketId;

    /** enum 상수 이름. 프론트가 분기해야 할 때 쓴다 (ESSENTIAL, FREE, EMERGENCY, FUTURE_ASSET) */
    private PocketType pocketType;

    /** 화면에 그대로 띄울 이름. 사용자가 바꿀 수 있어서 서버가 행에서 읽어 내려준다 */
    private String pocketName;

    /** 이 포켓이 무엇을 담는지 한 줄 설명 */
    private String description;

    public static PocketResponse from(Pocket pocket) {
        return PocketResponse.builder()
                .pocketId(pocket.getPocketId())
                .pocketType(pocket.getPocketType())
                .pocketName(pocket.getPocketName())
                .description(pocket.getDescription())
                .build();
    }
}

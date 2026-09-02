package com.dday.domain.pocket.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 포켓의 용도. 기획서의 네 갈래를 그대로 옮긴 것이다.
 *
 * <p>포켓은 자금을 직접 보관하지 않는다. 이용자 계좌 위에 얹는 <b>목적별 관리 계층</b>이다.
 * 그래서 여기 있는 건 "어떤 목적의 돈인가"를 가르는 분류이지 계좌가 아니다.
 *
 * <p>순서가 곧 화면에 뿌리는 순서다({@code ordinal()}에 의존하는 로직은 두지 않되,
 * 목록 조회는 이 선언 순서로 정렬한다 — 주거가 가장 먼저 확보돼야 할 돈이기 때문이다).
 */
@Getter
@RequiredArgsConstructor
public enum PocketType {

    HOUSING("주거", "보증금·월세·공과금"),
    LIVING("생활", "식비·교통·통신 등 일상 지출"),
    EMERGENCY("비상", "의료비처럼 예기치 못한 지출"),
    ASSET("자산형성", "청년 자산형성 상품과 연계한 장기 목표");

    /** 화면에 그대로 노출되는 이름. 프론트가 별도 매핑을 두지 않도록 서버가 내려준다. */
    private final String label;

    /** 이 포켓이 무엇을 담는지에 대한 한 줄 설명. 역시 화면에 그대로 쓴다. */
    private final String description;
}

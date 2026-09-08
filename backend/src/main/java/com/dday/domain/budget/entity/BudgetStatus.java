package com.dday.domain.budget.entity;

/**
 * 월 예산의 확정 상태.
 *
 * <p>기획대로 <b>예산은 시스템이 초안을 제시하고 사용자가 확정한다.</b> 그래서 초안 상태로도
 * 행이 존재하며, 확정 전까지는 시스템이 다시 계산해 덮어써도 된다.
 */
public enum BudgetStatus {

    /** 시스템이 만든 초안 — 아직 사용자가 보지 않았거나 확정하지 않았다 */
    DRAFT,

    /** 사용자가 확정 — 이후 시스템이 임의로 덮어쓰지 않는다 */
    CONFIRMED
}

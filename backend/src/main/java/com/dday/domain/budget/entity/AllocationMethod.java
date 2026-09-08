package com.dday.domain.budget.entity;

/**
 * 포켓 목표액이 어떻게 정해졌는지.
 *
 * <p>합이 전체 예산과 맞아야 하는데 사용자가 세 개만 입력하는 경우가 흔하다. 나머지 하나를
 * {@link #AUTO_REMAINDER}로 두면 잔액이 자동으로 흘러들어가 합이 항상 맞는다.
 */
public enum AllocationMethod {

    USER_INPUT,
    AUTO_REMAINDER
}

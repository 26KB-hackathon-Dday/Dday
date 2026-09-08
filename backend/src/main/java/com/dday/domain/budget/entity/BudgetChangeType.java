package com.dday.domain.budget.entity;

/** 예산이 바뀐 방식. 총액이 바뀐 건지, 총액은 그대로고 포켓 사이에서만 옮긴 건지 가른다. */
public enum BudgetChangeType {

    /** 사용자가 총액이나 포켓 금액을 직접 고쳤다 */
    USER_EDIT,

    /** 총액은 그대로, 포켓 사이에서 돈을 옮겼다 */
    REALLOCATION
}

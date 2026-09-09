import { api } from '@/api/client'

export type BudgetAdjustmentPocketType = 'ESSENTIAL' | 'FREE' | 'FUTURE_ASSET' | 'EMERGENCY'

export interface PocketAdjustmentResponse {
  pocketId: number

  pocketType: BudgetAdjustmentPocketType

  pocketName: string

  /**
   * 현재 포켓에 할당되어 있는 금액
   */
  targetAmount: number

  /**
   * 현재 사용/달성 금액
   *
   * ESSENTIAL:
   * 현재까지 실제 사용액
   *
   * FREE:
   * 현재까지 실제 사용액
   *
   * FUTURE_ASSET:
   * 현재까지 실제 달성액
   *
   * EMERGENCY:
   * 0
   */
  usedAmount: number

  remainingAmount: number
}

export interface BudgetAdjustmentResponse {
  month: string

  totalBudgetAmount: number

  totalUsedAmount: number

  pockets: PocketAdjustmentResponse[]
}

export interface PocketAdjustmentRequest {
  pocketType: BudgetAdjustmentPocketType

  amount: number
}

/**
 * 최종 포켓 재조정 요청
 *
 * 중요:
 * 백엔드 BudgetAdjustmentRequest의 필드명이
 * allocations이므로 프론트도 반드시 allocations 사용.
 */
export interface BudgetAdjustmentRequest {
  totalBudgetAmount: number

  allocations: PocketAdjustmentRequest[]

  changeReason?: string
}

export interface TotalBudgetUpdateResponse {
  budgetMonth: string

  totalBudgetAmount: number

  minimumTotalBudget?: number
}

export const budgetAdjustmentApi = {
  /**
   * 현재 진행 중인 예산 조회
   */
  findCurrent: () => api.get<BudgetAdjustmentResponse>('/api/budget-adjustments/current'),

  /**
   * 총 예산만 단독 수정
   */
  updateTotalBudget: (totalBudgetAmount: number) =>
    api.patch<TotalBudgetUpdateResponse>('/api/budget-adjustments/current/total-budget', {
      totalBudgetAmount,
    }),

  /**
   * 최종 포켓 배분 저장
   */
  adjustCurrent: (request: BudgetAdjustmentRequest) =>
    api.patch<BudgetAdjustmentResponse>('/api/budget-adjustments/current', request),
}

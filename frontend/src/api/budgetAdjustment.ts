import { api } from './client'

export type BudgetAdjustmentPocketType = 'ESSENTIAL' | 'FREE' | 'EMERGENCY' | 'FUTURE_ASSET'

export interface BudgetAdjustmentPocket {
  pocketId: number
  pocketType: BudgetAdjustmentPocketType
  pocketName: string

  /** 현재 설정된 포켓 예산 */
  targetAmount: number

  /** 이번 달 실제 사용액 */
  spentAmount: number

  /** 재조정 시 설정할 수 있는 최소 금액 */
  minimumAmount: number

  /** 현재 예산 - 사용액 */
  remainingAmount: number
}

export interface CurrentBudgetAdjustmentResponse {
  monthlyBudgetId: number
  budgetMonth: string

  /** 이번 달 총 예산 */
  totalBudgetAmount: number

  /** 이미 사용한 금액 합계 */
  minimumTotalBudget: number

  pockets: BudgetAdjustmentPocket[]
}

export interface BudgetAdjustmentPocketRequest {
  pocketType: BudgetAdjustmentPocketType
  amount: number
}

export interface UpdateBudgetAdjustmentRequest {
  totalBudgetAmount: number
  pockets: BudgetAdjustmentPocketRequest[]
  changeReason?: string
}

export const budgetAdjustmentApi = {
  /**
   * 진행 중인 이번 달 확정 예산 조회
   */
  getCurrent: () => api.get<CurrentBudgetAdjustmentResponse>('/api/budget-adjustments/current'),

  /**
   * 진행 중인 이번 달 예산 재조정
   */
  updateCurrent: (request: UpdateBudgetAdjustmentRequest) =>
    api.patch<CurrentBudgetAdjustmentResponse>('/api/budget-adjustments/current', request),
}

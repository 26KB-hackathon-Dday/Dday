import { api } from '@/api/client'

export type BudgetAdjustmentPocketType = 'ESSENTIAL' | 'FREE' | 'EMERGENCY' | 'FUTURE_ASSET'

export interface PocketAdjustmentResponse {
  pocketId: number

  pocketType: BudgetAdjustmentPocketType

  pocketName: string

  /*
   * 현재 포켓 할당액
   */
  targetAmount: number

  /*
   * 실제 소비 금액.
   *
   * 미래자산 / 비상금은
   * 화면 잠금 기준으로 사용하지 않는다.
   */
  spentAmount: number

  /*
   * 실제로 내려갈 수 있는 최소값.
   *
   * ESSENTIAL = 실제 사용액
   * FREE = 실제 사용액
   * FUTURE_ASSET = 실제 달성액
   * EMERGENCY = 0
   */
  minimumAmount: number

  remainingAmount: number
}

export interface BudgetAdjustmentResponse {
  monthlyBudgetId: number

  budgetMonth: string

  totalBudgetAmount: number

  /*
   * 필수 실제 사용액
   * + 자유 실제 사용액
   * + 미래자산 달성액
   *
   * 비상금 제외.
   */
  minimumTotalBudget: number

  pockets: PocketAdjustmentResponse[]
}

export interface PocketAdjustmentRequest {
  pocketType: BudgetAdjustmentPocketType

  amount: number
}

export interface BudgetAdjustmentRequest {
  totalBudgetAmount: number

  /*
   * 중요:
   * 백엔드 DTO 이름과 동일하게 pockets.
   */
  pockets: PocketAdjustmentRequest[]

  changeReason?: string
}

export interface TotalBudgetUpdateResponse {
  budgetMonth: string

  totalBudgetAmount: number

  minimumTotalBudget: number
}

export const budgetAdjustmentApi = {
  findCurrent: () => api.get<BudgetAdjustmentResponse>('/api/budget-adjustments/current'),

  updateTotalBudget: (totalBudgetAmount: number) =>
    api.patch<TotalBudgetUpdateResponse>('/api/budget-adjustments/current/total-budget', {
      totalBudgetAmount,
    }),

  adjustCurrent: (request: BudgetAdjustmentRequest) =>
    api.patch<BudgetAdjustmentResponse>('/api/budget-adjustments/current', request),
}

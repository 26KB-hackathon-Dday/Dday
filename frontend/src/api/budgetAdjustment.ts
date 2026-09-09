import { api } from '@/api/client'

export type BudgetAdjustmentPocketType = 'ESSENTIAL' | 'FREE' | 'EMERGENCY' | 'FUTURE_ASSET'

export interface PocketAdjustmentResponse {
  pocketId: number
  pocketType: BudgetAdjustmentPocketType
  pocketName: string
  targetAmount: number
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

export interface BudgetAdjustmentRequest {
  totalBudgetAmount: number
  allocations: PocketAdjustmentRequest[]
}

export const budgetAdjustmentApi = {
  findCurrent: () => api.get<BudgetAdjustmentResponse>('/api/budget-adjustments/current'),

  adjustCurrent: (request: BudgetAdjustmentRequest) =>
    api.patch<BudgetAdjustmentResponse>('/api/budget-adjustments/current', request),
}

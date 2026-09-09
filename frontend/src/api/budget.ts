import { api } from './client'
import type { PocketType } from './pocket'

export interface PocketBudgetRequest {
  pocketType: PocketType
  amount: number
}

export interface MonthlyBudgetConfirmRequest {
  totalBudgetAmount: number
  pockets: PocketBudgetRequest[]
}

export interface MonthlyBudgetConfirmResponse {
  monthlyBudgetId: number
  month: string
  totalBudgetAmount: number
  pockets: PocketBudgetRequest[]
}

export const budgetApi = {
  confirmCurrent: (body: MonthlyBudgetConfirmRequest) =>
    api.post<MonthlyBudgetConfirmResponse>('/api/budgets/current/confirm', body),
}

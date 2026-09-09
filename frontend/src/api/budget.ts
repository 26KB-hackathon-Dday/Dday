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

/*
 * =========================
 * 온보딩 기반 추천 예산
 * =========================
 */

export interface BudgetRecommendationPocket {
  pocketType: PocketType

  amount: number

  reason: string
}

export interface BudgetRecommendationResponse {
  totalBudgetAmount: number

  monthlyIncome: number

  monthlyHousingCost: number

  currentAsset: number

  emergencyReserveTarget: number

  emergencyReserveNeeded: boolean

  pockets: BudgetRecommendationPocket[]
}

export const budgetApi = {
  /*
   * 온보딩 DB 정보를 기준으로
   * 최초 추천 예산 조회.
   */
  getCurrentRecommendation: () =>
    api.get<BudgetRecommendationResponse>('/api/budgets/current/recommendation'),

  /*
   * 추천값 또는 사용자가 조정한 값을
   * 실제 월 예산으로 확정.
   */
  confirmCurrent: (body: MonthlyBudgetConfirmRequest) =>
    api.post<MonthlyBudgetConfirmResponse>('/api/budgets/current/confirm', body),
}

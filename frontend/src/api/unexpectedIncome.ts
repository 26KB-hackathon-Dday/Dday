import { api } from './client'

export type UnexpectedIncomePocketType = 'ESSENTIAL' | 'FREE' | 'EMERGENCY' | 'FUTURE_ASSET'

export type UnexpectedIncomeType = 'NEW_INCOME' | 'RECURRING_LIKELY' | 'RECURRING_OVER'

export interface UnexpectedIncome {
  transactionId: number
  amount: number
  senderName: string
  transactionAt: string
  type: UnexpectedIncomeType

  recurringIncomeId: number | null
  recurringIncomeName: string | null
  expectedAmount: number | null
  depositTiming: string | null
  excessAmount: number
}

export interface UnexpectedIncomeAllocation {
  pocketType: UnexpectedIncomePocketType
  amount: number
}

export interface UnexpectedIncomeAddRequest {
  addAmount: number
  allocations: UnexpectedIncomeAllocation[]
}

export interface UnexpectedIncomeAddedPocket {
  pocketType: UnexpectedIncomePocketType
  pocketName: string
  targetAmount: number
}

export interface UnexpectedIncomeAddResponse {
  transactionId: number
  budgetMonth: string
  incomeAmount: number
  addedAmount: number
  totalBudgetAmount: number
  pockets: UnexpectedIncomeAddedPocket[]
}

export const unexpectedIncomeApi = {
  /**
   * 아직 처리하지 않은 입금 한 건 조회.
   *
   * 백엔드에서 이미
   * NEW_INCOME / RECURRING_LIKELY / RECURRING_OVER
   * 중 하나로 분류해서 내려준다.
   */
  findPending: () => api.get<UnexpectedIncome | null>('/api/unexpected-incomes/pending'),

  /**
   * 이번 달 예산에 포함하지 않기.
   */
  exclude: (transactionId: number) =>
    api.patch<void>(`/api/unexpected-incomes/${transactionId}/exclude`),

  /**
   * 실제 입금액 중 선택한 금액을
   * 이번 달 포켓 예산에 추가.
   */
  addToBudget: (transactionId: number, request: UnexpectedIncomeAddRequest) =>
    api.patch<UnexpectedIncomeAddResponse>(
      `/api/unexpected-incomes/${transactionId}/add-to-budget`,
      request,
    ),
}

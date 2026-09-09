import { api } from './client'

/**
 * 포켓 용도. 백엔드 `PocketType` enum과 값이 같아야 한다.
 * 정본은 `backend/src/main/java/com/dday/domain/pocket/entity/PocketType.java`다.
 */
export type PocketType = 'ESSENTIAL' | 'FREE' | 'EMERGENCY' | 'FUTURE_ASSET'

export const POCKET_LABEL: Record<PocketType, string> = {
  ESSENTIAL: '필수 포켓',
  FREE: '자유 포켓',
  EMERGENCY: '비상금 포켓',
  FUTURE_ASSET: '미래자산 포켓',
}

export const POCKET_ORDER: PocketType[] = ['ESSENTIAL', 'FREE', 'FUTURE_ASSET', 'EMERGENCY']

export interface Pocket {
  pocketId: number
  pocketType: PocketType
  pocketName: string
  description: string
}

export interface PocketMonthlySummary {
  pocketId: number
  pocketType: PocketType
  pocketName: string
  targetAmount: number
  usedAmount: number | null
  remainingAmount: number | null
  usageRate: number | null
  overAmount: number | null
}
export interface PocketMonthlyResponse {
  month: string
  totalBudgetAmount: number
  pockets: PocketMonthlySummary[]
}

export interface PocketInitializeResponse {
  createdCount: number
  totalCount: number
}

export type TransactionType = 'INCOME' | 'EXPENSE' | 'SELF_TRANSFER' | 'OTHER'
export type TransactionStatus = 'NORMAL' | 'CANCELED' | 'REFUNDED'

export interface PocketTransaction {
  transactionId: number
  transactionAt: string
  merchantName: string | null
  memo: string | null
  amount: number
  transactionType: TransactionType
  transactionStatus: TransactionStatus
  category: { categoryId: number; categoryCode: string; categoryName: string } | null
  classificationStatus: string | null
  classificationSource: string | null
}

export interface PocketTransactionDetail extends PocketTransaction {
  sourceType: 'ACCOUNT' | 'CARD'
  sourceTransactionId: string
  merchantRegno: string | null
  pocket: Pocket | null
}

export interface PocketCategory {
  categoryId: number
  categoryCode: string
  categoryName: string
  defaultPocketType: PocketType
  children: PocketCategory[]
}

export interface PocketCategoryListResponse {
  categories: PocketCategory[]
}

export interface TransactionClassificationRequest {
  pocketType: PocketType
  categoryId: number
  applyFutureRule: boolean
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface PocketCategoryUsageItem {
  categoryId: number
  categoryCode: string
  categoryName: string
  usedAmount: number
}

export interface PocketCategoryUsageResponse {
  month: string
  pocketType: PocketType
  totalUsedAmount: number
  categories: PocketCategoryUsageItem[]
  unclassifiedUsedAmount: number
}

export const pocketApi = {
  initialize: () => api.post<PocketInitializeResponse>('/api/pockets/initialize'),
  findAll: () => api.get<Pocket[]>('/api/pockets'),
  findById: (pocketId: number) => api.get<Pocket>(`/api/pockets/${pocketId}`),
  findMonthly: (month: string) =>
    api.get<PocketMonthlyResponse>(`/api/pockets/monthly?month=${encodeURIComponent(month)}`),
  findTransactions: (pocketType: PocketType, month: string, page = 0, size = 3) =>
    api.get<PageResponse<PocketTransaction>>(
      `/api/pockets/${pocketType}/transactions?month=${encodeURIComponent(month)}&page=${page}&size=${size}`,
    ),
  findCategoryUsage: (pocketType: PocketType, month: string) =>
    api.get<PocketCategoryUsageResponse>(
      `/api/pockets/${pocketType}/category-usage?month=${encodeURIComponent(month)}`,
    ),
  findTransaction: (transactionId: number) =>
    api.get<PocketTransactionDetail>(`/api/transactions/${transactionId}`),
  findCategories: (pocketType: PocketType) =>
    api.get<PocketCategoryListResponse>(
      `/api/categories?pocketType=${encodeURIComponent(pocketType)}`,
    ),
  classifyTransaction: (transactionId: number, body: TransactionClassificationRequest) =>
    api.patch<void>(`/api/transactions/${transactionId}/classification`, body),
}

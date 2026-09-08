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

export const pocketApi = {
  initialize: () => api.post<PocketInitializeResponse>('/api/pockets/initialize'),
  findAll: () => api.get<Pocket[]>('/api/pockets'),
  findById: (pocketId: number) => api.get<Pocket>(`/api/pockets/${pocketId}`),
  findMonthly: (month: string) =>
    api.get<PocketMonthlyResponse>(`/api/pockets/monthly?month=${encodeURIComponent(month)}`),
}

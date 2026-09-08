import { api } from './client'

/**
 * 온보딩(자립 계획 생성) API + 그 요청/응답 타입.
 *
 * 백엔드 계약 정본: `backend/src/main/java/com/dday/domain/onboarding/`의
 * 컨트롤러 · DTO, 그리고 `domain/user/entity`(HousingType, SettlementReceived),
 * `domain/income/entity`(IncomeType).
 */

export type ProtectionStatus = 'IN_PROTECTION' | 'DISCHARGED'

export type HousingType =
  | 'LH_JEONSE'
  | 'MONTHLY'
  | 'SELF_RELIANCE_HOUSE'
  | 'DORM'
  | 'FAMILY'
  | 'ETC'

export const HOUSING_TYPE_LABEL: Record<HousingType, string> = {
  LH_JEONSE: 'LH전세임대',
  MONTHLY: '월세',
  SELF_RELIANCE_HOUSE: '자립생활관',
  DORM: '기숙사',
  FAMILY: '가족·지인과 거주',
  ETC: '기타',
}

export type IncomeType = 'SALARY' | 'ALLOWANCE' | 'SETTLEMENT_FUND' | 'DIDIM_SEED' | 'ETC'

/** 정기수입 목록 화면에서 유형을 표시할 때 쓴다. 입력 화면(12번)은 프리셋 버튼이라
 *  이 중 SALARY·ALLOWANCE·ETC만 실제로 저장될 수 있다. */
export const INCOME_TYPE_LABEL: Record<IncomeType, string> = {
  SALARY: '근로소득',
  ALLOWANCE: '지원금',
  SETTLEMENT_FUND: '자립정착금',
  DIDIM_SEED: '디딤씨앗통장',
  ETC: '기타',
}

export type SettlementReceived = 'RECEIVED' | 'NOT_YET' | 'NONE'

export const SETTLEMENT_RECEIVED_LABEL: Record<SettlementReceived, string> = {
  RECEIVED: '받았어요',
  NOT_YET: '아직 받지 않았어요',
  NONE: '해당 없음',
}

// ── 요청 ──────────────────────────────────────────────

export interface ProtectionDateRequest {
  /** YYYY-MM-DD */
  protectionEndDate: string
}

export interface RegionRequest {
  regionCode: string
  regionName: string
  districtName: string
}

export interface HousingTypeRequest {
  housingType: HousingType
}

export interface HousingCostRequest {
  deposit?: number
  monthlyRent?: number
  maintenanceFee?: number
}

export interface IncomeRequest {
  name: string
  incomeType: IncomeType
  amount: number
  paymentTiming?: string
}

export interface AssetRequest {
  totalSaved: number
  settlementReceived: SettlementReceived
}

// ── 응답 ──────────────────────────────────────────────

export interface OnboardingProgressResponse {
  isCompleted: boolean
}

export interface ProtectionDateResponse {
  protectionEndDate: string
  supportEndDate: string
  remainingMonths: number
  dDay: number
  protectionStatus: ProtectionStatus
}

export interface RegionResponse {
  success: boolean
  message: string
}

export interface HousingTypeResponse {
  housingType: HousingType
  /** true면 주거비 입력 화면을 건너뛴다 (거주 형태상 본인 주거비가 없는 경우) */
  skipHousingCost: boolean
}

export interface HousingCostResponse {
  estimatedMonthly: number
}

export interface IncomeItem {
  incomeId: number
  name: string
  incomeType: IncomeType
  amount: number
  paymentTiming?: string
}

export interface IncomeListResponse {
  items: IncomeItem[]
  totalMonthly: number
}

export interface IncomeCreateResponse {
  incomeId: number
  totalMonthly: number
}

export interface IncomeDeleteResponse {
  success: boolean
  message: string
  totalMonthly: number
}

export interface AssetResponse {
  success: boolean
  message: string
}

export interface OnboardingCompleteResponse {
  dDay: number
  monthlyIncome: number
  monthlyExpense: number
}

// ── API ───────────────────────────────────────────────

export const onboardingApi = {
  getProgress: () => api.get<OnboardingProgressResponse>('/api/onboarding/progress'),

  saveProtectionDate: (body: ProtectionDateRequest) =>
    api.post<ProtectionDateResponse>('/api/onboarding/protection-date', body),

  saveRegion: (body: RegionRequest) => api.post<RegionResponse>('/api/onboarding/region', body),

  saveHousingType: (body: HousingTypeRequest) =>
    api.post<HousingTypeResponse>('/api/onboarding/housing-type', body),

  saveHousingCost: (body: HousingCostRequest) =>
    api.post<HousingCostResponse>('/api/onboarding/housing-cost', body),

  getIncomes: () => api.get<IncomeListResponse>('/api/onboarding/incomes'),

  addIncome: (body: IncomeRequest) =>
    api.post<IncomeCreateResponse>('/api/onboarding/incomes', body),

  deleteIncome: (incomeId: number) =>
    api.delete<IncomeDeleteResponse>(`/api/onboarding/incomes/${incomeId}`),

  saveAssets: (body: AssetRequest) => api.post<AssetResponse>('/api/onboarding/assets', body),

  complete: () => api.post<OnboardingCompleteResponse>('/api/onboarding/complete'),
}

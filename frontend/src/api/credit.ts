import { api } from './client'

/**
 * 신용 관리 도메인 API. 전부 JWT가 필요하다.
 *
 * 백엔드 계약 정본: `backend/.../credit/dto/response/*.java`
 *
 * 두 응답 모두 **기록이 없어도 404가 아니라 200**이다. 온보딩 직후 신용점수 이력이 없는 것은
 * 정상 상태라, 화면은 에러가 아니라 빈 상태를 그린다 (`latestScore`·`score`가 null).
 */

// ── 최근 신용점수 이력 (GET /api/credit/scores/recent) ────────────────────

export interface CreditScoreItem {
  creditScoreId: number
  score: number
  /** 신용평가기관. Mock 데이터 단계에서는 null일 수 있다. */
  agency: string | null
  /** 직전 기록 대비 증감. 가장 오래된 항목처럼 비교 대상이 없으면 null. */
  diff: number | null
  /** 상위 몇 %인지. KCB 2025년말 인원분포 기준이며 **작을수록 좋다**. */
  percentile: number
  updatedAt: string
}

export interface CreditScoreHistory {
  /** 기록이 없으면 null. */
  latestScore: number | null
  latestPercentile: number | null
  diffFromPrevious: number | null
  /** 최신순, 최대 5건. */
  items: CreditScoreItem[]
}

// ── 예상 금리 (GET /api/credit/rates/expected) ───────────────────────────

export type LenderType = 'BANK' | 'CAPITAL' | 'CARD'

export interface LenderRate {
  lenderType: LenderType
  /** 화면에 그대로 쓰는 업권 이름 ("은행"·"캐피탈"·"카드사"). 정본은 서버다. */
  label: string
  /** 이 평균에 들어간 회사 수. 공시가 없는 구간이면 null. */
  institutionCount: number | null
  /**
   * 현재 점수의 업권 평균 금리(%).
   *
   * **null은 0%가 아니다.** 그 점수대에 공시가 없다는 뜻이므로 "-"로 그린다.
   * 캐피탈·카드사는 500점 이하 공시가 아예 없다.
   */
  currentRate: number | null
  currentAnnualInterest: number | null
  targetRate: number | null
  targetAnnualInterest: number | null
  /** 연 절약액(원). 현재·목표 중 한쪽이라도 금리가 없으면 null. */
  annualSaving: number | null
}

export interface ExpectedRates {
  /** 계산에 쓴 신용점수. 기록이 없으면 null. */
  score: number | null
  /** 절약액 계산 기준 원금(원). 화면의 "1,000만원 기준"이 이 값이다. */
  principal: number | null
  /** 현재 점수 + 25점. 이미 만점이면 null. */
  targetScore: number | null
  scoreGap: number | null
  /** 은행 → 캐피탈 → 카드사 순. */
  lenders: LenderRate[]
}

export const creditApi = {
  fetchScoreHistory: () => api.get<CreditScoreHistory>('/api/credit/scores/recent'),
  fetchExpectedRates: () => api.get<ExpectedRates>('/api/credit/rates/expected'),
  fetchPaymentHistory: () => api.get<PaymentHistory>('/api/credit/payments'),
  fetchCardUsage: () => api.get<CardUsage>('/api/credit/card-usage'),
}

// ── 비금융 납부 이력 (GET /api/credit/payments) ───────────────────────────

export type PaymentType = 'HEALTH_INSURANCE' | 'NATIONAL_PENSION' | 'TELECOM'
export type PaymentStatus = 'PAID' | 'LATE' | 'UNPAID'

export interface PaymentRecord {
  /** 청구월 "yyyy-MM". 납부한 달은 보통 그 다음 달이다. */
  billingMonth: string
  amount: number
  dueDate: string
  /** 실제 납부일. 미납이면 null. */
  paidDate: string | null
  status: PaymentStatus
  /** 화면에 그대로 쓰는 상태 문구. 정본은 서버다. */
  statusLabel: string
}

export interface PaymentTypeHistory {
  paymentType: PaymentType
  label: string
  institutionName: string
  latestBillingMonth: string
  /** 최근부터 연속으로 제때 낸 개월 수. 연체가 나오면 거기서 끊긴다. */
  onTimeStreak: number
  lateCount: number
  /** 최신 청구월부터. */
  records: PaymentRecord[]
}

export interface PaymentHistory {
  monthsCovered: number
  onTimeCount: number
  lateCount: number
  unpaidCount: number
  /** 건강보험료 → 국민연금 → 통신요금 순. 이력이 없는 종류는 빠진다. */
  types: PaymentTypeHistory[]
}

// ── 카드 한도 대비 이용률 (GET /api/credit/card-usage) ────────────────────

export interface CardUsageItem {
  cardId: number
  cardName: string
  creditLimit: number
  /** 이번 달 사용액(원). */
  usage: number
  /** 한도 대비 이용률(%). */
  utilization: number
}

export interface CardUsageTrend {
  /** "yyyy-MM". */
  month: string
  usage: number
  utilization: number
}

export interface CardUsage {
  months: number
  /** "yyyy-MM". 신용카드가 없으면 null. */
  currentMonth: string | null
  totalCreditLimit: number
  currentUsage: number
  /** 총 사용액 / 총 한도. 신용카드가 없으면 null — 0%와 구별해야 한다. */
  currentUtilization: number | null
  /** 한도가 있는 카드만. 체크·선불카드는 빠진다. */
  cards: CardUsageItem[]
  /** 오래된 달부터. 거래가 없는 달도 0으로 채워져 있다. */
  trend: CardUsageTrend[]
}

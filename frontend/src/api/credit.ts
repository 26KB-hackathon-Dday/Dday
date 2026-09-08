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
}

import { ApiError } from './types'
import { api } from './client'

/**
 * 지원금 도메인 API.
 *
 * - 전체 목록·상세: 백엔드 `/api/v1/welfare-programs` (공개, 인증 불필요).
 * - 홈 대시보드(`fetchHome`)·수급 여부(`updateReceivingStatus`): `/api/v1/me/subsidies/**` (JWT 필요).
 *
 * 백엔드 계약 정본: `backend/.../welfare/dto/response/*.java`
 */

// ── 전체 목록 (GET /api/v1/welfare-programs) ──────────────────────────────

export type GrantStatus = 'OPEN' | 'CLOSED'
export type GrantAmountType = 'MONTHLY' | 'FIXED' | 'LIMIT' | 'SEMIANNUAL'

export interface GrantListItem {
  programId: string
  name: string
  /** 정규화 카테고리 (주거·생활·자산형성·기타). 수집 전이면 null. */
  category: string | null
  /** 지원 내용 한 줄 (예: "월 200,000원"). 금액 미상이면 null. */
  benefitText: string | null
  /** 기간 한 줄 (예: "상시 접수", "~ 2026.12.31 마감"). */
  periodText: string | null
  status: GrantStatus
}

/** 백엔드 `PageResponse<T>` 봉투. */
export interface GrantPage {
  content: GrantListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

// ── 상세 (GET /api/v1/welfare-programs/{programId}) ──────────────────────

export interface GrantDetail {
  programId: string
  name: string
  agency: string | null
  category: string | null
  /** 제도 한 줄 설명 (목적·취지). 백엔드 servDgst. */
  description: string | null
  targetDescription: string | null
  supportAmount: number | null
  supportAmountType: GrantAmountType | null
  supportDurationMonths: number | null
  applicationDeadline: string | null
  ongoingApplication: boolean
  benefitText: string | null
  /** 지원 기간 문구 (예: "최대 12개월간 지원"). */
  benefitNote: string | null
  periodText: string | null
  status: GrantStatus
  /**
   * "예상 수입 변화" 카드. 금액 지원형(CASH)이고 월별일 때만 채워진다.
   * "현재/수령 시" 절대 금액은 유저 소득이 필요해 아직 없음 — 월 지원액·개월 수만.
   */
  incomeChange: {
    title: string
    monthlyAmount: number
    durationMonths: number | null
  } | null
  /** 필요 서류. 현재는 항상 빈 배열 (상세 API에 구조화 필드 없음). */
  requiredDocuments: string[]
  applicationChannel: {
    name: string | null
    url: string | null
    phone: string | null
  }
}

export const grantApi = {
  /** 전체 지원제도 검색·목록. `q`·`category`는 서로 독립 필터. */
  fetchList(params: { q?: string; category?: string; size?: number } = {}): Promise<GrantPage> {
    const search = new URLSearchParams()
    if (params.q) search.set('q', params.q)
    if (params.category) search.set('category', params.category)
    search.set('size', String(params.size ?? 50))
    return api.get<GrantPage>(`/api/v1/welfare-programs?${search.toString()}`)
  },

  /** 제도 상세. 없는 programId면 404 → null. */
  async fetchDetail(programId: string): Promise<GrantDetail | null> {
    try {
      return await api.get<GrantDetail>(`/api/v1/welfare-programs/${encodeURIComponent(programId)}`)
    } catch (e) {
      if (e instanceof ApiError && e.code === 'PROGRAM_NOT_FOUND') return null
      throw e
    }
  },

  /** 지원금 매칭 홈. 자격 판별 이력이 없으면 서버가 이 호출에서 먼저 판별한다. */
  async fetchHome(): Promise<GrantHome> {
    return toGrantHome(await api.get<SubsidyHomeResponse>('/api/v1/me/subsidies/home'))
  },

  /** 수급 여부 등록/수정 (되묻기 결과). 자격 없는 제도면 INELIGIBLE_PROGRAM. */
  updateReceivingStatus(programId: string, receiving: boolean): Promise<void> {
    return api.patch(`/api/v1/me/subsidies/${encodeURIComponent(programId)}/receiving-status`, {
      receivingStatus: receiving ? 'RECEIVING' : 'NOT_RECEIVING',
    })
  },
}

// ── 홈 대시보드 (GET /api/v1/me/subsidies/home) ─────────────────────────

/** 백엔드 `SubsidyHomeResponse`. */
interface SubsidyHomeResponse {
  summary: {
    confirmed: number
    receiving: number
    actionNeeded: number
    needsReview: number
    totalMonthlyEquivalentAmount: number
    evaluatedAt: string | null
  }
  receivingList: SubsidyCard[]
  missing: SubsidyCard[]
  reviewQueue: SubsidyCard[]
}

interface SubsidyCard {
  programId: string
  name: string
  category: string | null
  benefitText: string | null
  monthlyEquivalentAmount: number | null
  matchedCriteria: string[]
}

/** 판별 조건 키 → 사람이 읽는 라벨. 서버가 `matched_criteria`에 키만 준다. */
const CRITERIA_LABEL: Record<string, string> = {
  protectionEndDate: '자립준비청년 대상',
  protectionPhase: '보호종료 후 지원 대상',
  region: '거주 지역 일치',
}

function toReasons(matched: string[]): GrantReason[] {
  return matched.map((k) => ({ met: true, title: CRITERIA_LABEL[k] ?? k, detail: '' }))
}

function toCard(c: SubsidyCard, tag: GrantCard['tags'][number], withReasons: boolean): GrantCard {
  return {
    id: c.programId,
    title: c.name,
    amountText: c.benefitText ?? '지원 내용 확인 필요',
    tags: [tag],
    ...(withReasons && c.matchedCriteria.length
      ? {
          reasons: toReasons(c.matchedCriteria),
          reasonFootnote: '현재 정보를 기준으로 일부 조건에 해당하여 추천되었습니다.',
        }
      : {}),
  }
}

function toGrantHome(res: SubsidyHomeResponse): GrantHome {
  return {
    summary: {
      confirmed: res.summary.confirmed,
      receiving: res.summary.receiving,
      actionNeeded: res.summary.actionNeeded,
      needsReview: res.summary.needsReview,
      lastUpdated: res.summary.evaluatedAt
        ? res.summary.evaluatedAt.slice(0, 10).replace(/-/g, '.')
        : '-',
    },
    missing: res.missing.map((c) => toCard(c, { label: '신청 가능성 높음', tone: 'teal' }, true)),
    receivingList: res.receivingList.map((c) => toCard(c, { label: '수급 중', tone: 'teal' }, false)),
    reviewQueue: res.reviewQueue.map((c) => ({
      id: c.programId,
      programName: c.name,
      amountText: c.benefitText ?? '',
    })),
  }
}

// ── 프론트 화면용 타입 ─────────────────────────────────────────────────

/**
 * "나에게 추천된 이유" 한 줄. `/me` 자격판별(SUBSIDY-002) 결과물이라 상세 API엔 없다 —
 * 홈 응답에만 실어 홈의 바텀시트에서만 보여준다.
 */
export interface GrantReason {
  /** 이미 충족한 조건이면 true, 확인이 더 필요하면 false. */
  met: boolean
  title: string
  detail: string
}

export interface GrantCard {
  /** 실제 제도 ID (WLF…) — 상세로 이동 가능해야 하므로 목도 실 ID를 쓴다. */
  id: string
  tags: { label: string; tone: 'teal' | 'blue' | 'neutral' }[]
  title: string
  amountText: string
  /** "놓치고 있을 수 있는 지원" 카드만 채워진다. 있으면 "왜 추천됐나요?" 버튼을 띄운다. */
  reasons?: GrantReason[]
  reasonFootnote?: string
}

export interface GrantReviewItem {
  id: string
  programName: string
  amountText: string
}

export interface GrantHome {
  summary: {
    confirmed: number
    receiving: number
    actionNeeded: number
    needsReview: number
    lastUpdated: string
  }
  missing: GrantCard[]
  receivingList: GrantCard[]
  reviewQueue: GrantReviewItem[]
}


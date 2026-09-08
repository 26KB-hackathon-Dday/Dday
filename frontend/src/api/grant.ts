import { ApiError } from './types'
import { api } from './client'

/**
 * 지원금 도메인 API.
 *
 * - 전체 목록·상세: 백엔드 `/api/v1/welfare-programs` 에 연결됨.
 * - 홈 대시보드(`fetchHome`): 아직 목 데이터. 유저별 자격 판별(SUBSIDY-002)·수급 상태가
 *   있어야 하는데 `/me/subsidies` 엔드포인트가 없다 (docs/welfare-api/matching-spec.md).
 *
 * 백엔드 계약 정본: `backend/.../welfare/dto/response/WelfareProgram*Response.java`
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

  /** 홈 대시보드. TODO: `/me/subsidies` 생기면 실호출로 교체. */
  fetchHome: (): Promise<GrantHome> => Promise.resolve(MOCK_HOME),
}

// ── 홈 대시보드 (목 — /me/subsidies 미구현) ──────────────────────────────

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

const MOCK_HOME: GrantHome = {
  summary: {
    confirmed: 6,
    receiving: 3,
    actionNeeded: 2,
    needsReview: 1,
    lastUpdated: '2026.09.07',
  },
  missing: [
    {
      id: 'WLF00004661',
      tags: [{ label: '신청 가능성 높음', tone: 'teal' }],
      title: '청년월세 지원사업',
      amountText: '월 200,000원',
      reasons: [
        { met: true, title: '연령 조건 충족', detail: '현재 만 28세로 대상에 포함됩니다.' },
        { met: true, title: '거주 지역 일치', detail: '서울시 거주 등록이 확인되었습니다.' },
        {
          met: false,
          title: '소득/재산 기준 확인 필요',
          detail: '본인 및 가구원 소득액 조회가 필요합니다.',
        },
      ],
      reasonFootnote: '현재 정보를 기준으로 일부 조건에 해당하여 추천되었습니다.',
    },
    {
      id: 'WLF00000060',
      tags: [
        { label: '신청 가능성 높음', tone: 'blue' },
        { label: '상시 접수', tone: 'neutral' },
      ],
      title: '청년내일저축계좌',
      amountText: '월 100,000원',
      reasons: [
        { met: true, title: '연령 조건 충족', detail: '만 19~34세 가입 연령에 해당합니다.' },
        { met: true, title: '근로·사업소득 확인', detail: '월 10만원 이상 소득이 확인되었습니다.' },
        {
          met: false,
          title: '가구 소득인정액 확인 필요',
          detail: '중위소득 100% 이하 여부 조회가 필요합니다.',
        },
      ],
      reasonFootnote: '현재 정보를 기준으로 일부 조건에 해당하여 추천되었습니다.',
    },
  ],
  receivingList: [
    {
      id: 'WLF00001175',
      tags: [{ label: '수급 중', tone: 'teal' }],
      title: '자립준비청년 자립수당 지급',
      amountText: '월 500,000원',
    },
    {
      id: 'WLF00006199',
      tags: [{ label: '수급 중', tone: 'teal' }],
      title: '자립준비청년 생활보조수당 지원',
      amountText: '월 200,000원',
    },
    {
      id: 'WLF00005445',
      tags: [{ label: '지급 완료', tone: 'neutral' }],
      title: '자립준비청년(보호종료아동) 자립정착금 지원',
      amountText: '1,000만원 (1회)',
    },
  ],
  reviewQueue: [{ id: 'WLF00001175', programName: '자립수당', amountText: '월 500,000원' }],
}

/**
 * 마이데이터(금융기관 연동) API + 도메인 타입.
 *
 * `connect`는 실제 백엔드를 부른다. 서버가 동의 기록 → 데모 목데이터 준비 → 첫 동기화를
 * 한 번에 끝내므로, 응답이 오면 소비 내역 화면을 바로 열 수 있다.
 *
 * ⚠️ 기관 목록(`findInstitutions`)은 아직 서버 API가 없어 Mock이다.
 *    화면에서 고른 기관과 무관하게 서버는 데모 계좌 전부를 붙인다.
 */
import { api } from '@/api/client'

export type InstitutionCategory = 'BANK' | 'CARD' | 'SECURITIES'

export interface Institution {
  /** 기관 코드. 연동 요청 때 이 값을 보낸다 */
  institutionId: string
  /** 화면에 그대로 띄우는 이름. 서버가 내려주므로 프론트에 매핑표를 두지 않는다 */
  name: string
  category: InstitutionCategory
  /** 로고 이미지 URL. 없으면 화면에서 이니셜로 대체한다 */
  logoUrl?: string
}

export interface ConnectRequest {
  /** 사용자가 고른 기관 id 목록 */
  institutionIds: string[]
}

export interface ConnectedAccount {
  /** 기관 코드(예: 004). 식별용이고 화면에 띄우지 않는다 */
  institutionId: string
  /** 화면에 그대로 띄우는 기관 이름(예: 국민은행). 서버가 만들어 준다 */
  institutionName: string
  /** 마스킹된 계좌번호 (예: 110-***-4567) */
  accountNumber: string
  balance: number
}

export interface ConnectResponse {
  connectedCount: number
  accounts: ConnectedAccount[]
}

const delay = (ms = 400) => new Promise((resolve) => setTimeout(resolve, ms))

const MOCK_INSTITUTIONS: Institution[] = [
  { institutionId: 'KB', name: '국민은행', category: 'BANK' },
  { institutionId: 'SHINHAN', name: '신한은행', category: 'BANK' },
  { institutionId: 'WOORI', name: '우리은행', category: 'BANK' },
  { institutionId: 'HANA', name: '하나은행', category: 'BANK' },
  { institutionId: 'NH', name: '농협은행', category: 'BANK' },
  { institutionId: 'KAKAO', name: '카카오뱅크', category: 'BANK' },
  { institutionId: 'TOSS', name: '토스뱅크', category: 'BANK' },
  { institutionId: 'HYUNDAI_CARD', name: '현대카드', category: 'CARD' },
  { institutionId: 'SAMSUNG_CARD', name: '삼성카드', category: 'CARD' },
]

export const mydataApi = {
  /** 연동 가능한 기관 목록 */
  async findInstitutions(): Promise<Institution[]> {
    console.log('[mock] GET /mydata/institutions')
    await delay()
    return MOCK_INSTITUTIONS
    // TODO(real): return api.get<Institution[]>('/mydata/institutions')
  },

  /**
   * 선택한 기관 연동 요청.
   *
   * 서버는 어떤 기관을 골랐는지 보지 않는다 — 데모 계정의 계좌·카드를 통째로 붙인다.
   * 기관별 선택을 실제로 반영하려면 서버에 기관 필터가 생겨야 한다.
   */
  async connect(_body: ConnectRequest): Promise<ConnectResponse> {
    return api.post<ConnectResponse>('/api/mydata/connect')
  },
}

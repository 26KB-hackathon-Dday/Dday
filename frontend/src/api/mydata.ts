/**
 * 마이데이터(금융기관 연동) API + 도메인 타입.
 *
 * `connect`는 실제 백엔드를 부른다. 서버가 동의 기록 → 데모 목데이터 준비 → 첫 동기화를
 * 한 번에 끝내므로, 응답이 오면 소비 내역 화면을 바로 열 수 있다.
 *
 * ⚠️ 기관 목록(`findInstitutions`)은 아직 서버 API가 없어 Mock이다.
 *    화면에서 고른 기관과 무관하게 서버는 데모 계좌 전부를 붙인다.
 *    다만 `institutionId`는 백엔드 `Institution` enum의 실제 기관코드와 맞춰뒀다 —
 *    연결 해제(`disconnectInstitution`)가 이 id를 그대로 orgCode로 쓴다.
 */
import { api } from '@/api/client'
import logoKookmin from '@/assets/logos/kookmin.svg'
import logoShinhan from '@/assets/logos/shinhan.svg'
import logoWoori from '@/assets/logos/woori.svg'
import logoHana from '@/assets/logos/hana.svg'
import logoNonghyup from '@/assets/logos/nonghyup.svg'
import logoKakaobank from '@/assets/logos/kakaobank.svg'
import logoTossbank from '@/assets/logos/tossbank.svg'
import logoHyundaiCard from '@/assets/logos/hyundai-card.svg'
import logoSamsungCard from '@/assets/logos/samsung-card.png'

export type InstitutionCategory = 'BANK' | 'CARD' | 'SECURITIES'

export interface Institution {
  /** 기관 코드(백엔드 `Institution` enum의 code). 연동·해제 요청 때 이 값을 보낸다 */
  institutionId: string
  /** 화면에 그대로 띄우는 이름. 서버가 내려주므로 프론트에 매핑표를 두지 않는다 */
  name: string
  category: InstitutionCategory
  /** 로고 이미지 URL. 없으면 화면에서 이니셜로 대체한다 */
  logoUrl?: string
  /** 가장 많이 쓰는 대표 기관 3개 — 목록 화면이 기본으로 이것만 먼저 보여준다 */
  popular?: boolean
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

export type AccountType = 'DEPOSIT' | 'SAVINGS' | 'LOAN'

/** 연동 계좌 한 건. 기관 이름은 안 내려온다 — orgCode만으로 표시해야 하면 서버에 필드 추가가 필요하다 */
export interface UserAccount {
  accountId: number
  orgCode: string
  accountName: string | null
  productName: string | null
  accountType: AccountType
  balance: number
  availableBalance: number | null
  selected: boolean
  active: boolean
  lastSyncedAt: string | null
}

export type CardType = 'CREDIT' | 'CHECK' | 'PREPAID' | 'ETC'

/** 연동 카드 한 건. */
export interface UserCard {
  cardId: number
  orgCode: string
  cardName: string | null
  cardType: CardType
  creditLimit: number | null
  selected: boolean
  active: boolean
  lastSyncedAt: string | null
}

const delay = (ms = 400) => new Promise((resolve) => setTimeout(resolve, ms))

/**
 * 은행 7개 + 카드사 5개 — 백엔드 `Institution` enum과 코드를 맞췄다(대출 전용 기관인
 * 현대캐피탈·SBI저축은행은 연동 화면에서 고를 대상이 아니라 뺐다).
 *
 * 대표 3개(`popular`)는 국내 이용자 수 기준 상위권 은행/카드사로 골랐다.
 */
const MOCK_INSTITUTIONS: Institution[] = [
  { institutionId: '004', name: '국민은행', category: 'BANK', logoUrl: logoKookmin, popular: true },
  { institutionId: '090', name: '카카오뱅크', category: 'BANK', logoUrl: logoKakaobank, popular: true },
  { institutionId: '088', name: '신한은행', category: 'BANK', logoUrl: logoShinhan, popular: true },
  { institutionId: '020', name: '우리은행', category: 'BANK', logoUrl: logoWoori },
  { institutionId: '081', name: '하나은행', category: 'BANK', logoUrl: logoHana },
  { institutionId: '011', name: '농협은행', category: 'BANK', logoUrl: logoNonghyup },
  { institutionId: '092', name: '토스뱅크', category: 'BANK', logoUrl: logoTossbank },

  { institutionId: '0306', name: '신한카드', category: 'CARD', logoUrl: logoShinhan, popular: true },
  { institutionId: '0301', name: 'KB국민카드', category: 'CARD', logoUrl: logoKookmin, popular: true },
  { institutionId: '0303', name: '삼성카드', category: 'CARD', logoUrl: logoSamsungCard, popular: true },
  { institutionId: '0302', name: '현대카드', category: 'CARD', logoUrl: logoHyundaiCard },
  { institutionId: '0313', name: '우리카드', category: 'CARD', logoUrl: logoWoori },
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

  /** 기관 하나의 연결 해제. orgCode는 화면 표시용이 아니라 서버 식별값(예: "004")이다 */
  disconnectInstitution: (orgCode: string) =>
    api.delete<void>(`/api/mydata/institutions/${orgCode}`),

  /** 연동된 계좌 목록. 마이페이지 금융정보 관리 화면이 쓴다 */
  fetchAccounts: () => api.get<{ accounts: UserAccount[] }>('/api/mydata/accounts'),

  /** 연동된 카드 목록. 마이페이지 금융정보 관리 화면이 쓴다 */
  fetchCards: () => api.get<{ cards: UserCard[] }>('/api/mydata/cards'),
}

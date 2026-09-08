/**
 * 마이데이터(금융기관 연동) API + 도메인 타입.
 *
 * ⚠️ 해커톤용 Mock 구현이다. auth.ts와 같은 방식으로 `// TODO(real)` 줄을
 * 살리면 실제 호출로 바뀐다.
 */

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
  institutionId: string
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

  /** 선택한 기관 연동 요청 */
  async connect(body: ConnectRequest): Promise<ConnectResponse> {
    console.log('[mock] POST /mydata/connect', body)
    await delay(1200) // 실제 연동은 오래 걸린다 — 로딩 화면 확인용
    return {
      connectedCount: body.institutionIds.length,
      accounts: body.institutionIds.map((institutionId, i) => ({
        institutionId,
        accountNumber: `110-***-${1000 + i}`,
        balance: 1_000_000 + i * 250_000,
      })),
    }
    // TODO(real): return api.post<ConnectResponse>('/mydata/connect', body)
  },
}

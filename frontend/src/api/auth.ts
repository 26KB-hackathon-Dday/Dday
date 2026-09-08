/**
 * 인증 API + 그 요청/응답 타입.
 *
 * 도메인 타입은 `api/{도메인}.ts`에 둔다(AGENTS.md §3). `types.ts`는 봉투 전용이다.
 *
 * ⚠️ 해커톤용 Mock 구현이다. 백엔드가 붙으면 각 함수의 `// TODO(real)` 줄을
 * 살리고 그 위의 mock 블록을 지우면 된다 — 시그니처는 그대로 둔다.
 */

// ── 요청 ──────────────────────────────────────────────

export interface PhoneSendRequest {
  /** 하이픈 없는 11자리 (01012345678) */
  phone: string
}

export interface PhoneVerifyRequest {
  phone: string
  /** 6자리 인증번호 */
  code: string
}

export interface EmailCheckRequest {
  email: string
}

export interface SignupRequest {
  name: string
  phone: string
  email: string
  password: string
  /** 동의한 약관 키 목록 (예: ['service', 'privacy']) */
  agreements: string[]
  /** 연동하기로 선택한 기관 id 목록 */
  institutionIds: string[]
}

export interface LoginRequest {
  email: string
  password: string
}

// ── 응답 ──────────────────────────────────────────────

export interface PhoneSendResponse {
  /** 인증번호 유효시간(초). 화면 타이머가 이 값을 쓴다 */
  expiresIn: number
}

export interface PhoneVerifyResponse {
  verified: boolean
}

export interface EmailCheckResponse {
  /** true면 가입 가능한 이메일 */
  available: boolean
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
}

export interface MemberSummary {
  memberId: number
  name: string
  email: string
}

export interface LoginResponse extends TokenResponse {
  member: MemberSummary
}

export type SignupResponse = LoginResponse

// ── Mock 유틸 ─────────────────────────────────────────

/** 네트워크가 있는 척. 화면의 로딩 상태를 실제처럼 확인하려고 둔다. */
const delay = (ms = 400) => new Promise((resolve) => setTimeout(resolve, ms))

/** Mock 구간에서만 쓰는 가짜 토큰. 실제 JWT 형식이 아니어도 상관없다. */
const mockTokens = (): TokenResponse => ({
  accessToken: `mock-access-token-${Date.now()}`,
  refreshToken: `mock-refresh-token-${Date.now()}`,
})

export const authApi = {
  /** 휴대폰 인증번호 발송 */
  async sendPhoneCode(body: PhoneSendRequest): Promise<PhoneSendResponse> {
    console.log('[mock] POST /auth/phone/send', body)
    await delay()
    return { expiresIn: 180 }
    // TODO(real): return api.post<PhoneSendResponse>('/auth/phone/send', body)
  },

  /**
   * 휴대폰 인증번호 확인.
   *
   * ⚠️ Mock에서는 코드가 **"000000"일 때만** 통과한다. 그 외에는 실패 응답과
   * 같은 모양으로 `verified: false`를 돌려준다 — 화면은 이 값만 보면 된다.
   */
  async verifyPhoneCode(body: PhoneVerifyRequest): Promise<PhoneVerifyResponse> {
    console.log('[mock] POST /auth/phone/verify', body)
    await delay()
    return { verified: body.code === '000000' }
    // TODO(real): return api.post<PhoneVerifyResponse>('/auth/phone/verify', body)
  },

  /** 이메일 중복 확인 */
  async checkEmail(body: EmailCheckRequest): Promise<EmailCheckResponse> {
    console.log('[mock] GET /auth/email/check', body)
    await delay()
    // Mock: 'test@dday.com'만 이미 쓰이는 이메일로 취급한다.
    return { available: body.email !== 'test@dday.com' }
    // TODO(real): return api.get<EmailCheckResponse>(`/auth/email/check?email=${encodeURIComponent(body.email)}`)
  },

  /** 회원가입 — 6단계에서 모은 값을 한 번에 보낸다 */
  async signup(body: SignupRequest): Promise<SignupResponse> {
    console.log('[mock] POST /auth/signup', body)
    await delay(600)
    return {
      ...mockTokens(),
      member: { memberId: 1, name: body.name, email: body.email },
    }
    // TODO(real): return api.post<SignupResponse>('/auth/signup', body)
  },

  /** 로그인 */
  async login(body: LoginRequest): Promise<LoginResponse> {
    console.log('[mock] POST /auth/login', body)
    await delay()
    return {
      ...mockTokens(),
      member: { memberId: 1, name: '홍길동', email: body.email },
    }
    // TODO(real): return api.post<LoginResponse>('/auth/login', body)
  },
}

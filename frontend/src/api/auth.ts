import { api } from './client'

/**
 * 인증 API + 그 요청/응답 타입.
 *
 * 도메인 타입은 `api/{도메인}.ts`에 둔다(AGENTS.md §3). `types.ts`는 봉투 전용이다.
 *
 * 백엔드 계약 정본: `backend/src/main/java/com/dday/domain/auth/`의
 * 컨트롤러 · DTO · `AuthErrorCode`.
 */

// ── 공통 ──────────────────────────────────────────────

/** 가입용 코드로 비밀번호를 바꾸지 못하게 서버가 구분하는 값 */
export type VerificationPurpose = 'SIGNUP' | 'PASSWORD_RESET'

// ── 요청 ──────────────────────────────────────────────

export interface PhoneSendRequest {
  /** "010-1234-5678" 형식. 하이픈 필수 */
  phone: string
  purpose: VerificationPurpose
}

export interface PhoneVerifyRequest {
  phone: string
  /** 6자리 인증번호. Mock 서버에서는 항상 "000000" */
  code: string
}

export interface EmailCheckRequest {
  email: string
}

export interface SignupRequest {
  name: string
  /** "010-1234-5678" 형식 */
  phone: string
  email: string
  password: string
  /** 이용약관 동의 (필수) */
  agreedTerms: boolean
  /** 개인정보 수집·이용 동의 (필수) */
  agreedPrivacy: boolean
  /** 위치정보 동의 (선택) */
  agreedLocation: boolean
}

export interface LoginRequest {
  email: string
  password: string
  rememberMe: boolean
}

export interface TokenRefreshRequest {
  refreshToken: string
}

export interface PasswordFindRequest {
  /** 이메일 또는 휴대폰 번호 */
  emailOrPhone: string
}

export interface PasswordResetRequest {
  phone: string
  code: string
  newPassword: string
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

export interface UserSummary {
  userId: number
  name: string
  /** false면 마이데이터 연동 등 온보딩이 남아 있다 — 프론트가 그리로 보낸다 */
  onboardingCompleted: boolean
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  user: UserSummary
}

export interface SignupResponse {
  userId: number
  accessToken: string
  refreshToken: string
}

/** 액세스 토큰 재발급 응답. 리프레시 토큰은 회전시키지 않는다 */
export interface TokenResponse {
  accessToken: string
}

// ── API ───────────────────────────────────────────────

export const authApi = {
  /** 휴대폰 인증번호 발송 */
  sendPhoneCode: (body: PhoneSendRequest) =>
    api.post<PhoneSendResponse>('/api/auth/phone/send', body),

  /** 휴대폰 인증번호 확인 */
  verifyPhoneCode: (body: PhoneVerifyRequest) =>
    api.post<PhoneVerifyResponse>('/api/auth/phone/verify', body),

  /** 이메일 중복 확인 */
  checkEmail: (body: EmailCheckRequest) =>
    api.post<EmailCheckResponse>('/api/auth/email/check', body),

  /**
   * 회원가입 — `/phone/send`(purpose=SIGNUP) → `/phone/verify`를 먼저 마쳐야 한다.
   * 성공하면 바로 로그인된 상태가 되도록 토큰까지 함께 내려온다.
   */
  signup: (body: SignupRequest) => api.post<SignupResponse>('/api/auth/signup', body),

  /** 로그인 */
  login: (body: LoginRequest) => api.post<LoginResponse>('/api/auth/login', body),

  /** 로그아웃. JWT 무상태라 서버는 할 일이 없다 — 토큰은 호출부가 스토어에서 지운다 */
  logout: () => api.post<void>('/api/auth/logout'),

  /** 액세스 토큰 재발급 */
  reissue: (body: TokenRefreshRequest) => api.post<TokenResponse>('/api/auth/token/refresh', body),

  /** 비밀번호 찾기 (인증번호 발송) */
  findPassword: (body: PasswordFindRequest) =>
    api.post<PhoneSendResponse>('/api/auth/password/find', body),

  /** 비밀번호 재설정 */
  resetPassword: (body: PasswordResetRequest) =>
    api.post<void>('/api/auth/password/reset', body),
}

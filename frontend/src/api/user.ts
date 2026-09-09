import { api } from './client'

/**
 * 회원 정보 (`GET /api/users/me`). 홈·마이페이지가 쓴다.
 * 온보딩 전에는 프로필 값이 전부 `null` — `onboardingCompleted`로 분기한다.
 
 * 회원 도메인 API.
 *
 * 백엔드 계약 정본: `backend/.../user/dto/response/UserResponse.java`
 */

export interface Me {
  userId: number
  email: string
  name: string
  phone: string
  onboardingCompleted: boolean
  /** D-day 계산 기준. `YYYY-MM-DD`. 온보딩 전이면 `null`. */
  protectionEndDate: string | null
  regionCode: string | null
  /** 온보딩 시점 보유 자산(원). */
  initialAsset: number | null
  mydataConnected: boolean
}

export interface UserUpdateRequest {
  name?: string
  /** "010-1234-5678" 형식 */
  phone?: string
}

export interface PasswordChangeRequest {
  currentPassword: string
  newPassword: string
}

export interface UserWithdrawRequest {
  /** 100자 이하. 선택 입력이라 안 보내도 된다 */
  reason?: string
}

export const userApi = {
  /**
   * 로그인한 회원 정보.
   *
   * 로그인 응답에도 이름이 들어 있지만 저장해두지 않는다 — 개명하면 화면 문구가
   * 조용히 어긋난다. 이름을 쓰는 화면은 그때그때 여기서 읽는다.
   */
  fetchMe: () => api.get<Me>('/api/users/me'),

  /** 기본정보 수정. PATCH라 보내지 않은 필드는 바뀌지 않는다. 이메일은 여기서 못 바꾼다 */
  updateMe: (body: UserUpdateRequest) => api.patch<Me>('/api/users/me', body),

  /** 비밀번호 변경. 현재 비밀번호를 알아야 한다 */
  changePassword: (body: PasswordChangeRequest) =>
    api.patch<void>('/api/users/me/password', body),

  /** 회원 탈퇴. 데이터를 지우지 않고 상태만 WITHDRAWN으로 바꾼다. 사유는 선택 */
  withdraw: (body?: UserWithdrawRequest) => api.delete<void>('/api/users/me', body),
}

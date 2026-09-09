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

export const userApi = {
  /**
   * 로그인한 회원 정보.
   *
   * 로그인 응답에도 이름이 들어 있지만 저장해두지 않는다 — 개명하면 화면 문구가
   * 조용히 어긋난다. 이름을 쓰는 화면은 그때그때 여기서 읽는다.
   */
  fetchMe: () => api.get<Me>('/api/users/me'),
}

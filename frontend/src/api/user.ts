import { api } from './client'

/**
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

import { api } from './client'

/**
 * 회원 정보 (`GET /api/users/me`). 홈·마이페이지가 쓴다.
 * 온보딩 전에는 프로필 값이 전부 `null` — `onboardingCompleted`로 분기한다.
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
  fetchMe: () => api.get<Me>('/api/users/me'),
}

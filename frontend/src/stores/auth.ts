import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { ACCESS_TOKEN_KEY, ONBOARDING_COMPLETED_KEY, REFRESH_TOKEN_KEY } from '@/api/client'

/** 로그인/가입 응답 중 토큰 저장에 필요한 부분만. */
interface Tokens {
  accessToken: string
  refreshToken: string
}

/**
 * 로그인 토큰 보관소.
 *
 * localStorage가 정본이고 스토어는 그 사본이다 — 새로고침해도 살아남아야 하고,
 * Axios 요청 인터셉터가 스토어를 거치지 않고 localStorage에서 직접 꺼내 쓴다.
 * 그래서 토큰을 건드리는 곳은 이 스토어 하나로 모은다.
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem(ACCESS_TOKEN_KEY))
  const refreshToken = ref<string | null>(localStorage.getItem(REFRESH_TOKEN_KEY))

  const isLoggedIn = computed(() => !!accessToken.value)

  const onboardingCompleted = ref(localStorage.getItem(ONBOARDING_COMPLETED_KEY) === 'true')

  /** 로그인·회원가입 성공 응답을 그대로 넘기면 된다. */
  function setTokens(tokens: Tokens) {
    accessToken.value = tokens.accessToken
    refreshToken.value = tokens.refreshToken
    localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken)
  }

  /** 로그인 응답의 `user.onboardingCompleted` 또는 온보딩 완료(`/complete`) 성공 시 부른다.
   *  라우터 가드가 새로고침 후에도 읽어야 하므로 localStorage에도 남긴다. */
  function setOnboardingCompleted(value: boolean) {
    onboardingCompleted.value = value
    localStorage.setItem(ONBOARDING_COMPLETED_KEY, String(value))
  }

  /** 토큰만 지운다. 화면 이동은 부르는 쪽이 정한다. */
  function logout() {
    accessToken.value = null
    refreshToken.value = null
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(ONBOARDING_COMPLETED_KEY)
  }

  return {
    accessToken,
    refreshToken,
    isLoggedIn,
    onboardingCompleted,
    setTokens,
    setOnboardingCompleted,
    logout,
  }
})

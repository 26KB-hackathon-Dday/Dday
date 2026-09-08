import { createRouter, createWebHistory } from 'vue-router'

import LandingView from '@/views/auth/LandingView.vue'
import LoginView from '@/views/auth/LoginView.vue'

import SignupStartView from '@/views/signup/SignupStartView.vue'
import SignupNameView from '@/views/signup/SignupNameView.vue'
import SignupPhoneView from '@/views/signup/SignupPhoneView.vue'
import SignupPhoneVerifyView from '@/views/signup/SignupPhoneVerifyView.vue'
import SignupEmailView from '@/views/signup/SignupEmailView.vue'
import SignupPasswordView from '@/views/signup/SignupPasswordView.vue'
import SignupTermsView from '@/views/signup/SignupTermsView.vue'
import SignupDoneView from '@/views/signup/SignupDoneView.vue'

import MydataIntroView from '@/views/signup/MydataIntroView.vue'
import MydataConsentView from '@/views/signup/MydataConsentView.vue'
import MydataSelectView from '@/views/signup/MydataSelectView.vue'
import MydataDoneView from '@/views/signup/MydataDoneView.vue'

import PocketView from '@/views/PocketView.vue'

/**
 * 라우트 정의.
 *
 * 온보딩 화면은 전부 정적 import다 — 개수가 적고 순서대로 이어지므로
 * 코드 스플리팅해봐야 단계마다 네트워크 왕복만 늘어난다.
 *
 * `meta.requiresAuth`가 붙은 화면은 아래 가드가 로그인 여부를 확인한다.
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'landing', component: LandingView },
    { path: '/login', name: 'login', component: LoginView },

    // 회원가입 6단계
    { path: '/signup', redirect: '/signup/start' },
    { path: '/signup/start', name: 'signup-start', component: SignupStartView },
    { path: '/signup/name', name: 'signup-name', component: SignupNameView },
    { path: '/signup/phone', name: 'signup-phone', component: SignupPhoneView },
    { path: '/signup/phone/verify', name: 'signup-phone-verify', component: SignupPhoneVerifyView },
    { path: '/signup/email', name: 'signup-email', component: SignupEmailView },
    { path: '/signup/password', name: 'signup-password', component: SignupPasswordView },
    { path: '/signup/terms', name: 'signup-terms', component: SignupTermsView },
    { path: '/signup/done', name: 'signup-done', component: SignupDoneView },

    // 마이데이터 연동
    { path: '/mydata', name: 'mydata-intro', component: MydataIntroView },
    { path: '/mydata/consent', name: 'mydata-consent', component: MydataConsentView },
    { path: '/mydata/select', name: 'mydata-select', component: MydataSelectView },
    { path: '/mydata/done', name: 'mydata-done', component: MydataDoneView },

    // 온보딩을 마치면 도착하는 홈
    { path: '/pockets', name: 'pockets', component: PocketView, meta: { requiresAuth: true } },

    // 없는 주소는 랜딩으로. SPA라 새로고침으로도 들어올 수 있다
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  /** 단계 이동마다 화면 맨 위에서 시작한다. 뒤로가기는 원래 위치를 살린다 */
  scrollBehavior: (_to, _from, saved) => saved ?? { top: 0 },
})

router.beforeEach((to) => {
  // 스토어 대신 localStorage를 본다 — 가드는 Pinia가 준비되기 전에도 돈다.
  const isLoggedIn = !!localStorage.getItem('accessToken')
  if (to.meta.requiresAuth && !isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})

export default router


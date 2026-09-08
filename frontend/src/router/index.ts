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

import GrantMatchHomeView from '@/views/welfare/GrantMatchHomeView.vue'

import OnboardingIntroView from '@/views/onboarding/OnboardingIntroView.vue'
import ProtectionDateView from '@/views/onboarding/ProtectionDateView.vue'
import ProtectionDateConfirmView from '@/views/onboarding/ProtectionDateConfirmView.vue'
import ProtectionDateCheckpointView from '@/views/onboarding/ProtectionDateCheckpointView.vue'
import RegionView from '@/views/onboarding/RegionView.vue'
import RegionDistrictView from '@/views/onboarding/RegionDistrictView.vue'
import RegionConfirmView from '@/views/onboarding/RegionConfirmView.vue'
import HousingTypeView from '@/views/onboarding/HousingTypeView.vue'
import HousingCostView from '@/views/onboarding/HousingCostView.vue'
import HousingCheckpointView from '@/views/onboarding/HousingCheckpointView.vue'
import IncomeListView from '@/views/onboarding/IncomeListView.vue'
import IncomeNewView from '@/views/onboarding/IncomeNewView.vue'
import AssetsSettlementView from '@/views/onboarding/AssetsSettlementView.vue'
import AssetsSavedView from '@/views/onboarding/AssetsSavedView.vue'
import ReviewView from '@/views/onboarding/ReviewView.vue'
import ProcessingView from '@/views/onboarding/ProcessingView.vue'
import OnboardingDoneView from '@/views/onboarding/OnboardingDoneView.vue'

declare module 'vue-router' {
  interface RouteMeta {
    /**
     * 상단바(AppTopBar)에 표시할 화면 제목.
     * 온보딩 화면은 자체 헤더를 그려서 제목이 없으므로 선택값이다.
     */
    title?: string
    /** true면 공용 상단바·하단 내비를 숨긴다 (화면이 자체 크롬을 그리는 경우). */
    hideChrome?: boolean
    /** true면 아래 가드가 로그인 여부를 확인한다. */
    requiresAuth?: boolean
  }
}

/**
 * 라우트 정의.
 *
 * 크게 두 묶음이다.
 *
 * - **온보딩(랜딩·로그인·가입·마이데이터)** — 전부 정적 import다. 개수가 적고 순서대로
 *   이어지므로 코드 스플리팅해봐야 단계마다 네트워크 왕복만 늘어난다.
 *   자체 헤더를 그리므로 `hideChrome: true`로 공용 크롬을 끈다.
 * - **로그인 후 앱 화면(홈·포켓·지원금·신용 관리)** — 공용 상단바·하단 내비 안에서 돈다.
 *   `meta.title`이 상단바 제목이 되고, 하단 탭은 모두 최상위 경로라 exact 활성만 보면 된다.
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  // 화면 전환 시 맨 위로. 뒤로 가기는 이전 스크롤 위치를 복원한다.
  scrollBehavior: (_to, _from, savedPosition) => savedPosition ?? { top: 0 },
  routes: [
    // ── 온보딩 (로그인 전) ────────────────────────────────────────────────
    { path: '/landing', name: 'landing', component: LandingView, meta: { hideChrome: true } },
    { path: '/login', name: 'login', component: LoginView, meta: { hideChrome: true } },

    // 회원가입 6단계
    { path: '/signup', redirect: '/signup/start' },
    {
      path: '/signup/start',
      name: 'signup-start',
      component: SignupStartView,
      meta: { hideChrome: true },
    },
    {
      path: '/signup/name',
      name: 'signup-name',
      component: SignupNameView,
      meta: { hideChrome: true },
    },
    {
      path: '/signup/phone',
      name: 'signup-phone',
      component: SignupPhoneView,
      meta: { hideChrome: true },
    },
    {
      path: '/signup/phone/verify',
      name: 'signup-phone-verify',
      component: SignupPhoneVerifyView,
      meta: { hideChrome: true },
    },
    {
      path: '/signup/email',
      name: 'signup-email',
      component: SignupEmailView,
      meta: { hideChrome: true },
    },
    {
      path: '/signup/password',
      name: 'signup-password',
      component: SignupPasswordView,
      meta: { hideChrome: true },
    },
    {
      path: '/signup/terms',
      name: 'signup-terms',
      component: SignupTermsView,
      meta: { hideChrome: true },
    },
    {
      path: '/signup/done',
      name: 'signup-done',
      component: SignupDoneView,
      meta: { hideChrome: true },
    },

    // 마이데이터 연동
    {
      path: '/mydata',
      name: 'mydata-intro',
      component: MydataIntroView,
      meta: { hideChrome: true },
    },
    {
      path: '/mydata/consent',
      name: 'mydata-consent',
      component: MydataConsentView,
      meta: { hideChrome: true },
    },
    {
      path: '/mydata/select',
      name: 'mydata-select',
      component: MydataSelectView,
      meta: { hideChrome: true },
    },
    {
      path: '/mydata/done',
      name: 'mydata-done',
      component: MydataDoneView,
      meta: { hideChrome: true },
    },

    // 온보딩(자립 계획 생성) — 회원가입 완료 또는 onboardingCompleted=false 로그인 시 가드가 여기로 보낸다.
    { path: '/onboarding', redirect: '/onboarding/intro' },
    {
      path: '/onboarding/intro',
      name: 'onboarding-intro',
      component: OnboardingIntroView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/protection-date',
      name: 'onboarding-protection-date',
      component: ProtectionDateView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/protection-date/confirm',
      name: 'onboarding-protection-date-confirm',
      component: ProtectionDateConfirmView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/protection-date/checkpoint',
      name: 'onboarding-protection-date-checkpoint',
      component: ProtectionDateCheckpointView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/region',
      name: 'onboarding-region',
      component: RegionView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/region/district',
      name: 'onboarding-region-district',
      component: RegionDistrictView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/region/confirm',
      name: 'onboarding-region-confirm',
      component: RegionConfirmView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/housing-type',
      name: 'onboarding-housing-type',
      component: HousingTypeView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/housing-cost',
      name: 'onboarding-housing-cost',
      component: HousingCostView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/housing-checkpoint',
      name: 'onboarding-housing-checkpoint',
      component: HousingCheckpointView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/income',
      name: 'onboarding-income',
      component: IncomeListView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/income/new',
      name: 'onboarding-income-new',
      component: IncomeNewView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/assets/settlement',
      name: 'onboarding-assets-settlement',
      component: AssetsSettlementView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/assets/saved',
      name: 'onboarding-assets-saved',
      component: AssetsSavedView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/review',
      name: 'onboarding-review',
      component: ReviewView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/processing',
      name: 'onboarding-processing',
      component: ProcessingView,
      meta: { hideChrome: true, requiresAuth: true },
    },
    {
      path: '/onboarding/done',
      name: 'onboarding-done',
      component: OnboardingDoneView,
      meta: { hideChrome: true, requiresAuth: true },
    },

    // ── 앱 화면 (로그인 후) — 하단 탭 순서와 같다 ─────────────────────────
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/PlaceholderView.vue'),
      // 로그인 전에 여기로 들어오면 가드가 랜딩으로 보낸다. 하단바의 '홈' 탭이 이 경로다.
      meta: { title: '홈', requiresAuth: true },
    },
    {
      path: '/pockets',
      name: 'pockets',
      component: () => import('@/views/PocketView.vue'),
      meta: { title: '내 포켓', requiresAuth: true },
    },
    {
      path: '/pockets/budget-initial',
      name: 'pocket-budget-initial',
      component: () => import('@/views/pocket/PocketBudgetInitialView.vue'),
      meta: {
        title: '내 포켓',
      },
    },
    {
      path: '/pockets/budget-adjust',
      name: 'pocket-budget-adjust',
      component: () => import('@/views/pocket/PocketBudgetAdjustView.vue'),
      meta: {
        title: '내 포켓',
      },
    },
    {
      path: '/grants',
      name: 'grants',
      component: GrantMatchHomeView,
      // 개인화된 매칭 결과라 로그인이 필요하다. 전체 목록(/grants/all)·상세는 공개.
      meta: { title: '지원금 매칭', requiresAuth: true },
    },
    {
      // '/grants/:id'보다 먼저 둔다 ('all'이 id로 잡히지 않도록).
      path: '/grants/all',
      name: 'grant-list',
      component: () => import('@/views/welfare/GrantListView.vue'),
      meta: { title: '전체 지원제도' },
    },
    {
      path: '/grants/:id',
      name: 'grant-detail',
      component: () => import('@/views/welfare/GrantDetailView.vue'),
      meta: { title: '지원 상세', hideChrome: true },
    },
    {
      path: '/credit-manage',
      name: 'credit-manage',
      component: () => import('@/views/credit/CreditHomeView.vue'),
      meta: { title: '신용 관리', requiresAuth: true },
    },
    {
      path: '/credit-manage/rates',
      name: 'credit-expected-rates',
      component: () => import('@/views/credit/ExpectedRateView.vue'),
      meta: { title: '예상 금리', requiresAuth: true },
    },
    {
      // 하단 탭엔 없고 상단바 계정 아이콘으로만 들어온다.
      path: '/mypage',
      name: 'mypage',
      component: () => import('@/views/MyPageView.vue'),
      meta: { title: '마이페이지', requiresAuth: true },
    },

    {
      path: '/pockets/unexpected-income/allocate',
      name: 'pocket-unexpected-income-allocate',
      component: () => import('@/views/pocket/UnexpectedIncomeAllocateView.vue'),
      meta: {
        title: '내 포켓',
      },
    },
    {
      path: '/pockets/income-match-test',
      name: 'pocket-income-match-test',
      component: () => import('@/views/pocket/IncomeMatchTestView.vue'),
      meta: {
        title: '내 포켓',
      },
    },
    {
      path: '/pockets/budget-readjust',
      name: 'pocket-budget-readjust',
      component: () => import('@/views/pocket/PocketBudgetReAdjustView.vue'),
      meta: {
        title: '내 포켓',
      },
    },
    {
      path: '/pockets/unexpected-income-test',
      name: 'pocket-unexpected-income-test',
      component: () => import('@/views/pocket/UnexpectedIncomeModalTestView.vue'),
      meta: {
        title: '내 포켓',
      },
    },
    {
      path: '/pockets/budget-readjust/confirm',
      name: 'pocket-budget-readjust-confirm',
      component: () => import('@/views/pocket/PocketBudgetReAdjustConfirmView.vue'),
      meta: {
        title: '내 포켓',
      },
    },
    {
      path: '/pockets/unexpected-income/amount',
      name: 'pocket-unexpected-income-amount',
      component: () => import('@/views/pocket/UnexpectedIncomeAmountView.vue'),
      meta: {
        title: '추가할 금액 지정',
      },
    },
    {
      // 모든 정적 /pockets/* 경로 뒤에 둬 budget-* 화면을 pocketType으로 오인하지 않게 한다.
      path: '/pockets/:pocketType',
      name: 'pocket-detail',
      component: () => import('@/views/pocket/PocketDetailView.vue'),
      meta: { title: '필수 포켓', requiresAuth: true },
    },
    // 없는 주소는 홈으로. SPA라 새로고침으로도 들어올 수 있다
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  // 스토어 대신 localStorage를 본다 — 가드는 Pinia가 준비되기 전에도 돈다.
  const isLoggedIn = !!localStorage.getItem('accessToken')
  if (to.meta.requiresAuth && !isLoggedIn) {
    // 로그인 화면이 아니라 랜딩으로 보낸다. 처음 온 사용자에게는 가입 경로가 먼저 보여야 한다.
    return { name: 'landing' }
  }

  // 로그인은 했지만 온보딩(자립 계획 생성)을 안 끝낸 사용자는 온보딩 화면으로 보낸다.
  // 온보딩 화면 자체는 무한 리다이렉트를 막기 위해 제외한다.
  if (
    to.meta.requiresAuth &&
    isLoggedIn &&
    localStorage.getItem('onboardingCompleted') !== 'true' &&
    !to.path.startsWith('/onboarding')
  ) {
    return { name: 'onboarding-intro' }
  }
})

export default router

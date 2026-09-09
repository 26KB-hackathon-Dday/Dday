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
    title?: string
    hideChrome?: boolean
    requiresAuth?: boolean
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),

  scrollBehavior: (_to, _from, savedPosition) => {
    return (
      savedPosition ?? {
        top: 0,
      }
    )
  },

  routes: [
    /*
     * =========================
     * 로그인 전
     * =========================
     */

    {
      path: '/landing',
      name: 'landing',
      component: LandingView,

      meta: {
        hideChrome: true,
      },
    },

    {
      path: '/login',
      name: 'login',
      component: LoginView,

      meta: {
        hideChrome: true,
      },
    },

    /*
     * =========================
     * 회원가입
     * =========================
     */

    {
      path: '/signup',
      redirect: '/signup/start',
    },

    {
      path: '/signup/start',
      name: 'signup-start',
      component: SignupStartView,

      meta: {
        hideChrome: true,
      },
    },

    {
      path: '/signup/name',
      name: 'signup-name',
      component: SignupNameView,

      meta: {
        hideChrome: true,
      },
    },

    {
      path: '/signup/phone',
      name: 'signup-phone',
      component: SignupPhoneView,

      meta: {
        hideChrome: true,
      },
    },

    {
      path: '/signup/phone/verify',

      name: 'signup-phone-verify',

      component: SignupPhoneVerifyView,

      meta: {
        hideChrome: true,
      },
    },

    {
      path: '/signup/email',
      name: 'signup-email',
      component: SignupEmailView,

      meta: {
        hideChrome: true,
      },
    },

    {
      path: '/signup/password',
      name: 'signup-password',
      component: SignupPasswordView,

      meta: {
        hideChrome: true,
      },
    },

    {
      path: '/signup/terms',
      name: 'signup-terms',
      component: SignupTermsView,

      meta: {
        hideChrome: true,
      },
    },

    {
      path: '/signup/done',
      name: 'signup-done',
      component: SignupDoneView,

      meta: {
        hideChrome: true,
      },
    },

    /*
     * =========================
     * 마이데이터 가입
     * =========================
     */

    {
      path: '/mydata',
      name: 'mydata-intro',
      component: MydataIntroView,

      meta: {
        hideChrome: true,
      },
    },

    {
      path: '/mydata/consent',
      name: 'mydata-consent',
      component: MydataConsentView,

      meta: {
        hideChrome: true,
      },
    },

    {
      path: '/mydata/select',
      name: 'mydata-select',
      component: MydataSelectView,

      meta: {
        hideChrome: true,
      },
    },

    {
      path: '/mydata/done',
      name: 'mydata-done',
      component: MydataDoneView,

      meta: {
        hideChrome: true,
      },
    },

    /*
     * =========================
     * 온보딩
     * =========================
     */

    {
      path: '/onboarding',
      redirect: '/onboarding/intro',
    },

    {
      path: '/onboarding/intro',

      name: 'onboarding-intro',

      component: OnboardingIntroView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/protection-date',

      name: 'onboarding-protection-date',

      component: ProtectionDateView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/protection-date/confirm',

      name: 'onboarding-protection-date-confirm',

      component: ProtectionDateConfirmView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/protection-date/checkpoint',

      name: 'onboarding-protection-date-checkpoint',

      component: ProtectionDateCheckpointView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/region',

      name: 'onboarding-region',

      component: RegionView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/region/district',

      name: 'onboarding-region-district',

      component: RegionDistrictView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/region/confirm',

      name: 'onboarding-region-confirm',

      component: RegionConfirmView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/housing-type',

      name: 'onboarding-housing-type',

      component: HousingTypeView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/housing-cost',

      name: 'onboarding-housing-cost',

      component: HousingCostView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/housing-checkpoint',

      name: 'onboarding-housing-checkpoint',

      component: HousingCheckpointView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/income',

      name: 'onboarding-income',

      component: IncomeListView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/income/new',

      name: 'onboarding-income-new',

      component: IncomeNewView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/assets/settlement',

      name: 'onboarding-assets-settlement',

      component: AssetsSettlementView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/assets/saved',

      name: 'onboarding-assets-saved',

      component: AssetsSavedView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/review',

      name: 'onboarding-review',

      component: ReviewView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/processing',

      name: 'onboarding-processing',

      component: ProcessingView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    {
      path: '/onboarding/done',

      name: 'onboarding-done',

      component: OnboardingDoneView,

      meta: {
        hideChrome: true,
        requiresAuth: true,
      },
    },

    /*
     * =========================
     * 홈
     * =========================
     */

    {
      path: '/',
      name: 'home',

      component: () => import('@/views/HomeView.vue'),

      meta: {
        title: '홈',
        requiresAuth: true,
      },
    },

    {
      path: '/asset-forecast',
      name: 'asset-forecast',
      component: () => import('@/views/AssetForecastView.vue'),
      meta: {
        title: '지원 종료 시 예상 자산',
        requiresAuth: true,
      },
    },

    /*
     * =========================
     * 포켓
     * =========================
     */

    {
      path: '/pockets',
      name: 'pockets',

      component: () => import('@/views/PocketView.vue'),

      meta: {
        title: '내 포켓',
        requiresAuth: true,
      },
    },

    {
      path: '/pockets/budget-initial',

      name: 'pocket-budget-initial',

      component: () => import('@/views/pocket/PocketBudgetInitialView.vue'),

      meta: {
        title: '내 포켓',
        requiresAuth: true,
      },
    },

    {
      path: '/pockets/budget-adjust',

      name: 'pocket-budget-adjust',

      component: () => import('@/views/pocket/PocketBudgetAdjustView.vue'),

      meta: {
        title: '내 포켓',
        requiresAuth: true,
      },
    },

    /*
     * 진행 중 예산 재조정
     */

    {
      path: '/pockets/budget-readjust',

      name: 'pocket-budget-readjust',

      component: () => import('@/views/pocket/PocketBudgetReAdjustView.vue'),

      meta: {
        title: '내 포켓',
        requiresAuth: true,
      },
    },

    {
      path: '/pockets/budget-readjust/confirm',

      name: 'pocket-budget-readjust-confirm',

      component: () => import('@/views/pocket/PocketBudgetReAdjustConfirmView.vue'),

      meta: {
        title: '내 포켓',
        requiresAuth: true,
      },
    },

    /*
     * 예상 밖 수입
     */

    {
      path: '/pockets/unexpected-income-test',

      name: 'pocket-unexpected-income-test',

      component: () => import('@/views/pocket/UnexpectedIncomeModalTestView.vue'),

      meta: {
        title: '내 포켓',
        requiresAuth: true,
      },
    },

    {
      path: '/pockets/unexpected-income/amount',

      name: 'pocket-unexpected-income-amount',

      component: () => import('@/views/pocket/UnexpectedIncomeAmountView.vue'),

      meta: {
        title: '추가할 금액 지정',
        requiresAuth: true,
      },
    },

    {
      path: '/pockets/unexpected-income/allocate',

      name: 'pocket-unexpected-income-allocate',

      component: () => import('@/views/pocket/UnexpectedIncomeAllocateView.vue'),

      meta: {
        title: '내 포켓',
        requiresAuth: true,
      },
    },

    /*
     * 포켓 상세.
     *
     * budget-* 같은 정적 경로보다
     * 반드시 아래에 있어야 한다.
     */
    {
      path: '/pockets/:pocketType',

      name: 'pocket-detail',

      component: () => import('@/views/pocket/PocketDetailView.vue'),

      meta: {
        title: '필수 포켓',
        requiresAuth: true,
      },

      beforeEnter: (to) => {
        const titles: Record<string, string> = {
          ESSENTIAL: '필수 포켓',

          FREE: '자유 포켓',

          EMERGENCY: '비상금 포켓',

          FUTURE_ASSET: '미래자산 포켓',
        }

        const pocketType = typeof to.params.pocketType === 'string' ? to.params.pocketType : ''

        if (!titles[pocketType]) {
          return {
            name: 'pockets',

            query: {
              month: to.query.month,
            },
          }
        }

        to.meta.title = titles[pocketType]
      },
    },

    /*
     * =========================
     * 지원금
     * =========================
     */

    {
      path: '/grants',
      name: 'grants',

      component: GrantMatchHomeView,

      meta: {
        title: '지원금 매칭',
        requiresAuth: true,
      },
    },

    {
      path: '/grants/all',
      name: 'grant-list',

      component: () => import('@/views/welfare/GrantListView.vue'),

      meta: {
        title: '전체 지원제도',
        requiresAuth: true,
      },
    },

    {
      path: '/grants/:id',
      name: 'grant-detail',

      component: () => import('@/views/welfare/GrantDetailView.vue'),

      meta: {
        title: '지원 상세',
        hideChrome: true,
        requiresAuth: true,
      },
    },

    /*
     * =========================
     * 신용 관리
     * =========================
     */

    {
      path: '/credit-manage',

      name: 'credit-manage',

      component: () => import('@/views/credit/CreditHomeView.vue'),

      meta: {
        title: '신용 관리',
        requiresAuth: true,
      },
    },

    {
      path: '/credit-manage/rates',

      name: 'credit-expected-rates',

      component: () => import('@/views/credit/ExpectedRateView.vue'),

      meta: {
        title: '예상 금리',
        requiresAuth: true,
      },
    },

    {
      path: '/credit-manage/learn',

      name: 'credit-learn',

      component: () => import('@/views/credit/CreditLearnView.vue'),

      meta: {
        title: '신용관리 하기',
        requiresAuth: true,
      },
    },

    {
      path: '/credit-manage/payments',

      name: 'credit-payments',

      component: () => import('@/views/credit/PaymentHistoryView.vue'),

      meta: {
        title: '납부 기록',
        requiresAuth: true,
      },
    },

    {
      path: '/credit-manage/card-usage',

      name: 'credit-card-usage',

      component: () => import('@/views/credit/CardUsageView.vue'),

      meta: {
        title: '카드 사용 현황',
        requiresAuth: true,
      },
    },

    {
      path: '/credit-manage/loans',

      name: 'credit-loans',

      component: () => import('@/views/credit/LoanStatusView.vue'),

      meta: {
        title: '대출 현황',
        requiresAuth: true,
      },
    },

    /*
     * =========================
     * 마이페이지
     * =========================
     */

    {
      path: '/mypage',
      name: 'mypage',

      component: () => import('@/views/MyPageView.vue'),

      meta: {
        title: '마이페이지',
        requiresAuth: true,
      },
    },

    {
      path: '/mypage/edit',

      name: 'mypage-edit',

      component: () => import('@/views/mypage/MyPageEditView.vue'),

      meta: {
        title: '기본 정보 수정',
        requiresAuth: true,
      },
    },

    {
      path: '/mypage/password',

      name: 'mypage-password',

      component: () => import('@/views/mypage/PasswordChangeView.vue'),

      meta: {
        title: '보안 및 비밀번호',
        requiresAuth: true,
      },
    },

    {
      path: '/mypage/terms',

      name: 'mypage-terms',

      component: () => import('@/views/mypage/TermsView.vue'),

      meta: {
        title: '이용약관',
        requiresAuth: true,
      },
    },

    {
      path: '/mypage/privacy',

      name: 'mypage-privacy',

      component: () => import('@/views/mypage/PrivacyPolicyView.vue'),

      meta: {
        title: '개인정보처리방침',
        requiresAuth: true,
      },
    },

    {
      path: '/mypage/mydata',

      name: 'mypage-mydata',

      component: () => import('@/views/mypage/MydataManageView.vue'),

      meta: {
        title: '금융정보 연결 관리',
        requiresAuth: true,
      },
    },

    {
      path: '/mypage/withdraw',

      name: 'mypage-withdraw',

      component: () => import('@/views/mypage/WithdrawView.vue'),

      meta: {
        title: '회원탈퇴',
        requiresAuth: true,
      },
    },

    /*
     * 반드시 마지막.
     */
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach((to) => {
  const isLoggedIn = !!localStorage.getItem('accessToken')

  if (to.meta.requiresAuth && !isLoggedIn) {
    return {
      name: 'landing',
    }
  }

  if (
    to.meta.requiresAuth &&
    isLoggedIn &&
    localStorage.getItem('onboardingCompleted') !== 'true' &&
    !to.path.startsWith('/onboarding')
  ) {
    return {
      name: 'onboarding-intro',
    }
  }
})

export default router

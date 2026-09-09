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
    return savedPosition ?? { top: 0 }
  },

  routes: [
    // ─────────────────────────────
    // 로그인 전
    // ─────────────────────────────

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

    // ─────────────────────────────
    // 회원가입
    // ─────────────────────────────

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

    // ─────────────────────────────
    // 마이데이터
    // ─────────────────────────────

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

    // ─────────────────────────────
    // 홈
    // ─────────────────────────────

    {
      path: '/',
      name: 'home',
      component: () => import('@/views/PlaceholderView.vue'),

      meta: {
        title: '홈',
        requiresAuth: true,
      },
    },

    // ─────────────────────────────
    // 포켓
    // ─────────────────────────────

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

    // ─────────────────────────────
    // 예산 재조정
    // ─────────────────────────────

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

    // ─────────────────────────────
    // 예상 밖 수입
    // ─────────────────────────────

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

    // ─────────────────────────────
    // 지원금
    // ─────────────────────────────

    {
      path: '/grants',
      name: 'grants',
      component: GrantMatchHomeView,

      meta: {
        title: '지원금 매칭',
      },
    },

    {
      path: '/grants/all',
      name: 'grant-list',

      component: () => import('@/views/welfare/GrantListView.vue'),

      meta: {
        title: '전체 지원제도',
      },
    },

    {
      path: '/grants/:id',
      name: 'grant-detail',

      component: () => import('@/views/welfare/GrantDetailView.vue'),

      meta: {
        title: '지원 상세',
        hideChrome: true,
      },
    },

    // ─────────────────────────────
    // 신용 관리
    // ─────────────────────────────

    {
      path: '/credit-manage',
      name: 'credit-manage',

      component: () => import('@/views/PlaceholderView.vue'),

      meta: {
        title: '신용 관리',
        requiresAuth: true,
      },
    },

    // ─────────────────────────────
    // 반드시 마지막
    // ─────────────────────────────

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
})

export default router

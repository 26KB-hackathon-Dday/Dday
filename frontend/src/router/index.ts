import { createRouter, createWebHistory } from 'vue-router'
import GrantMatchHomeView from '@/views/welfare/GrantMatchHomeView.vue'

declare module 'vue-router' {
  interface RouteMeta {
    /** 상단바에 표시할 화면 제목 */
    title: string
    /** true면 공용 상단바·하단 내비를 숨긴다 (화면이 자체 크롬을 그리는 경우). */
    hideChrome?: boolean
  }
}

/**
 * meta.title — 상단바(AppTopBar)가 그대로 띄운다.
 * 탭 경로는 모두 최상위라 하단바가 exact 활성만 보면 된다.
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  // 화면 전환 시 맨 위로. 뒤로 가기는 이전 스크롤 위치를 복원한다.
  scrollBehavior: (_to, _from, savedPosition) => savedPosition ?? { top: 0 },
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/PlaceholderView.vue'),
      meta: { title: '홈' },
    },
    {
      path: '/pockets',
      name: 'pockets',
      component: () => import('@/views/PocketView.vue'),
      meta: { title: '내 포켓' },
    },
    {
      path: '/grants',
      name: 'grants',
      component: GrantMatchHomeView,
      meta: { title: '지원금 매칭' },
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
      path: '/mypage',
      name: 'mypage',
      component: () => import('@/views/PlaceholderView.vue'),
      meta: { title: '마이페이지' },
    },
  ],
})

export default router

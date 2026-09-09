<script setup lang="ts">
/**
 * 하단 탭 내비. 탭 목록은 여기서 소유하고, 활성 표시는 라우트로 판단한다.
 * 탭 경로와 그 하위 경로(예: /grants → /grants/all)에서 해당 탭을 켠다.
 *
 * 아이콘은 단색 라인 SVG(`assets/icons/nav-*.svg`, `benefit.svg`)를 AppIcon의 CSS 마스크로
 * 그린다 — 색이 `currentColor`라 활성/비활성은 탭의 `color`만 바꾸면 된다.
 */
import { useRoute } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'

const route = useRoute()

const tabs = [
  { to: '/', label: '홈', icon: 'nav-home' },
  { to: '/pockets', label: '내 포켓', icon: 'nav-pocket' },
  { to: '/grants', label: '지원금', icon: 'benefit' },
  { to: '/credit-manage', label: '신용 관리', icon: 'nav-credit' },
]

// '/' 은 정확히 일치할 때만. 나머지는 하위 경로(예: /grants/all)도 그 탭을 켠다.
const isActive = (to: string) =>
  to === '/' ? route.path === '/' : route.path === to || route.path.startsWith(to + '/')
</script>

<template>
  <nav class="bottomnav">
    <RouterLink
      v-for="tab in tabs"
      :key="tab.to"
      :to="tab.to"
      class="bottomnav__tab"
      :class="{ 'is-active': isActive(tab.to) }"
    >
      <span class="bottomnav__iconbox">
        <AppIcon :name="tab.icon" :size="22" />
      </span>
      <span class="bottomnav__label">{{ tab.label }}</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.bottomnav {
  position: sticky;
  bottom: 0;
  z-index: 10;
  display: flex;
  height: 64px;
  background: var(--c-bg);
  border-top: 1px solid var(--c-border);
  padding-bottom: env(safe-area-inset-bottom);
}

.bottomnav__tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 0;
  color: var(--c-text-3);
}

.bottomnav__iconbox {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 24px;
}
.bottomnav__iconbox :deep(.app-icon) {
  transition: transform 0.3s ease;
}
.bottomnav__tab.is-active {
  color: #000;
}
/* 활성 탭 아이콘은 살짝 키운다. */
.bottomnav__tab.is-active .bottomnav__iconbox :deep(.app-icon) {
  transform: scale(1.3);
}

.bottomnav__label {
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
}
</style>

<script setup lang="ts">
/**
 * 하단 탭 내비. 탭 목록은 여기서 소유하고, 활성 표시는 라우트로 판단한다.
 * 탭 경로와 그 하위 경로(예: /grants → /grants/all)에서 해당 탭을 켠다.
 *
 * 아이콘은 임베디드 래스터가 든 손그림 SVG라 CSS 마스크가 안 먹는다 — `<img>`로 렌더하고,
 * 활성 탭은 진한 `dark-*` 아이콘으로 교체한다 (불투명도로 흐리지 않는다).
 */
import { useRoute } from 'vue-router'

import homeIcon from '@/assets/icons/home.svg'
import homeIconActive from '@/assets/icons/dark-home.svg'
import pocketIcon from '@/assets/icons/pocket.svg'
import pocketIconActive from '@/assets/icons/dark-pocket.svg'
import welfareIcon from '@/assets/icons/welfare.svg'
import welfareIconActive from '@/assets/icons/dark-welfare.svg'
import creditIcon from '@/assets/icons/credit.svg'
import creditIconActive from '@/assets/icons/dark-credit.svg'

const route = useRoute()

const tabs = [
  { to: '/', label: '홈', icon: homeIcon, iconActive: homeIconActive },
  { to: '/pockets', label: '내 포켓', icon: pocketIcon, iconActive: pocketIconActive },
  { to: '/grants', label: '지원금', icon: welfareIcon, iconActive: welfareIconActive },
  { to: '/credit-manage', label: '신용 관리', icon: creditIcon, iconActive: creditIconActive },
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
        <img
          :src="isActive(tab.to) ? tab.iconActive : tab.icon"
          alt=""
          class="bottomnav__icon"
        />
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
.bottomnav__icon {
  max-height: 24px;
  max-width: 26px;
  width: auto;
  object-fit: contain;
  transition: transform 0.3s ease;
}
.bottomnav__tab.is-active {
  color: #000;
}
/* 활성 탭 아이콘은 살짝 키운다. */
.bottomnav__tab.is-active .bottomnav__icon {
  transform: scale(1.3);
}

.bottomnav__label {
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
}
</style>

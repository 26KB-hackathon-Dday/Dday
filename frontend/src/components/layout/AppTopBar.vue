<script setup lang="ts">
/**
 * 상단 앱바. 제목은 라우트 meta.title에서 온다 (router/index.ts).
 * 뒤로 갈 곳이 있을 때만 화살표를, 마이페이지 화면이 아닐 때 우측에 알림·계정 아이콘을 띄운다.
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import ddayLogo from '@/assets/dday-logo.png'

const route = useRoute()
const router = useRouter()

const title = computed(() => route.meta.title ?? '')
const pathsWithoutBack = new Set([
  '/',
  '/pockets',
  '/pockets/budget-initial',
  '/grants',
  '/credit-manage',
])
// route가 바뀔 때마다 다시 계산되도록 fullPath를 참조한다 (history.state는 반응형이 아니다).
const canGoBack = computed(
  () =>
    route.fullPath != null &&
    !pathsWithoutBack.has(route.path) &&
    window.history.state?.back != null,
)
// 마이페이지 진입점 — 하단 탭엔 없어서 상단바가 유일한 경로다. 마이페이지 자신에서는 숨긴다.
const showAccount = computed(() => route.name !== 'mypage')

function goBack() {
  router.back()
}
</script>

<template>
  <header class="topbar">
    <button v-if="canGoBack" type="button" class="topbar__back" aria-label="뒤로" @click="goBack">
      <AppIcon name="back" :size="16" />
    </button>
    <img v-else class="topbar__logo" :src="ddayLogo" alt="D-1825" />
    <div class="topbar__heading">
      <h1 class="topbar__title">{{ title }}</h1>
    </div>
    <button v-if="showAccount" type="button" class="topbar__bell" aria-label="알림">
      <AppIcon name="bell" :size="26" />
    </button>
    <button
      v-if="showAccount"
      type="button"
      class="topbar__account"
      aria-label="마이페이지"
      @click="router.push('/mypage')"
    >
      <AppIcon name="profile" :size="22" />
    </button>
  </header>
</template>

<style scoped>
.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 24px;
  border-bottom: 1px solid var(--c-border);
  background: var(--c-bg);
}

.topbar__back {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  padding: 8px;
  margin-left: -8px;
  border-radius: 9999px;
  color: var(--c-text-2);
}

.topbar__logo {
  display: block;
  width: auto;
  height: 30px;
  object-fit: contain;
}

.topbar__back:hover {
  background: var(--c-surface);
}

.topbar__heading {
  position: absolute;
  left: 50%;
  width: calc(100% - 144px);
  min-width: 0;
  transform: translateX(-50%);
  text-align: center;
  pointer-events: none;
}

.topbar__title {
  overflow: hidden;
  font-size: 17px;
  font-weight: 500;
  letter-spacing: -0.2px;
  color: #000;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topbar__bell {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: auto;
  padding: 8px;
  border-radius: 9999px;
  color: var(--c-text);
}
.topbar__bell:hover {
  background: var(--c-surface);
}

.topbar__account {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  margin-left: 4px;
  margin-right: -8px;
  border-radius: 9999px;
  color: var(--c-text);
}
.topbar__account:hover {
  background: var(--c-surface);
}
</style>

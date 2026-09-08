<script setup lang="ts">
/**
 * 상단 앱바. 제목은 라우트 meta.title에서 온다 (router/index.ts).
 * 뒤로 갈 곳이 있을 때만 화살표를, 마이페이지 화면이 아닐 때 우측에 계정 아이콘을 띄운다.
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'

const route = useRoute()
const router = useRouter()

const title = computed(() => route.meta.title ?? '')
// route가 바뀔 때마다 다시 계산되도록 fullPath를 참조한다 (history.state는 반응형이 아니다).
const canGoBack = computed(() => route.fullPath != null && window.history.state?.back != null)
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
    <h1 class="topbar__title">{{ title }}</h1>
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
  background: var(--c-bg);
}

.topbar__back {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  padding: 8px;
  margin-left: -8px;
  border-radius: 9999px;
  color: var(--c-text-2);
}

.topbar__back:hover {
  background: var(--c-surface);
}

.topbar__title {
  flex: 1;
  min-width: 0;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: -0.2px;
  color: #000;
}

.topbar__account {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  margin-right: -8px;
  border-radius: 9999px;
  color: var(--c-text);
}
.topbar__account:hover {
  background: var(--c-surface);
}
</style>

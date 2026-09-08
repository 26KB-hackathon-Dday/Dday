<script setup lang="ts">
/**
 * 상단 앱바. 제목은 라우트 meta.title에서 온다 (router/index.ts).
 * 뒤로 갈 곳이 있을 때만 화살표를 띄운다.
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'

const route = useRoute()
const router = useRouter()

const title = computed(() => route.meta.title ?? '')
// route가 바뀔 때마다 다시 계산되도록 fullPath를 참조한다 (history.state는 반응형이 아니다).
const canGoBack = computed(() => route.fullPath != null && window.history.state?.back != null)

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
</style>

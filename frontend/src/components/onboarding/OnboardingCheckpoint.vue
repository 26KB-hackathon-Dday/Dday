<script setup lang="ts">
/** 온보딩 중간 체크포인트 화면(자립 시작·생활 정보 입력 완료) 공통 레이아웃 */
import PrimaryButton from '@/components/common/PrimaryButton.vue'

defineProps<{
  title: string
  buttonText?: string
}>()

defineEmits<{ next: [] }>()
</script>

<template>
  <div class="page">
    <div class="content">
      <div class="check-icon">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="m5 12.5 4.5 4.5L19 7" />
        </svg>
      </div>
      <h1 class="title">{{ title }}</h1>
    </div>

    <div class="actions">
      <PrimaryButton @click="$emit('next')">{{ buttonText ?? '계속하기' }}</PrimaryButton>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 0 var(--space-page);
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.check-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin-bottom: var(--space-lg);
  color: #ffffff;
  background-color: #27b87e;
  border-radius: 50%;
  animation: pop 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.check-icon svg {
  width: 36px;
  height: 36px;
}

@keyframes pop {
  from {
    transform: scale(0.6);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .check-icon {
    animation: none;
  }
}

.title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const router = useRouter()

function start() {
  router.replace('/onboarding/intro')
}
</script>

<template>
  <div class="page">
    <div class="content">
      <div class="check-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="m5 12.5 4.5 4.5L19 7" />
        </svg>
      </div>

      <h1 class="title">가입이 완료되었어요</h1>
      <p class="subtitle">이제 나에게 맞는 자립 계획을 만들어볼게요.</p>
    </div>

    <div class="actions">
      <PrimaryButton @click="start">시작하기 →</PrimaryButton>
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
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.subtitle {
  margin-top: var(--space-sm);
  font-size: 14px;
  color: var(--color-secondary);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

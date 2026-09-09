<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useOnboardingStore } from '@/stores/onboarding'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const router = useRouter()
const onboarding = useOnboardingStore()

function start() {
  router.replace('/')
}
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

      <h1 class="title">첫 번째 자립 계획이<br />완성됐어요</h1>

      <div class="card">
        <span class="card-label">목표 달성까지</span>
        <strong class="card-value">D-{{ onboarding.dDay }}</strong>
      </div>
      <div class="card">
        <span class="card-label">월 예상 수입</span>
        <strong class="card-value">₩{{ onboarding.monthlyIncome.toLocaleString('ko-KR') }}</strong>
      </div>
      <div class="card">
        <span class="card-label">월 예상 지출</span>
        <strong class="card-value">₩{{ onboarding.monthlyExpense.toLocaleString('ko-KR') }}</strong>
      </div>
    </div>

    <div class="actions">
      <PrimaryButton @click="start">내 자립 계획 시작하기</PrimaryButton>
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
  width: 100%;
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
  margin-bottom: var(--space-lg);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: -0.02em;
  color: var(--color-primary);
  text-align: center;
}

.card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 64px;
  margin-top: var(--space-sm);
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.card-label {
  font-size: 14px;
  color: var(--color-secondary);
}

.card-value {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-primary);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

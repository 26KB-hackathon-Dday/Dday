<script setup lang="ts">
import { useRouter } from 'vue-router'
import { SETTLEMENT_RECEIVED_LABEL, type SettlementReceived } from '@/api/onboarding'
import { useOnboardingStore } from '@/stores/onboarding'
import StepProgress from '@/components/signup/StepProgress.vue'

const router = useRouter()
const onboarding = useOnboardingStore()

const options: SettlementReceived[] = ['RECEIVED', 'NOT_YET', 'NONE']

function select(option: SettlementReceived) {
  onboarding.settlementReceived = option
  router.push('/onboarding/assets/saved')
}
</script>

<template>
  <div class="page">
    <StepProgress :current-step="4" :total-steps="4" />

    <div class="content">
      <h1 class="title">자립정착금을 받았나요?</h1>

      <div class="info-card">
        <strong class="info-title">자립정착금이란?</strong>
        <p class="info-body">
          보호가 종료되는 청소년이 자립을 준비할 수 있도록 지방자치단체가 지급하는 정착 지원금이에요.
        </p>
      </div>

      <div class="options">
        <button
          v-for="option in options"
          :key="option"
          type="button"
          class="option"
          @click="select(option)"
        >
          {{ SETTLEMENT_RECEIVED_LABEL[option] }}
        </button>
      </div>
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
  padding-top: var(--space-lg);
}

.title {
  margin-bottom: var(--space-lg);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.info-card {
  padding: var(--space-md);
  margin-bottom: var(--space-lg);
  background-color: var(--color-bg-soft);
  border-radius: var(--radius-md);
}

.info-title {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-primary);
}

.info-body {
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-secondary);
}

.options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option {
  height: 56px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  color: var(--color-primary);
  text-align: left;
  padding: 0 var(--space-md);
}

.option:active {
  background-color: var(--color-bg-soft);
}
</style>

<template>
  <div class="budget-page">
    <main class="budget-content">
      <section class="intro-section">
        <h2 class="intro-title">{{ monthNumber }}월 최적화 예산</h2>

        <p class="intro-description">
          이번 달 들어올 돈과 지난 소비를 바탕으로<br />
          최적화 예산을 먼저 나눠봤어요.
        </p>
      </section>

      <section class="income-card">
        <span class="income-label"> 이번 달 예상 수입 </span>

        <strong class="income-amount">
          {{ formatCurrency(budgetStore.totalBudget) }}
        </strong>
      </section>

      <section class="pocket-list">
        <div
          v-for="pocket in pockets"
          :key="pocket.type"
          class="pocket-card"
          :class="`pocket-card--${pocket.type}`"
        >
          <span class="pocket-badge" :class="`pocket-badge--${pocket.type}`">
            {{ pocket.label }}
          </span>

          <strong class="pocket-amount">
            {{ formatCurrency(pocket.amount) }}
          </strong>
        </div>
      </section>

      <section class="button-area">
        <button class="adjust-button" type="button" @click="goToAdjust">배분 조정하기</button>

        <button class="confirm-button" type="button" :disabled="submitting" @click="handleConfirm">
          {{ submitting ? '확정 중…' : '이대로 확정하기' }}
        </button>
        <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { usePocketBudgetStore } from '@/stores/pocketBudget'
import { budgetApi } from '@/api/budget'
import { ApiError } from '@/api/types'

const router = useRouter()
const route = useRoute()
const budgetStore = usePocketBudgetStore()
const submitting = ref(false)
const errorMessage = ref('')
const currentMonth = computed(() => {
  const queryMonth = typeof route.query.month === 'string' ? route.query.month : ''
  if (/^\d{4}-(0[1-9]|1[0-2])$/.test(queryMonth)) return queryMonth
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
})
const monthNumber = computed(() => Number(currentMonth.value.slice(5)))

const pockets = computed(() => [
  {
    type: 'essential',
    label: '필수 포켓',
    amount: budgetStore.essentialBudget,
  },
  {
    type: 'free',
    label: '자유 포켓',
    amount: budgetStore.freeBudget,
  },
  {
    type: 'future',
    label: '미래자산 포켓',
    amount: budgetStore.futureBudget,
  },
  {
    type: 'emergency',
    label: '비상금 포켓',
    amount: budgetStore.emergencyBudget,
  },
])

const formatCurrency = (value: number) => {
  return `${value.toLocaleString('ko-KR')}원`
}

const goToAdjust = () => {
  router.push({
    name: 'pocket-budget-adjust',
    query: { month: currentMonth.value },
  })
}

const handleConfirm = async () => {
  if (submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await budgetApi.confirmCurrent({
      totalBudgetAmount: budgetStore.totalBudget,
      pockets: [
        { pocketType: 'ESSENTIAL', amount: budgetStore.essentialBudget },
        { pocketType: 'FREE', amount: budgetStore.freeBudget },
        { pocketType: 'FUTURE_ASSET', amount: budgetStore.futureBudget },
        { pocketType: 'EMERGENCY', amount: budgetStore.emergencyBudget },
      ],
    })
    router.replace({ name: 'pockets', query: { month: currentMonth.value } })
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '예산 확정에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.budget-page {
  width: 100%;
  min-height: 100vh;

  background: #ffffff;
  color: #171717;
}

.budget-content {
  display: flex;
  flex-direction: column;

  width: 100%;
  max-width: 430px;
  min-height: calc(100vh - 56px);

  margin: 0 auto;
  padding: 34px 28px 40px;
}

.intro-section {
  margin-bottom: 28px;
}

.intro-title {
  margin: 0;

  font-size: 28px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -1px;
}

.intro-description {
  margin: 8px 0 0;

  color: #666666;

  font-size: 14px;
  font-weight: 400;
  line-height: 1.55;
}

.income-card {
  display: flex;
  align-items: center;
  justify-content: space-between;

  width: 100%;
  height: 82px;

  padding: 0 18px;

  border: 1px solid #e5e5e5;
  border-radius: 16px;

  background: #ffffff;
}

.income-label {
  font-size: 16px;
  font-weight: 700;
}

.income-amount {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.8px;
}

.pocket-list {
  display: flex;
  flex-direction: column;

  gap: 14px;

  margin-top: 68px;
}

.pocket-card {
  display: flex;
  align-items: center;
  justify-content: space-between;

  width: 100%;
  height: 80px;

  padding: 0 22px 0 32px;

  border-radius: 16px;
}

.pocket-card--essential {
  background: #f0f6ff;
}

.pocket-card--free {
  background: #fff7f1;
}

.pocket-card--future {
  background: #faf2ff;
}

.pocket-card--emergency {
  background: #ebfafa;
}

.pocket-badge {
  display: inline-flex;
  align-items: center;

  padding: 6px 11px;

  border-radius: 999px;

  font-size: 14px;
  font-weight: 600;
  line-height: 1.2;
}

.pocket-badge--essential {
  color: #397bc7;
  background: #d5e9ff;
}

.pocket-badge--free {
  color: #ef8e3d;
  background: #ffe1c7;
}

.pocket-badge--future {
  color: #bd6bea;
  background: #eed6ff;
}

.pocket-badge--emergency {
  color: #22a9ad;
  background: #c2eeee;
}

.pocket-amount {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.button-area {
  display: flex;
  flex-direction: column;

  gap: 12px;

  margin-top: auto;
  padding-top: 90px;
}

.adjust-button,
.confirm-button {
  width: 100%;
  height: 64px;

  border: none;
  border-radius: 10px;

  font-size: 17px;
  font-weight: 700;

  cursor: pointer;
}

.adjust-button {
  color: #171717;
  background: #f3f3f3;
}

.confirm-button {
  color: #ffffff;
  background: #111111;
}

.confirm-button:disabled {
  cursor: default;
  opacity: 0.6;
}

.error {
  color: var(--color-danger);
  font-size: 13px;
  text-align: center;
}

.adjust-button:active,
.confirm-button:active {
  opacity: 0.85;
}
</style>

<template>
  <div class="budget-page">
    <main class="budget-content">
      <section class="intro-section">
        <h2 class="intro-title">{{ monthNumber }}월 최적화 예산</h2>

        <p class="intro-description">
          온보딩에서 입력한 생활 정보를 바탕으로<br />
          이번 달 예산을 먼저 나눠봤어요.
        </p>
      </section>

      <div v-if="loading" class="loading-state">맞춤 예산을 계산하고 있어요.</div>

      <template v-else>
        <section class="income-card">
          <span class="income-label"> 이번 달 예상 수입 </span>

          <strong class="income-amount">
            {{ formatCurrency(budgetStore.totalBudget) }}
          </strong>
        </section>

        <p v-if="recommendationError" class="income-error">
          {{ recommendationError }}
        </p>

        <p v-else-if="budgetStore.totalBudget === 0" class="income-empty">
          등록된 정기수입이 없어요. 온보딩에서 정기수입을 먼저 등록해 주세요.
        </p>

        <!-- 개인화 기준 간단 안내 -->
        <section v-if="budgetStore.totalBudget > 0" class="recommendation-info">
          <strong class="recommendation-info__title"> 이렇게 계산했어요 </strong>

          <p class="recommendation-info__text">
            월 주거비
            {{ formatCurrency(budgetStore.monthlyHousingCost) }}을 필수생활비로 먼저 확보했어요.
          </p>

          <p class="recommendation-info__text">
            현재 모아둔 돈은
            {{ formatCurrency(budgetStore.currentAsset) }}이에요.
          </p>

          <p class="recommendation-info__text">
            {{
              budgetStore.emergencyReserveNeeded
                ? '생활방어 자금이 아직 부족해 비상금 비중을 높였어요.'
                : '생활방어 자금이 충분해 장기 자산과 자유생활에 더 배분했어요.'
            }}
          </p>
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
          <button
            class="adjust-button"
            type="button"
            :disabled="budgetStore.totalBudget <= 0"
            @click="goToAdjust"
          >
            배분 조정하기
          </button>

          <button
            class="confirm-button"
            type="button"
            :disabled="submitting || budgetStore.totalBudget <= 0"
            @click="handleConfirm"
          >
            {{ submitting ? '확정 중…' : '이대로 확정하기' }}
          </button>

          <p v-if="errorMessage" class="error" role="alert">
            {{ errorMessage }}
          </p>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { useRoute, useRouter } from 'vue-router'

import { usePocketBudgetStore } from '@/stores/pocketBudget'

import { budgetApi } from '@/api/budget'

import { ApiError } from '@/api/types'

const router = useRouter()

const route = useRoute()

const budgetStore = usePocketBudgetStore()

const submitting = ref(false)

const loading = ref(true)

const errorMessage = ref('')

const recommendationError = ref('')

const currentMonth = computed(() => {
  const queryMonth = typeof route.query.month === 'string' ? route.query.month : ''

  if (/^\d{4}-(0[1-9]|1[0-2])$/.test(queryMonth)) {
    return queryMonth
  }

  const now = new Date()

  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
})

const monthNumber = computed(() => {
  return Number(currentMonth.value.slice(5))
})

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

const formatCurrency = (value: number): string => {
  return `${Math.round(Number(value) || 0).toLocaleString('ko-KR')}원`
}

/*
 * =========================
 * 온보딩 기반 추천 예산 조회
 * =========================
 */
const loadRecommendation = async () => {
  loading.value = true

  recommendationError.value = ''

  try {
    const response = await budgetApi.getCurrentRecommendation()

    budgetStore.initializeFromRecommendation(response)
  } catch (error) {
    recommendationError.value =
      error instanceof ApiError
        ? error.message
        : error instanceof Error
          ? error.message
          : '추천 예산을 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

const goToAdjust = () => {
  if (budgetStore.totalBudget <= 0) {
    return
  }

  router.push({
    name: 'pocket-budget-adjust',

    query: {
      month: currentMonth.value,
    },
  })
}

const handleConfirm = async () => {
  if (submitting.value || budgetStore.totalBudget <= 0) {
    return
  }

  submitting.value = true

  errorMessage.value = ''

  try {
    await budgetApi.confirmCurrent({
      totalBudgetAmount: budgetStore.totalBudget,

      pockets: [
        {
          pocketType: 'ESSENTIAL',

          amount: budgetStore.essentialBudget,
        },

        {
          pocketType: 'FREE',

          amount: budgetStore.freeBudget,
        },

        {
          pocketType: 'FUTURE_ASSET',

          amount: budgetStore.futureBudget,
        },

        {
          pocketType: 'EMERGENCY',

          amount: budgetStore.emergencyBudget,
        },
      ],
    })

    await router.replace({
      name: 'pockets',

      query: {
        month: currentMonth.value,
      },
    })
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '예산 확정에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}

onMounted(loadRecommendation)
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

  box-sizing: border-box;
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
  line-height: 1.55;
}

.loading-state {
  padding: 80px 0;

  color: #777777;

  font-size: 14px;

  text-align: center;
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

  box-sizing: border-box;
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

.income-error {
  margin: 12px 0 0;

  color: #e05252;

  font-size: 12px;
}

.income-empty {
  margin: 12px 0 0;

  padding: 14px 16px;

  border-radius: 10px;

  color: #777777;
  background: #f6f6f6;

  font-size: 12px;
  line-height: 1.5;
}

.recommendation-info {
  margin-top: 18px;

  padding: 16px 18px;

  border-radius: 14px;

  background: #f7f8fa;
}

.recommendation-info__title {
  display: block;

  margin-bottom: 8px;

  font-size: 13px;
  font-weight: 700;
}

.recommendation-info__text {
  margin: 3px 0;

  color: #777777;

  font-size: 11px;
  line-height: 1.5;
}

.pocket-list {
  display: flex;
  flex-direction: column;

  gap: 14px;

  margin-top: 48px;
}

.pocket-card {
  display: flex;
  align-items: center;
  justify-content: space-between;

  width: 100%;
  height: 80px;

  padding: 0 22px 0 32px;

  border-radius: 16px;

  box-sizing: border-box;
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

  padding-top: 70px;
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

.adjust-button:disabled,
.confirm-button:disabled {
  opacity: 0.5;

  cursor: default;
}

.error {
  color: #e05252;

  font-size: 13px;

  text-align: center;
}
</style>

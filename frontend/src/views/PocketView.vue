<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import PocketStatusCard from '@/components/pocket/PocketStatusCard.vue'
import FutureAssetPocketCard from '@/components/pocket/FutureAssetPocketCard.vue'
import EmergencyPocketCard from '@/components/pocket/EmergencyPocketCard.vue'

import UnexpectedIncomeModal from '@/components/pocket/UnexpectedIncomeModal.vue'
import RecurringIncomeMatchModal from '@/components/pocket/RecurringIncomeMatchModal.vue'

import { POCKET_LABEL, POCKET_ORDER, pocketApi, type PocketMonthlyResponse } from '@/api/pocket'
import { assetForecastApi, type AssetForecastResponse } from '@/api/assetForecast'

import { unexpectedIncomeApi, type UnexpectedIncome } from '@/api/unexpectedIncome'

import { ApiError } from '@/api/types'
import { formatWon } from '@/utils/format'

import { useUnexpectedIncomeStore } from '@/stores/unexpectedIncome'

const route = useRoute()
const router = useRouter()

const unexpectedIncomeStore = useUnexpectedIncomeStore()

/*
 * =========================
 * 포켓 월별 조회
 * =========================
 */

const response = ref<PocketMonthlyResponse | null>(null)
const futureAssetForecast = ref<AssetForecastResponse | null>(null)

const loading = ref(true)

const error = ref<string | null>(null)

const errorCode = ref<string | null>(null)

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

const orderedPockets = computed(() => {
  return [...(response.value?.pockets ?? [])].sort((a, b) => {
    return POCKET_ORDER.indexOf(a.pocketType) - POCKET_ORDER.indexOf(b.pocketType)
  })
})

const needsBudgetConfirmation = computed(() => {
  return ['MONTHLY_BUDGET_NOT_FOUND', 'MONTHLY_POCKET_BUDGET_NOT_FOUND'].includes(
    errorCode.value ?? '',
  )
})

const loadPockets = async () => {
  loading.value = true

  response.value = null
  futureAssetForecast.value = null
  error.value = null
  errorCode.value = null

  try {
    response.value = await pocketApi.findMonthly(currentMonth.value)
    try {
      futureAssetForecast.value = await assetForecastApi.find()
    } catch {
      // 미래자산 부가 정보 실패가 포켓 전체 조회를 막지 않게 한다.
      futureAssetForecast.value = null
    }
    return true
  } catch (e) {
    response.value = null

    error.value = e instanceof ApiError ? e.message : '포켓 정보를 불러오지 못했습니다.'

    errorCode.value = e instanceof ApiError ? e.code : null

    if (needsBudgetConfirmation.value) {
      await router.replace({
        name: 'pocket-budget-initial',
        query: { month: currentMonth.value },
      })
      return false
    }
    return false
  } finally {
    loading.value = false
  }
}

/*
 * =========================
 * 예상 밖 수입
 * =========================
 */

const currentIncome = ref<UnexpectedIncome | null>(null)

const showUnexpectedIncomeModal = ref(false)

const showRecurringIncomeModal = ref(false)

const checkingIncome = ref(false)

const closeIncomeModals = () => {
  showUnexpectedIncomeModal.value = false

  showRecurringIncomeModal.value = false
}

const checkNewIncome = async () => {
  if (checkingIncome.value) {
    return
  }

  checkingIncome.value = true

  closeIncomeModals()

  try {
    const pending = await unexpectedIncomeApi.findPending()

    currentIncome.value = pending ?? null

    if (!currentIncome.value) {
      return
    }

    unexpectedIncomeStore.setDetectedIncome(currentIncome.value)

    switch (currentIncome.value.type) {
      case 'NEW_INCOME':
        showUnexpectedIncomeModal.value = true

        break

      case 'RECURRING_LIKELY':
      case 'RECURRING_OVER':
        showRecurringIncomeModal.value = true

        break
    }
  } catch (e) {
    /*
     * 새 입금 확인 실패가
     * 포켓 화면 전체를 막지는 않게 한다.
     */
    console.error('새 입금 조회 실패', e)
  } finally {
    checkingIncome.value = false
  }
}

const checkNextIncome = async () => {
  closeIncomeModals()

  currentIncome.value = null

  unexpectedIncomeStore.resetAll()

  await checkNewIncome()
}

const handleUnexpectedAdd = async () => {
  if (!currentIncome.value) {
    return
  }

  unexpectedIncomeStore.setDetectedIncome(currentIncome.value)

  showUnexpectedIncomeModal.value = false

  await router.push({
    name: 'pocket-unexpected-income-amount',
  })
}

const handleExclude = async () => {
  if (!currentIncome.value) {
    return
  }

  try {
    await unexpectedIncomeApi.exclude(currentIncome.value.transactionId)

    await checkNextIncome()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '입금을 처리하지 못했습니다.'
  }
}

const handleNotRecurring = () => {
  if (!currentIncome.value) {
    return
  }

  /*
   * 서버 상태를 변경하지 않고
   * 현재 거래만 일반 예상 밖 수입으로
   * 화면 전환한다.
   */
  unexpectedIncomeStore.convertToNewIncome()

  currentIncome.value = {
    ...currentIncome.value,

    type: 'NEW_INCOME',

    recurringIncomeId: null,
    recurringIncomeName: null,

    expectedAmount: null,
    depositTiming: null,

    excessAmount: 0,
  }

  showRecurringIncomeModal.value = false

  showUnexpectedIncomeModal.value = true
}

const handleAddExcess = async () => {
  if (!currentIncome.value || currentIncome.value.type !== 'RECURRING_OVER') {
    return
  }

  const excessAmount = Number(currentIncome.value.excessAmount ?? 0)

  if (excessAmount <= 0) {
    return
  }

  unexpectedIncomeStore.setDetectedIncome(currentIncome.value)

  unexpectedIncomeStore.setIncludedAmount(excessAmount)

  showRecurringIncomeModal.value = false

  await router.push({
    name: 'pocket-unexpected-income-allocate',
  })
}

/*
 * =========================
 * 화면 이동
 * =========================
 */

const openPocketDetail = (pocketType: 'ESSENTIAL' | 'FREE' | 'FUTURE_ASSET') => {
  router.push({
    name: 'pocket-detail',

    params: {
      pocketType,
    },

    query: {
      month: currentMonth.value,
    },
  })
}

const openBudgetReadjust = () => {
  router.push({
    name: 'pocket-budget-readjust',
  })
}

const load = async () => {
  const pocketsLoaded = await loadPockets()

  if (pocketsLoaded) {
    await checkNewIncome()
  }
}

watch(currentMonth, load, { immediate: true })
</script>

<template>
  <main class="page">
    <h1 v-if="response">{{ monthNumber }}월 포켓</h1>

    <p v-if="loading" class="state" role="status">포켓 현황을 불러오는 중…</p>

    <div v-else-if="error" class="state error" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="load">다시 시도</button>
    </div>

    <template v-else-if="response">
      <section class="total" aria-label="이번 달 총 예산">
        <span> {{ monthNumber }}월 총 예산 </span>

        <strong>
          {{ formatWon(response.totalBudgetAmount) }}
        </strong>
      </section>

      <section class="list" aria-label="포켓별 현황">
        <template v-for="pocket in orderedPockets" :key="pocket.pocketId">
          <FutureAssetPocketCard
            v-if="pocket.pocketType === 'FUTURE_ASSET'"
            :title="POCKET_LABEL.FUTURE_ASSET"
            :budget="pocket.targetAmount"
            :achieved-amount="futureAssetForecast?.achievedAmount"
            :achievement-rate="futureAssetForecast?.achievementRate"
            :current-asset="futureAssetForecast?.currentAsset"
            @select="openPocketDetail('FUTURE_ASSET')"
          />

          <EmergencyPocketCard
            v-else-if="pocket.pocketType === 'EMERGENCY'"
            :title="POCKET_LABEL.EMERGENCY"
            :budget="pocket.targetAmount"
            @adjust="openBudgetReadjust"
          />

          <PocketStatusCard
            v-else
            :pocket-type="pocket.pocketType"
            :title="POCKET_LABEL[pocket.pocketType]"
            :budget="pocket.targetAmount"
            :used="pocket.usedAmount"
            :remaining="pocket.remainingAmount"
            :usage-rate="pocket.usageRate"
            :over-amount="pocket.overAmount"
            @select="openPocketDetail"
          />
        </template>
      </section>
    </template>

    <!-- 일반 예상 밖 수입 -->
    <UnexpectedIncomeModal
      v-if="currentIncome"
      :open="showUnexpectedIncomeModal"
      :detected-amount="currentIncome.amount"
      :sender-name="currentIncome.senderName || '알 수 없는 입금자'"
      @add="handleUnexpectedAdd"
      @exclude="handleExclude"
      @close="showUnexpectedIncomeModal = false"
    />

    <!-- 고정수입 추정 / 초과 -->
    <RecurringIncomeMatchModal
      v-if="currentIncome"
      :open="showRecurringIncomeModal"
      :income-type="currentIncome.type"
      :sender-name="currentIncome.senderName || '알 수 없는 입금자'"
      :amount="currentIncome.amount"
      :recurring-income-name="currentIncome.recurringIncomeName"
      :expected-amount="currentIncome.expectedAmount"
      :deposit-timing="currentIncome.depositTiming"
      :excess-amount="currentIncome.excessAmount"
      @exclude="handleExclude"
      @not-recurring="handleNotRecurring"
      @add-excess="handleAddExcess"
    />
  </main>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: calc(100dvh - 120px);

  padding: 4px 16px 28px;

  background: var(--c-bg);
}

h1 {
  font-size: 24px;
  line-height: 1.3;

  font-weight: 700;

  letter-spacing: -0.5px;
}

.state {
  display: grid;

  place-items: center;

  min-height: 280px;

  color: var(--c-text-3);

  text-align: center;
}

.state.error {
  gap: 10px;

  color: var(--c-danger);
}

.error button {
  padding: 8px 16px;

  border: 1px solid var(--c-border);

  border-radius: 8px;

  color: var(--c-text);

  background: transparent;

  cursor: pointer;
}

.total {
  display: flex;

  align-items: center;
  justify-content: space-between;

  gap: 16px;

  margin-top: 28px;

  padding: 18px 16px;

  border: 1px solid var(--c-border);

  border-radius: 12px;

  font-size: 13px;
}

.total strong {
  flex: none;

  font-family: var(--font-num);

  font-size: 20px;

  white-space: nowrap;
}

.list {
  display: grid;

  gap: 24px;

  margin-top: 18px;
}

@media (max-width: 340px) {
  .page {
    padding-inline: 12px;
  }

  .total strong {
    font-size: 17px;
  }
}
</style>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import PocketStatusCard from '@/components/pocket/PocketStatusCard.vue'
import FutureAssetPocketCard from '@/components/pocket/FutureAssetPocketCard.vue'
import EmergencyPocketCard from '@/components/pocket/EmergencyPocketCard.vue'
import MydataRefreshStatus from '@/components/pocket/MydataRefreshStatus.vue'

import UnexpectedIncomeModal from '@/components/pocket/UnexpectedIncomeModal.vue'
import RecurringIncomeMatchModal from '@/components/pocket/RecurringIncomeMatchModal.vue'
import PendingIncomeListModal from '@/components/pocket/PendingIncomeListModal.vue'

import { POCKET_LABEL, POCKET_ORDER, pocketApi, type PocketMonthlyResponse } from '@/api/pocket'

import { assetForecastApi, type AssetForecastResponse } from '@/api/assetForecast'

import { unexpectedIncomeApi } from '@/api/unexpectedIncome'

import { ApiError } from '@/api/types'
import { formatWon } from '@/utils/format'

import { useUnexpectedIncomeStore } from '@/stores/unexpectedIncome'
import { useMydataRefresh } from '@/composables/useMydataRefresh'

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
      /*
       * 미래자산 부가 정보 조회 실패가
       * 포켓 전체 조회를 막지 않게 한다.
       */
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

        query: {
          month: currentMonth.value,
        },
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

/*
 * 현재 queue에서 처리 중인 입금.
 */
const currentIncome = computed(() => {
  return unexpectedIncomeStore.currentIncome
})

/*
 * 여러 건 발견 시 처음 보여줄 목록 모달.
 */
const showPendingIncomeListModal = ref(false)

/*
 * 일반 예상 밖 입금 모달.
 */
const showUnexpectedIncomeModal = ref(false)

/*
 * 정기수입 추정 / 초과 모달.
 */
const showRecurringIncomeModal = ref(false)

const checkingIncome = ref(false)

const closeIncomeModals = () => {
  showPendingIncomeListModal.value = false

  showUnexpectedIncomeModal.value = false

  showRecurringIncomeModal.value = false
}

/*
 * 현재 queue의 입금을 실제 처리 대상으로 세팅하고
 * 타입에 맞는 모달을 연다.
 */
const openCurrentIncomeModal = () => {
  closeIncomeModals()

  const income = currentIncome.value

  if (!income) {
    unexpectedIncomeStore.resetDetectedIncome()

    return
  }

  unexpectedIncomeStore.setDetectedIncome(income)

  switch (income.type) {
    case 'NEW_INCOME':
      showUnexpectedIncomeModal.value = true
      break

    case 'RECURRING_LIKELY':
    case 'RECURRING_OVER':
      showRecurringIncomeModal.value = true
      break
  }
}

/*
 * 포켓 진입 시 신규 입금 조회.
 *
 * 이미 store에 처리 중인 queue가 남아있다면
 * 서버를 다시 조회하지 않고 다음 거래부터 이어간다.
 */
const checkNewIncome = async () => {
  if (checkingIncome.value) {
    return
  }

  /*
   * 포켓 추가 화면에서 한 건을 처리하고
   * 다시 PocketView로 돌아온 경우.
   *
   * 기존 queue가 남아있으므로
   * 서버를 다시 조회하지 않는다.
   */
  if (unexpectedIncomeStore.hasPendingIncomes) {
    openCurrentIncomeModal()

    return
  }

  checkingIncome.value = true

  closeIncomeModals()

  try {
    const pending = await unexpectedIncomeApi.findPending()

    const incomes = pending?.incomes ?? []

    unexpectedIncomeStore.setPendingIncomes(incomes)

    if (incomes.length === 0) {
      unexpectedIncomeStore.resetDetectedIncome()

      return
    }

    /*
     * 2건 이상이면
     * 먼저 "새로 들어온 돈 N건" 모달 표시.
     */
    if (incomes.length >= 2) {
      showPendingIncomeListModal.value = true

      return
    }

    /*
     * 1건뿐이면 기존처럼
     * 바로 개별 모달을 보여준다.
     */
    openCurrentIncomeModal()
  } catch (e) {
    /*
     * 새 입금 조회 실패가
     * 포켓 화면 전체를 막지는 않게 한다.
     */
    console.error('새 입금 조회 실패', e)
  } finally {
    checkingIncome.value = false
  }
}

/*
 * 여러 건 목록 모달에서
 * "하나씩 확인하기" 클릭.
 */
const handleStartPendingIncome = () => {
  openCurrentIncomeModal()
}

/*
 * 현재 거래를 처리 완료한 뒤
 * queue에서 제거하고 다음 거래를 연다.
 */
const moveToNextIncome = () => {
  closeIncomeModals()

  /*
   * 방금 처리 완료한 거래 제거.
   */
  unexpectedIncomeStore.removeCurrentIncome()

  /*
   * 현재 거래용 입력값만 초기화.
   * pending queue 자체는 유지한다.
   */
  unexpectedIncomeStore.resetDetectedIncome()

  /*
   * 더 이상 처리할 거래가 없으면 종료.
   */
  if (!unexpectedIncomeStore.hasPendingIncomes) {
    return
  }

  /*
   * 다음 거래 모달 표시.
   */
  openCurrentIncomeModal()
}

/*
 * 일반 예상 밖 수입
 * → 이번 달 예산에 추가하기.
 */
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

/*
 * 이번 달 예산에 포함하지 않기.
 */
const handleExclude = async () => {
  if (!currentIncome.value) {
    return
  }

  try {
    await unexpectedIncomeApi.exclude(currentIncome.value.transactionId)

    /*
     * 서버 재조회 X.
     * 현재 queue에서 바로 다음 거래로 이동.
     */
    moveToNextIncome()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '입금을 처리하지 못했습니다.'
  }
}

/*
 * 정기수입 추정 모달에서
 * "고정수입이 아니에요" 선택.
 */
const handleNotRecurring = () => {
  if (!currentIncome.value) {
    return
  }

  /*
   * 현재 처리용 store 데이터 변경.
   */
  unexpectedIncomeStore.convertToNewIncome()

  /*
   * queue 속 현재 거래도
   * NEW_INCOME으로 바꿔준다.
   */
  currentIncome.value.type = 'NEW_INCOME'

  currentIncome.value.recurringIncomeId = null

  currentIncome.value.recurringIncomeName = null

  currentIncome.value.expectedAmount = null

  currentIncome.value.depositTiming = null

  currentIncome.value.excessAmount = 0

  showRecurringIncomeModal.value = false

  showUnexpectedIncomeModal.value = true
}

/*
 * 고정수입보다 초과 입금된 금액만
 * 포켓 예산에 추가.
 */
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

const { lastSyncedAt, refreshing, refreshError, loadLastSyncedAt, refresh } = useMydataRefresh(load)

const initialize = async () => {
  const synced = await refresh()

  if (!synced) {
    await Promise.all([loadLastSyncedAt(), load()])
  }
}

onMounted(initialize)

watch(currentMonth, load)
</script>

<template>
  <main class="page">
    <div v-if="response" class="page-heading">
      <h1>{{ monthNumber }}월 포켓</h1>

      <MydataRefreshStatus
        :last-synced-at="lastSyncedAt"
        :refreshing="refreshing"
        :error="refreshError"
        @refresh="refresh"
      />
    </div>

    <p v-if="loading" class="state" role="status">포켓 현황을 불러오는 중…</p>

    <div v-else-if="error" class="state error" role="alert">
      <p>
        {{ error }}
      </p>

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

    <!--
      =========================
      신규 입금 여러 건 목록
      =========================
    -->
    <PendingIncomeListModal
      :open="showPendingIncomeListModal"
      :incomes="unexpectedIncomeStore.pendingIncomes"
      :total-amount="unexpectedIncomeStore.totalPendingAmount"
      @confirm="handleStartPendingIncome"
    />

    <!--
      =========================
      일반 예상 밖 수입
      =========================
    -->
    <UnexpectedIncomeModal
      v-if="currentIncome"
      :open="showUnexpectedIncomeModal"
      :detected-amount="currentIncome.amount"
      :sender-name="currentIncome.senderName || '알 수 없는 입금자'"
      @add="handleUnexpectedAdd"
      @exclude="handleExclude"
      @close="showUnexpectedIncomeModal = false"
    />

    <!--
      =========================
      고정수입 추정 / 초과
      =========================
    -->
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

.page-heading {
  display: flex;

  align-items: flex-start;

  justify-content: space-between;

  gap: 12px;
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

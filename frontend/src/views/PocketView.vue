<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { api } from '@/api/client'
import { unexpectedIncomeApi, type UnexpectedIncome } from '@/api/unexpectedIncome'
import { ApiError } from '@/api/types'

import UnexpectedIncomeModal from '@/components/pocket/UnexpectedIncomeModal.vue'
import RecurringIncomeMatchModal from '@/components/pocket/RecurringIncomeMatchModal.vue'

import { useUnexpectedIncomeStore } from '@/stores/unexpectedIncome'

type PocketType = 'ESSENTIAL' | 'FREE' | 'EMERGENCY' | 'FUTURE_ASSET'

interface PocketMonthlyItem {
  pocketId: number
  pocketType: PocketType
  pocketName: string
  targetAmount: number
  usedAmount: number | null
  remainingAmount: number | null
  usageRate: number | null
  overAmount: number | null
}

interface PocketMonthlyResponse {
  month: string
  totalBudgetAmount: number
  pockets: PocketMonthlyItem[]
}

const router = useRouter()

const unexpectedIncomeStore = useUnexpectedIncomeStore()

const pockets = ref<PocketMonthlyItem[]>([])

const loading = ref(true)

const error = ref<string | null>(null)

const currentIncome = ref<UnexpectedIncome | null>(null)

const showUnexpectedIncomeModal = ref(false)

const showRecurringIncomeModal = ref(false)

const checkingIncome = ref(false)

const currentMonth = computed(() => {
  const now = new Date()

  const year = now.getFullYear()

  const month = String(now.getMonth() + 1).padStart(2, '0')

  return `${year}-${month}`
})

const won = (value: number | null | undefined) => {
  return `${Math.round(Number(value ?? 0)).toLocaleString('ko-KR')}원`
}

const getUsageRate = (pocket: PocketMonthlyItem) => {
  return Number(pocket.usageRate ?? 0)
}

const getUsedAmount = (pocket: PocketMonthlyItem) => {
  return Number(pocket.usedAmount ?? 0)
}

const getRemainingAmount = (pocket: PocketMonthlyItem) => {
  return Number(pocket.remainingAmount ?? 0)
}

const getOverAmount = (pocket: PocketMonthlyItem) => {
  return Number(pocket.overAmount ?? 0)
}

const isOver = (pocket: PocketMonthlyItem) => {
  return getOverAmount(pocket) > 0
}

async function loadPockets() {
  loading.value = true
  error.value = null

  try {
    const response = await api.get<PocketMonthlyResponse>(
      `/api/pockets/monthly?month=${currentMonth.value}`,
    )

    pockets.value = response.pockets ?? []
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '포켓 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function closeIncomeModals() {
  showUnexpectedIncomeModal.value = false
  showRecurringIncomeModal.value = false
}

async function checkNewIncome() {
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
    error.value = e instanceof ApiError ? e.message : '새 입금을 확인하지 못했습니다.'
  } finally {
    checkingIncome.value = false
  }
}

async function checkNextIncome() {
  closeIncomeModals()

  currentIncome.value = null

  unexpectedIncomeStore.resetAll()

  queueMicrotask(() => {
    checkNewIncome()
  })
}

async function handleUnexpectedAdd() {
  if (!currentIncome.value) {
    return
  }

  unexpectedIncomeStore.setDetectedIncome(currentIncome.value)

  showUnexpectedIncomeModal.value = false

  await router.push({
    name: 'pocket-unexpected-income-amount',
  })
}

async function handleExclude() {
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

function handleNotRecurring() {
  if (!currentIncome.value) {
    return
  }

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

async function handleAddExcess() {
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

async function load() {
  await loadPockets()

  await checkNewIncome()
}

onMounted(load)
</script>

<template>
  <main class="page">
    <header class="head">
      <h1>내 포켓</h1>

      <p>정착금과 지원금을 용도별로 나눠 관리합니다.</p>
    </header>

    <p v-if="loading" class="state">불러오는 중…</p>

    <div v-else-if="error" class="state error">
      <p>
        {{ error }}
      </p>

      <button type="button" @click="load">다시 시도</button>
    </div>

    <ul v-else class="list">
      <li v-for="pocket in pockets" :key="pocket.pocketId" class="card">
        <div class="card-head">
          <div>
            <h2>
              {{ pocket.pocketName }}
            </h2>

            <p class="desc">
              <template v-if="pocket.pocketType === 'ESSENTIAL'">
                주거비, 공과금, 통신비 등 꼭 필요한 생활비
              </template>

              <template v-else-if="pocket.pocketType === 'FREE'">
                식비, 교통, 여가 등 자유롭게 사용하는 생활비
              </template>

              <template v-else-if="pocket.pocketType === 'EMERGENCY'">
                예상하지 못한 상황에 대비하는 비상금
              </template>

              <template v-else> 적금과 투자 등 미래를 위한 자산 </template>
            </p>
          </div>

          <span
            class="rate"
            :class="{
              over: isOver(pocket),
            }"
          >
            {{ getUsageRate(pocket) }}%
          </span>
        </div>

        <div class="bar">
          <div
            class="fill"
            :class="{
              over: isOver(pocket),
            }"
            :style="{
              width: `${Math.min(getUsageRate(pocket), 100)}%`,
            }"
          />
        </div>

        <dl class="figures">
          <div>
            <dt>배분액</dt>

            <dd>
              {{ won(pocket.targetAmount) }}
            </dd>
          </div>

          <div>
            <dt>사용</dt>

            <dd>
              {{ won(getUsedAmount(pocket)) }}
            </dd>
          </div>

          <div>
            <dt>
              {{ isOver(pocket) ? '초과' : '남음' }}
            </dt>

            <dd
              :class="{
                over: isOver(pocket),
              }"
            >
              {{ isOver(pocket) ? won(getOverAmount(pocket)) : won(getRemainingAmount(pocket)) }}
            </dd>
          </div>
        </dl>
      </li>
    </ul>

    <UnexpectedIncomeModal
      v-if="currentIncome"
      :open="showUnexpectedIncomeModal"
      :detected-amount="currentIncome.amount"
      :sender-name="currentIncome.senderName || '알 수 없는 입금자'"
      @add="handleUnexpectedAdd"
      @exclude="handleExclude"
      @close="showUnexpectedIncomeModal = false"
    />

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
  max-width: 640px;

  margin: 0 auto;

  padding: 2rem 1rem 4rem;
}

.head h1 {
  margin: 0;

  font-size: 1.5rem;
  font-weight: 700;
}

.head p {
  margin-top: 0.25rem;

  color: var(--color-text-light, #888);

  font-size: 0.9rem;
}

.state {
  margin-top: 2rem;

  text-align: center;

  color: var(--color-text-light, #888);
}

.state.error {
  color: #c0392b;
}

.state button {
  margin-top: 0.75rem;

  padding: 0.4rem 1rem;

  cursor: pointer;
}

.list {
  display: grid;

  gap: 0.75rem;

  margin: 1.5rem 0 0;
  padding: 0;

  list-style: none;
}

.card {
  padding: 1rem;

  border: 1px solid var(--color-border, #e0e0e0);

  border-radius: 12px;
}

.card-head {
  display: flex;

  align-items: flex-start;
  justify-content: space-between;

  gap: 1rem;
}

.card-head h2 {
  margin: 0;

  font-size: 1.05rem;
  font-weight: 600;
}

.desc {
  margin-top: 0.15rem;

  color: var(--color-text-light, #888);

  font-size: 0.8rem;
}

.rate {
  white-space: nowrap;

  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.rate.over {
  color: #c0392b;
}

.bar {
  height: 6px;

  margin-top: 0.75rem;

  overflow: hidden;

  border-radius: 3px;

  background: var(--color-background-mute, #f1f1f1);
}

.fill {
  height: 100%;

  background: #00857a;

  transition: width 0.3s;
}

.fill.over {
  background: #c0392b;
}

.figures {
  display: flex;

  justify-content: space-between;

  margin: 0.75rem 0 0;
}

.figures div {
  flex: 1;

  text-align: center;
}

.figures dt {
  color: var(--color-text-light, #888);

  font-size: 0.75rem;
}

.figures dd {
  margin: 0.15rem 0 0;

  font-size: 0.9rem;
  font-weight: 600;

  font-variant-numeric: tabular-nums;
}

.figures dd.over {
  color: #c0392b;
}
</style>

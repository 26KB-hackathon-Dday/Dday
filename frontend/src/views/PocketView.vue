<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { api } from '@/api/client'
import { unexpectedIncomeApi, type UnexpectedIncome } from '@/api/unexpectedIncome'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import PocketStatusCard from '@/components/pocket/PocketStatusCard.vue'
import FutureAssetPocketCard from '@/components/pocket/FutureAssetPocketCard.vue'
import EmergencyPocketCard from '@/components/pocket/EmergencyPocketCard.vue'
import { POCKET_LABEL, POCKET_ORDER, pocketApi, type PocketMonthlyResponse } from '@/api/pocket'
import { ApiError } from '@/api/types'
import { formatWon } from '@/utils/format'

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

const route = useRoute()
const response = ref<PocketMonthlyResponse | null>(null)
const loading = ref(true)

const error = ref<string | null>(null)

const currentIncome = ref<UnexpectedIncome | null>(null)
const errorCode = ref<string | null>(null)
const needsBudgetConfirmation = computed(() =>
  ['MONTHLY_BUDGET_NOT_FOUND', 'MONTHLY_POCKET_BUDGET_NOT_FOUND'].includes(errorCode.value ?? ''),
)
const currentMonth = computed(() => {
  const queryMonth = typeof route.query.month === 'string' ? route.query.month : ''
  if (/^\d{4}-(0[1-9]|1[0-2])$/.test(queryMonth)) return queryMonth
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
})
const monthNumber = computed(() => Number(currentMonth.value.slice(5)))
const nextMonthNumber = computed(() => (monthNumber.value % 12) + 1)
const orderedPockets = computed(() =>
  [...(response.value?.pockets ?? [])].sort(
    (a, b) => POCKET_ORDER.indexOf(a.pocketType) - POCKET_ORDER.indexOf(b.pocketType),
  ),
)

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
  errorCode.value = null
  try {
    response.value = await pocketApi.findMonthly(currentMonth.value)
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '알 수 없는 오류가 발생했습니다.'
    errorCode.value = e instanceof ApiError ? e.code : null
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

function openNextBudget() {
  router.push({ name: 'pocket-budget-initial' })
}
function openPocketDetail(pocketType: 'ESSENTIAL' | 'FREE') {
  if (!['ESSENTIAL', 'FREE'].includes(pocketType)) return
  router.push({
    name: 'pocket-detail',
    params: { pocketType },
    query: { month: currentMonth.value },
  })
}
function openBudgetReadjust() {
  router.push({ name: 'pocket-budget-readjust' })
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
    <h1>{{ monthNumber }}월 포켓</h1>
    <button type="button" class="notice" @click="openNextBudget">
      <span>{{ nextMonthNumber }}월 최적화 포켓 예산이 만들어졌어요!</span
      ><AppIcon name="chevron-right" :size="18" aria-hidden="true" />
    </button>
    <p v-if="loading" class="state" role="status">포켓 현황을 불러오는 중…</p>
    <div v-else-if="error" class="state error" role="alert">
      <p>{{ error }}</p>
      <button
        v-if="needsBudgetConfirmation"
        type="button"
        class="budget-button"
        @click="openNextBudget"
      >
        예산 확정하러 가기
      </button>
      <button v-else type="button" @click="load">다시 시도</button>
    </div>
    <template v-else-if="response">
      <section class="total" aria-label="이번 달 총 예산">
        <span>{{ monthNumber }}월 총 예산</span
        ><strong>{{ formatWon(response.totalBudgetAmount) }}</strong>
      </section>
      <section class="list" aria-label="포켓별 현황">
        <template v-for="pocket in orderedPockets" :key="pocket.pocketId">
          <FutureAssetPocketCard
            v-if="pocket.pocketType === 'FUTURE_ASSET'"
            :title="POCKET_LABEL.FUTURE_ASSET"
            :budget="pocket.targetAmount"
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
  width: 100%;
  padding: 4px 16px 28px;
  background: var(--c-bg);
}
h1 {
  font-size: 24px;
  line-height: 1.3;
  font-weight: 700;
  letter-spacing: -0.5px;
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
.notice {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  padding: 12px 14px;
  border-radius: 9px;
  background: var(--c-surface);
  color: var(--c-text-2);
  text-align: left;
  font-size: 12px;
}
.notice:hover {
  filter: brightness(0.98);
}
.notice .app-icon {
  color: #b9b9b9;
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
}
.error .budget-button {
  border-color: var(--c-primary);
  background: var(--c-primary);
  color: var(--c-bg);
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

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import PocketStatusCard from '@/components/pocket/PocketStatusCard.vue'
import FutureAssetPocketCard from '@/components/pocket/FutureAssetPocketCard.vue'
import { POCKET_LABEL, POCKET_ORDER, pocketApi, type PocketMonthlyResponse } from '@/api/pocket'
import { ApiError } from '@/api/types'
import { formatWon } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const response = ref<PocketMonthlyResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
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

async function load() {
  loading.value = true
  error.value = null
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
function openNextBudget() {
  router.push({ name: 'pocket-budget-initial' })
}
function openPocketDetail(pocketType: 'ESSENTIAL' | 'FREE' | 'EMERGENCY') {
  if (pocketType !== 'ESSENTIAL' && pocketType !== 'FREE') return
  router.push({
    name: 'pocket-detail',
    params: { pocketType },
    query: { month: currentMonth.value },
  })
}
onMounted(load)
</script>

<template>
  <main class="page">
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

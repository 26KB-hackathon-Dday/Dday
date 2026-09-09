<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PocketBudgetSummary from '@/components/pocket/PocketBudgetSummary.vue'
import PocketCategoryUsageItem from '@/components/pocket/PocketCategoryUsageItem.vue'
import PocketSpendingDonut, {
  type SpendingCategory,
} from '@/components/pocket/PocketSpendingDonut.vue'
import PocketTransactionItem from '@/components/pocket/PocketTransactionItem.vue'
import {
  POCKET_LABEL,
  pocketApi,
  type PageResponse,
  type PocketCategoryUsageItem as PocketCategoryUsageData,
  type PocketCategoryUsageResponse,
  type PocketMonthlySummary,
  type PocketTransaction,
  type PocketType,
} from '@/api/pocket'
import { ApiError } from '@/api/types'
import { formatWon } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const summary = ref<PocketMonthlySummary | null>(null)
const transactions = ref<PocketTransaction[]>([])
const categoryUsage = ref<PocketCategoryUsageResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const supportedTypes: PocketType[] = ['ESSENTIAL', 'FREE', 'EMERGENCY']
const pocketType = computed<PocketType | null>(() => {
  const value = typeof route.params.pocketType === 'string' ? route.params.pocketType : ''
  return supportedTypes.includes(value as PocketType) ? (value as PocketType) : null
})
const isFree = computed(() => pocketType.value === 'FREE')
const isEmergency = computed(() => pocketType.value === 'EMERGENCY')
const summaryTheme = computed(() =>
  isEmergency.value ? ('teal' as const) : isFree.value ? ('orange' as const) : ('blue' as const),
)
const currentMonth = computed(() => {
  const queryMonth = typeof route.query.month === 'string' ? route.query.month : ''
  if (/^\d{4}-(0[1-9]|1[0-2])$/.test(queryMonth)) return queryMonth
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
})
const expenses = computed(() =>
  transactions.value.filter(
    (item) => item.transactionType === 'EXPENSE' && item.transactionStatus === 'NORMAL',
  ),
)
const visibleTransactions = computed(() => expenses.value.slice(0, 3))
const spendingCategories = computed<SpendingCategory[]>(() => {
  const totals = new Map<string, number>()
  expenses.value.forEach((item) => {
    const name = item.category?.categoryName ?? '미분류'
    totals.set(name, (totals.get(name) ?? 0) + item.amount)
  })
  return [...totals].map(([name, amount]) => ({ name, amount }))
})
const essentialCategories = computed<PocketCategoryUsageData[]>(() => {
  const response = categoryUsage.value
  if (!response) return []
  const categories = [...response.categories]
  if (response.unclassifiedUsedAmount > 0) {
    categories.push({
      categoryId: -1,
      categoryCode: 'UNCLASSIFIED',
      categoryName: '미분류',
      usedAmount: response.unclassifiedUsedAmount,
    })
  }
  return categories
})

async function findAllTransactions(type: PocketType) {
  const first = await pocketApi.findTransactions(type, currentMonth.value, 0, 100)
  if (first.totalPages <= 1) return first.content
  const remaining = await Promise.all(
    Array.from({ length: first.totalPages - 1 }, (_, index) =>
      pocketApi.findTransactions(type, currentMonth.value, index + 1, 100),
    ),
  )
  return [first, ...remaining].flatMap((page: PageResponse<PocketTransaction>) => page.content)
}

async function load() {
  if (!pocketType.value) {
    await router.replace({ name: 'pockets', query: { month: currentMonth.value } })
    return
  }
  route.meta.title = POCKET_LABEL[pocketType.value]
  loading.value = true
  error.value = null
  try {
    const selectedType = pocketType.value
    // 필수·자유·비상금은 같은 포켓 거래 API를 사용한다.
    const [monthly, allTransactions, actualCategoryUsage] = await Promise.all([
      pocketApi.findMonthly(currentMonth.value),
      findAllTransactions(selectedType),
      selectedType === 'ESSENTIAL'
        ? pocketApi.findCategoryUsage(selectedType, currentMonth.value)
        : Promise.resolve(null),
    ])
    summary.value = monthly.pockets.find((pocket) => pocket.pocketType === selectedType) ?? null
    if (!summary.value) throw new Error('POCKET_NOT_FOUND')
    transactions.value = allTransactions
    categoryUsage.value = actualCategoryUsage
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '포켓 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openBudgetReadjust() {
  // 기존 재조정 화면은 month query를 계약으로 사용하지 않으므로 라우트 이름만 전달한다.
  router.push({ name: 'pocket-budget-readjust' })
}
</script>

<template>
  <main class="detail-page">
    <p v-if="loading" class="state" role="status">포켓 정보를 불러오는 중…</p>
    <div v-else-if="error" class="state state--error" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="load">다시 시도</button>
    </div>
    <template v-else-if="summary">
      <PocketBudgetSummary
        :summary="summary"
        :theme="summaryTheme"
        :pill-label="isEmergency ? '이번 달 남은 비상금' : '이번 달 남은 예산'"
      />
      <PocketSpendingDonut v-if="isFree" class="section" :categories="spendingCategories" />
      <section v-else-if="!isEmergency" class="section">
        <header class="section__header">
          <h2>카테고리별 사용 현황</h2>
          <small v-if="categoryUsage">총 {{ formatWon(categoryUsage.totalUsedAmount) }}</small>
        </header>
        <ul v-if="essentialCategories.length" class="category-card">
          <PocketCategoryUsageItem
            v-for="category in essentialCategories"
            :key="category.categoryId"
            :name="category.categoryName"
            :category-code="category.categoryCode"
            :used-amount="category.usedAmount"
          />
        </ul>
        <p v-else class="empty">표시할 카테고리가 없습니다.</p>
      </section>
      <section class="section section--transactions">
        <header class="section__header">
          <h2>최근 지출 내역</h2>
          <button type="button" disabled title="거래내역 전체 화면 준비 중">전체보기</button>
        </header>
        <ul v-if="visibleTransactions.length" class="transaction-list">
          <PocketTransactionItem
            v-for="transaction in visibleTransactions"
            :key="transaction.transactionId"
            :transaction="transaction"
          />
        </ul>
        <p v-else class="empty">이번 달 지출 내역이 없습니다.</p>
      </section>
      <section v-if="isEmergency" class="adjustment">
        <h2>이번 달 예산을 바꾸고 싶나요?</h2>
        <p>남아 있는 예산 안에서 포켓별 금액을 다시 조정할 수 있어요.</p>
        <button type="button" @click="openBudgetReadjust">
          포켓 예산 조정하기 <span aria-hidden="true">→</span>
        </button>
      </section>
    </template>
  </main>
</template>

<style scoped>
.detail-page {
  width: 100%;
  padding: 18px 22px 32px;
  overflow-x: hidden;
  background: var(--c-bg);
}
.state {
  display: grid;
  min-height: 360px;
  place-items: center;
  color: var(--c-text-3);
  text-align: center;
}
.state--error {
  align-content: center;
  gap: 12px;
  color: var(--c-danger);
}
.state button {
  padding: 8px 16px;
  border: 1px solid var(--c-border);
  border-radius: 8px;
  color: var(--c-text);
}
.section {
  margin-top: 34px;
}
.section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.section__header h2 {
  font-size: 17px;
  font-weight: 700;
}
.section__header small {
  color: var(--c-text-3);
  font-size: 10px;
}
.section__header button {
  color: var(--c-text-3);
  font-size: 12px;
}
.section__header button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}
.category-card {
  overflow: hidden;
  border: 1px solid var(--c-border);
  border-radius: 13px;
  background: var(--c-bg);
  list-style: none;
}
.transaction-list {
  list-style: none;
}
.empty {
  padding: 28px 0;
  color: var(--c-text-3);
  text-align: center;
  font-size: 12px;
}
.adjustment {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid var(--c-border);
}
.adjustment h2 {
  font-size: 16px;
  font-weight: 700;
}
.adjustment p {
  margin-top: 5px;
  color: var(--c-text-2);
  font-size: 12px;
}
.adjustment button {
  margin-top: 14px;
  color: var(--c-teal);
  font-size: 13px;
  font-weight: 600;
}
@media (max-width: 340px) {
  .detail-page {
    padding-inline: 14px;
  }
}
</style>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BottomSheet from '@/components/BottomSheet.vue'
import PocketBudgetSummary from '@/components/pocket/PocketBudgetSummary.vue'
import PocketCategoryUsageItem from '@/components/pocket/PocketCategoryUsageItem.vue'
import PocketSpendingDonut, {
  type SpendingCategory,
} from '@/components/pocket/PocketSpendingDonut.vue'
import PocketTransactionItem from '@/components/pocket/PocketTransactionItem.vue'
import PocketTransactionDetailSheet from '@/components/pocket/PocketTransactionDetailSheet.vue'
import {
  POCKET_LABEL,
  pocketApi,
  type PageResponse,
  type PocketCategoryUsageItem as PocketCategoryUsageData,
  type PocketCategoryUsageResponse,
  type PocketCategory,
  type PocketMonthlySummary,
  type PocketTransaction,
  type PocketTransactionDetail,
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
const showAllTransactions = ref(false)
const selectedUsageCategory = ref<PocketCategoryUsageData | null>(null)
const categoryTransactionSheetOpen = ref(false)
const transactionSheetOpen = ref(false)
const selectedTransactionId = ref<number | null>(null)
const selectedTransaction = ref<PocketTransactionDetail | null>(null)
const selectableCategories = ref<PocketCategory[]>([])
const selectedCategoryId = ref<number | null>(null)
const transactionLoading = ref(false)
const transactionSaving = ref(false)
const transactionError = ref<string | null>(null)
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
const canExpandTransactions = computed(
  () =>
    (pocketType.value === 'ESSENTIAL' || pocketType.value === 'FREE') && expenses.value.length > 3,
)
const visibleTransactions = computed(() =>
  showAllTransactions.value && canExpandTransactions.value
    ? expenses.value
    : expenses.value.slice(0, 3),
)
const CATEGORY_CODE_BY_NAME: Record<string, string> = {
  식비: 'FOOD',
  쇼핑: 'SHOPPING',
  의료: 'MEDICAL',
  '의료·건강': 'MEDICAL',
  취미: 'CULTURE',
  '문화·여가': 'CULTURE',
  미용: 'BEAUTY',
  '뷰티·미용': 'BEAUTY',
  여행: 'TRAVEL',
  기타: 'ETC',
  교통: 'TRANSPORT',
  교통비: 'TRANSPORT',
}
const spendingCategories = computed<SpendingCategory[]>(() => {
  const totals = new Map<number, SpendingCategory>()
  expenses.value.forEach((item) => {
    const categoryId = item.category?.categoryId ?? -1
    const name = item.category?.categoryName ?? '미분류'
    const current = totals.get(categoryId)
    totals.set(categoryId, {
      categoryId,
      categoryCode: item.category?.categoryCode ?? CATEGORY_CODE_BY_NAME[name] ?? 'UNCLASSIFIED',
      name,
      amount: (current?.amount ?? 0) + item.amount,
    })
  })
  return [...totals.values()]
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
const selectedCategoryTransactions = computed(() => {
  const selected = selectedUsageCategory.value
  if (!selected) return []
  return expenses.value.filter((transaction) =>
    selected.categoryId === -1
      ? transaction.category == null
      : transaction.category?.categoryId === selected.categoryId,
  )
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
  showAllTransactions.value = false
  selectedUsageCategory.value = null
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

function flattenCategories(categories: PocketCategory[]): PocketCategory[] {
  return categories.flatMap((category) => [category, ...flattenCategories(category.children)])
}

async function loadSelectedTransaction() {
  const transactionId = selectedTransactionId.value
  const type = pocketType.value
  if (!transactionId || (type !== 'ESSENTIAL' && type !== 'FREE')) return

  transactionLoading.value = true
  transactionError.value = null
  try {
    const [detail, categoryResponse] = await Promise.all([
      pocketApi.findTransaction(transactionId),
      pocketApi.findCategories(type),
    ])
    selectedTransaction.value = detail
    selectableCategories.value = flattenCategories(categoryResponse.categories)
    selectedCategoryId.value = detail.category?.categoryId ?? null
  } catch (e) {
    transactionError.value =
      e instanceof ApiError ? e.message : '거래 상세 정보를 불러오지 못했습니다.'
  } finally {
    transactionLoading.value = false
  }
}

async function openTransaction(transactionId: number) {
  if (pocketType.value !== 'ESSENTIAL' && pocketType.value !== 'FREE') return
  categoryTransactionSheetOpen.value = false
  selectedTransactionId.value = transactionId
  selectedTransaction.value = null
  selectableCategories.value = []
  selectedCategoryId.value = null
  transactionSheetOpen.value = true
  await loadSelectedTransaction()
}

async function saveTransactionCategory(categoryId: number) {
  const transactionId = selectedTransactionId.value
  const type = pocketType.value
  if (!transactionId || (type !== 'ESSENTIAL' && type !== 'FREE') || transactionSaving.value) return

  transactionSaving.value = true
  transactionError.value = null
  try {
    await pocketApi.classifyTransaction(transactionId, {
      pocketType: type,
      categoryId,
      applyFutureRule: false,
    })
    await load()
    await loadSelectedTransaction()
  } catch (e) {
    transactionError.value = e instanceof ApiError ? e.message : '카테고리를 저장하지 못했습니다.'
  } finally {
    transactionSaving.value = false
  }
}

onMounted(load)

function openBudgetReadjust() {
  // 기존 재조정 화면은 month query를 계약으로 사용하지 않으므로 라우트 이름만 전달한다.
  router.push({ name: 'pocket-budget-readjust' })
}

function selectUsageCategory(category: PocketCategoryUsageData) {
  selectedUsageCategory.value = category
  categoryTransactionSheetOpen.value = true
}

function selectSpendingCategory(category: SpendingCategory) {
  selectUsageCategory({
    categoryId: category.categoryId,
    categoryCode: category.categoryCode,
    categoryName: category.name,
    usedAmount: category.amount,
  })
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
      <PocketSpendingDonut
        v-if="isFree"
        class="section"
        :categories="spendingCategories"
        @select="selectSpendingCategory"
      />
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
            :selected="selectedUsageCategory?.categoryId === category.categoryId"
            @select="selectUsageCategory(category)"
          />
        </ul>
        <p v-else class="empty">표시할 카테고리가 없습니다.</p>
      </section>
      <section class="section section--transactions">
        <header class="section__header">
          <h2>최근 지출 내역</h2>
          <button
            v-if="canExpandTransactions"
            type="button"
            :aria-expanded="showAllTransactions"
            aria-controls="pocket-transaction-list"
            @click="showAllTransactions = !showAllTransactions"
          >
            {{ showAllTransactions ? '접기' : '전체보기' }}
          </button>
        </header>
        <ul v-if="visibleTransactions.length" id="pocket-transaction-list" class="transaction-list">
          <PocketTransactionItem
            v-for="transaction in visibleTransactions"
            :key="transaction.transactionId"
            :transaction="transaction"
            @select="openTransaction"
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
    <BottomSheet v-model="categoryTransactionSheetOpen">
      <section v-if="selectedUsageCategory" class="category-transactions-sheet">
        <header class="category-transactions-sheet__header">
          <div>
            <h2>{{ selectedUsageCategory.categoryName }} 지출 내역</h2>
            <p>{{ currentMonth.replace('-', '년 ') }}월</p>
          </div>
          <button
            type="button"
            aria-label="카테고리 지출 내역 닫기"
            @click="categoryTransactionSheetOpen = false"
          >
            ×
          </button>
        </header>
        <ul v-if="selectedCategoryTransactions.length" class="transaction-list">
          <PocketTransactionItem
            v-for="transaction in selectedCategoryTransactions"
            :key="transaction.transactionId"
            :transaction="transaction"
            @select="openTransaction"
          />
        </ul>
        <p v-else class="empty">
          이번 달 {{ selectedUsageCategory.categoryName }} 지출 내역이 없습니다.
        </p>
      </section>
    </BottomSheet>
    <PocketTransactionDetailSheet
      v-model="transactionSheetOpen"
      v-model:category-id="selectedCategoryId"
      :detail="selectedTransaction"
      :categories="selectableCategories"
      :loading="transactionLoading"
      :saving="transactionSaving"
      :error="transactionError"
      @retry="loadSelectedTransaction"
      @save="saveTransactionCategory"
    />
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
  cursor: pointer;
  color: var(--c-text-3);
  font-size: 12px;
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
.category-transactions-sheet {
  padding: 8px 22px 28px;
}
.category-transactions-sheet__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 4px 0 16px;
  border-bottom: 1px solid var(--c-border);
}
.category-transactions-sheet__header h2 {
  font-size: 18px;
  font-weight: 700;
}
.category-transactions-sheet__header p {
  margin-top: 3px;
  color: var(--c-text-3);
  font-size: 11px;
}
.category-transactions-sheet__header button {
  display: grid;
  flex: none;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 50%;
  background: var(--c-surface);
  color: var(--c-text-2);
  font-size: 22px;
  line-height: 1;
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

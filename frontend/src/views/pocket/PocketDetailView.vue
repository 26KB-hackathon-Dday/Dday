<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PocketCategoryUsageItem from '@/components/pocket/PocketCategoryUsageItem.vue'
import PocketTransactionItem from '@/components/pocket/PocketTransactionItem.vue'
import { pocketApi, type PocketMonthlySummary, type PocketTransaction } from '@/api/pocket'
import { ApiError } from '@/api/types'
import { ESSENTIAL_CATEGORY_USAGE_MOCK } from '@/data/essentialPocketDetailMock'
import { formatWon } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const summary = ref<PocketMonthlySummary | null>(null)
const recentTransactions = ref<PocketTransaction[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const currentMonth = computed(() => {
  const queryMonth = typeof route.query.month === 'string' ? route.query.month : ''
  if (/^\d{4}-(0[1-9]|1[0-2])$/.test(queryMonth)) return queryMonth
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
})
const rate = computed(() => summary.value?.usageRate ?? 0)
const fillWidth = computed(() => `${Math.min(Math.max(rate.value, 0), 100)}%`)

async function load() {
  if (route.params.pocketType !== 'ESSENTIAL') {
    await router.replace({ name: 'pockets', query: { month: currentMonth.value } })
    return
  }
  loading.value = true
  error.value = null
  try {
    const [monthly, transactions] = await Promise.all([
      pocketApi.findMonthly(currentMonth.value),
      pocketApi.findTransactions('ESSENTIAL', currentMonth.value),
    ])
    summary.value = monthly.pockets.find((pocket) => pocket.pocketType === 'ESSENTIAL') ?? null
    if (!summary.value) throw new Error('ESSENTIAL_POCKET_NOT_FOUND')
    recentTransactions.value = transactions.content
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '필수 포켓 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="detail-page">
    <p v-if="loading" class="state" role="status">필수 포켓을 불러오는 중…</p>
    <div v-else-if="error" class="state state--error" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="load">다시 시도</button>
    </div>
    <template v-else-if="summary">
      <section class="summary" aria-labelledby="remaining-title">
        <p id="remaining-title" class="summary__pill">이번 달 남은 예산</p>
        <strong class="summary__remaining">{{ formatWon(summary.remainingAmount ?? 0) }}</strong>
        <dl class="summary__amounts">
          <div>
            <dt>사용</dt>
            <dd>{{ formatWon(summary.usedAmount ?? 0) }}</dd>
          </div>
          <div>
            <dt>총 예산</dt>
            <dd>{{ formatWon(summary.targetAmount) }}</dd>
          </div>
        </dl>
        <div class="summary__progress-row">
          <div
            class="summary__progress"
            role="progressbar"
            aria-label="필수 포켓 사용률"
            :aria-valuenow="Math.min(Math.max(rate, 0), 100)"
            aria-valuemin="0"
            aria-valuemax="100"
          >
            <span :style="{ width: fillWidth }" />
          </div>
          <small>{{ rate }}% 사용</small>
        </div>
      </section>

      <section class="section">
        <header class="section__header">
          <h2>카테고리별 사용 현황</h2>
          <small>데이터 연동 예정</small>
        </header>
        <ul class="category-card" aria-label="화면 확인용 카테고리 사용 현황 예시">
          <PocketCategoryUsageItem
            v-for="category in ESSENTIAL_CATEGORY_USAGE_MOCK"
            :key="category.id"
            v-bind="category"
          />
        </ul>
      </section>

      <section class="section section--transactions">
        <header class="section__header">
          <h2>최근 지출 내역</h2>
          <button type="button" disabled title="거래내역 전체 화면 준비 중">전체보기</button>
        </header>
        <ul v-if="recentTransactions.length" class="transaction-list">
          <PocketTransactionItem
            v-for="transaction in recentTransactions"
            :key="transaction.transactionId"
            :transaction="transaction"
          />
        </ul>
        <p v-else class="empty">이번 달 지출 내역이 없습니다.</p>
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
.summary {
  padding: 22px 0 8px;
  text-align: center;
}
.summary__pill {
  display: inline-flex;
  padding: 5px 12px;
  border-radius: 999px;
  background: #eef4ff;
  color: var(--c-blue);
  font-size: 12px;
  font-weight: 600;
}
.summary__remaining {
  display: block;
  margin-top: 8px;
  font-family: var(--font-num);
  font-size: 30px;
  line-height: 1.25;
  letter-spacing: -0.5px;
}
.summary__amounts {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 26px;
  color: var(--c-text-2);
  font-size: 12px;
}
.summary__amounts div {
  display: flex;
  gap: 5px;
}
.summary__amounts dd {
  font-family: var(--font-num);
  font-weight: 600;
}
.summary__progress-row {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 9px;
}
.summary__progress {
  flex: 1;
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: #dedede;
}
.summary__progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--c-blue);
}
.summary__progress-row small {
  flex: none;
  color: var(--c-text-2);
  font-size: 10px;
  white-space: nowrap;
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
@media (max-width: 340px) {
  .detail-page {
    padding-inline: 14px;
  }
  .summary__remaining {
    font-size: 27px;
  }
  .summary__amounts {
    gap: 8px;
  }
}
</style>

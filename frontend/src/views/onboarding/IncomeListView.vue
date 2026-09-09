<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import { ApiError } from '@/api/types'
import { INCOME_TYPE_LABEL } from '@/api/onboarding'
import { useOnboardingStore } from '@/stores/onboarding'
import StepProgress from '@/components/signup/StepProgress.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const route = useRoute()
const router = useRouter()
const onboarding = useOnboardingStore()

const loading = ref(true)
const errorMessage = ref('')

onMounted(async () => {
  try {
    await onboarding.fetchIncomes()
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '알 수 없는 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
})

async function remove(incomeId: number) {
  try {
    await showConfirmDialog({ title: '정기수입 삭제', message: '이 항목을 삭제할까요?' })
  } catch {
    return
  }
  try {
    await onboarding.removeIncome(incomeId)
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '알 수 없는 오류가 발생했습니다.'
  }
}

function next() {
  if (route.query.from === 'mypage') {
    router.push('/mypage')
    return
  }
  router.push(route.query.from === 'review' ? '/onboarding/review' : '/onboarding/assets/settlement')
}
</script>

<template>
  <div class="page">
    <StepProgress :current-step="4" :total-steps="4" />

    <div class="content">
      <h1 class="title">이번 달 정기적으로<br />들어오는 돈이 있나요?</h1>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <div v-if="!loading" class="income-list">
        <div v-for="item in onboarding.incomes" :key="item.incomeId" class="income-card">
          <div class="income-info">
            <strong class="income-name">{{ item.name }}</strong>
            <span class="income-meta">
              {{ INCOME_TYPE_LABEL[item.incomeType] }}
              <template v-if="item.paymentTiming"> · {{ item.paymentTiming }}</template>
            </span>
          </div>
          <div class="income-right">
            <strong class="income-amount">{{ item.amount.toLocaleString('ko-KR') }}원</strong>
            <van-popover placement="bottom-end">
              <div class="menu">
                <button type="button" class="menu-item" @click="remove(item.incomeId)">삭제</button>
              </div>
              <template #reference>
                <button type="button" class="kebab" aria-label="더보기">⋮</button>
              </template>
            </van-popover>
          </div>
        </div>

        <button type="button" class="add-button" @click="router.push('/onboarding/income/new')">
          + 정기수입 추가
        </button>
      </div>
    </div>

    <div class="footer">
      <div class="total-row">
        <span>이번 달 예상 정기수입</span>
        <strong>{{ onboarding.totalMonthly.toLocaleString('ko-KR') }}원</strong>
      </div>
      <PrimaryButton @click="next">다음</PrimaryButton>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 0 var(--space-page);
}

.content {
  flex: 1;
  padding-top: var(--space-lg);
}

.title {
  margin-bottom: var(--space-lg);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.error {
  margin-bottom: var(--space-md);
  font-size: 13px;
  color: var(--color-danger);
}

.income-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.income-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.income-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.income-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary);
}

.income-meta {
  font-size: 12px;
  color: var(--color-secondary);
}

.income-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.income-amount {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary);
}

.kebab {
  padding: 4px 6px;
  font-size: 18px;
  line-height: 1;
  color: var(--color-secondary);
}

.menu {
  min-width: 96px;
}

.menu-item {
  width: 100%;
  padding: 10px 14px;
  font-size: 14px;
  color: var(--color-danger);
  text-align: left;
}

.add-button {
  height: 52px;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-secondary);
}

.footer {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}

.total-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-md);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
}
</style>

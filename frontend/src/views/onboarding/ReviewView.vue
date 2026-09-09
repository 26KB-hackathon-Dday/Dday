<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { HOUSING_TYPE_LABEL } from '@/api/onboarding'
import { useOnboardingStore } from '@/stores/onboarding'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const router = useRouter()
const onboarding = useOnboardingStore()

const housingLabel = computed(() =>
  onboarding.housingType ? HOUSING_TYPE_LABEL[onboarding.housingType] : '-',
)

const monthlyRentLabel = computed(() =>
  onboarding.skipHousingCost ? '해당없음' : `${onboarding.monthlyRent.toLocaleString('ko-KR')}원`,
)

function next() {
  router.push('/onboarding/processing')
}
</script>

<template>
  <div class="page">
    <div class="content">
      <h1 class="title">입력한 정보를 확인해주세요</h1>

      <section class="card">
        <div class="card-header">
          <strong>자립 정보</strong>
          <button
            type="button"
            class="edit"
            @click="router.push({ path: '/onboarding/protection-date', query: { from: 'review' } })"
          >
            수정
          </button>
        </div>
        <dl class="rows">
          <div class="row">
            <dt>보호종료일</dt>
            <dd>{{ onboarding.protectionEndDate.replace(/-/g, '.') }}</dd>
          </div>
          <div class="row">
            <dt>거주 지역</dt>
            <dd>{{ onboarding.regionName }} {{ onboarding.districtName }}</dd>
          </div>
        </dl>
      </section>

      <section class="card">
        <div class="card-header">
          <strong>주거</strong>
          <button
            type="button"
            class="edit"
            @click="router.push({ path: '/onboarding/housing-type', query: { from: 'review' } })"
          >
            수정
          </button>
        </div>
        <dl class="rows">
          <div class="row">
            <dt>주거 형태</dt>
            <dd>{{ housingLabel }}</dd>
          </div>
          <div class="row">
            <dt>월세</dt>
            <dd>{{ monthlyRentLabel }}</dd>
          </div>
        </dl>
      </section>

      <section class="card">
        <div class="card-header">
          <strong>수입/자산</strong>
          <button
            type="button"
            class="edit"
            @click="router.push({ path: '/onboarding/income', query: { from: 'review' } })"
          >
            수정
          </button>
        </div>
        <dl class="rows">
          <div class="row">
            <dt>정기수입</dt>
            <dd>자립수당 등 {{ onboarding.incomes.length }}건</dd>
          </div>
          <div class="row">
            <dt>모아둔 자산</dt>
            <dd>{{ onboarding.totalSaved.toLocaleString('ko-KR') }}원</dd>
          </div>
        </dl>
      </section>
    </div>

    <div class="actions">
      <PrimaryButton @click="next">이 정보로 계획 만들기</PrimaryButton>
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
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.card {
  padding: var(--space-md);
  margin-bottom: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-sm);
  font-size: 15px;
  color: var(--color-primary);
}

.edit {
  font-size: 13px;
  color: var(--color-secondary);
  text-decoration: underline;
  text-underline-offset: 2px;
}

.rows {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
}

.row dt {
  color: var(--color-secondary);
}

.row dd {
  font-weight: 600;
  color: var(--color-primary);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

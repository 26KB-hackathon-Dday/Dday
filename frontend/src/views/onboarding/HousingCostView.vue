<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '@/api/types'
import { HOUSING_TYPE_LABEL, type HousingType } from '@/api/onboarding'
import { useOnboardingStore } from '@/stores/onboarding'
import StepProgress from '@/components/signup/StepProgress.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import AmountField from '@/components/common/AmountField.vue'

const route = useRoute()
const router = useRouter()
const onboarding = useOnboardingStore()

const errorMessage = ref('')
const submitting = ref(false)

/**
 * 주거 형태별로 실제로 물어볼 필요가 있는 항목이 다르다.
 * LH전세임대·월세만 보증금이 의미 있고, 자립생활관·기숙사는 관리비(이용료) 정도만,
 * 가족과 함께 사는 경우엔 대부분 해당 사항이 없다 — 그래도 실비가 있으면 적을 수 있게 열어둔다.
 */
const HOUSING_COST_CONFIG: Record<
  HousingType,
  { showDeposit: boolean; showMonthlyRent: boolean; note: string }
> = {
  LH_JEONSE: {
    showDeposit: true,
    showMonthlyRent: true,
    note: '월세는 실제로 월세를 내고 있는 경우에만 입력해주세요.',
  },
  MONTHLY: { showDeposit: true, showMonthlyRent: true, note: '' },
  SELF_RELIANCE_HOUSE: {
    showDeposit: false,
    showMonthlyRent: false,
    note: '자립생활관 이용료가 있다면 관리비 항목에 입력해주세요.',
  },
  DORM: { showDeposit: false, showMonthlyRent: false, note: '' },
  FAMILY: { showDeposit: false, showMonthlyRent: false, note: '' },
  ETC: { showDeposit: true, showMonthlyRent: true, note: '해당하는 항목만 입력해주세요.' },
}

const housingType = computed(() => onboarding.housingType ?? 'ETC')
const config = computed(() => HOUSING_COST_CONFIG[housingType.value])
const isDorm = computed(() => housingType.value === 'DORM')

const title = computed(() =>
  housingType.value === 'MONTHLY'
    ? '월세 정보를 입력해주세요'
    : `${HOUSING_TYPE_LABEL[housingType.value]} 주거비를 입력해주세요`,
)

// 입력은 만원 단위로 받고, 스토어(=서버로 보낼 값)에는 원 단위로 환산해 둔다.
const depositManwon = computed({
  get: () => Math.round(onboarding.deposit / 10000),
  set: (value: number) => {
    onboarding.deposit = value * 10000
  },
})
const monthlyRentManwon = computed({
  get: () => Math.round(onboarding.monthlyRent / 10000),
  set: (value: number) => {
    onboarding.monthlyRent = value * 10000
  },
})
const maintenanceFeeManwon = computed({
  get: () => Math.round(onboarding.maintenanceFee / 10000),
  set: (value: number) => {
    onboarding.maintenanceFee = value * 10000
  },
})

/**
 * 기숙사는 보통 한 학기(6개월) 단위로 한 번에 낸다. 서버에는 매달 나가는 돈 기준인
 * `maintenanceFee`만 있으므로, 학기 비용을 입력받아 6개월로 나눈 월 환산값을 저장한다.
 */
const SEMESTER_MONTHS = 6
const dormSemesterFeeManwon = ref(Math.round((onboarding.maintenanceFee * SEMESTER_MONTHS) / 10000))
watch(dormSemesterFeeManwon, (value) => {
  onboarding.maintenanceFee = Math.round((value * 10000) / SEMESTER_MONTHS)
})
const dormMonthlyEquivalentManwon = computed(() =>
  Math.round(dormSemesterFeeManwon.value / SEMESTER_MONTHS),
)

const estimatedMonthlyManwon = computed(
  () => Math.round((onboarding.monthlyRent + onboarding.maintenanceFee) / 10000),
)

async function next() {
  if (submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await onboarding.submitHousingCost()
    router.push(route.query.from === 'review' ? '/onboarding/review' : '/onboarding/housing-checkpoint')
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '알 수 없는 오류가 발생했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <StepProgress :current-step="3" :total-steps="4" />

    <div class="content">
      <h1 class="title">{{ title }}</h1>

      <div class="skip-banner">
        <strong>해당 사항이 없으면 0원으로 두고 바로 다음으로 넘어가도 괜찮아요.</strong>
      </div>

      <p v-if="config.note" class="note">{{ config.note }}</p>

      <div v-if="config.showDeposit" class="field">
        <label class="label">보증금</label>
        <AmountField v-model="depositManwon" unit="만원" />
      </div>

      <div v-if="config.showMonthlyRent" class="field">
        <label class="label">월세</label>
        <AmountField v-model="monthlyRentManwon" unit="만원" />
      </div>

      <div v-if="isDorm" class="field">
        <label class="label">학기 비용</label>
        <AmountField v-model="dormSemesterFeeManwon" unit="만원" />
        <p class="hint">
          한 학기(6개월) 기준으로 입력하면 월 평균
          <strong>{{ dormMonthlyEquivalentManwon }}만원</strong>으로 환산해서 계산해요.
        </p>
      </div>
      <div v-else class="field">
        <label class="label">관리비</label>
        <AmountField v-model="maintenanceFeeManwon" unit="만원" />
      </div>

      <div class="field">
        <label class="label">월 예상 주거비</label>
        <AmountField :model-value="estimatedMonthlyManwon" unit="만원" readonly />
      </div>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </div>

    <div class="actions">
      <PrimaryButton :loading="submitting" @click="next">다음</PrimaryButton>
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
  margin-bottom: var(--space-md);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.skip-banner {
  padding: var(--space-md);
  margin-bottom: var(--space-md);
  background-color: #fff4e5;
  border: 1px solid #ffd8a8;
  border-radius: var(--radius-md);
  font-size: 14px;
  line-height: 1.5;
  color: #a15c00;
}

.note {
  margin-bottom: var(--space-md);
  font-size: 13px;
  color: var(--color-secondary);
}

.field {
  margin-bottom: var(--space-md);
}

.label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-secondary);
}

.hint {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-secondary);
}

.hint strong {
  color: var(--color-primary);
}

.error {
  margin-top: var(--space-sm);
  font-size: 13px;
  color: var(--color-danger);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

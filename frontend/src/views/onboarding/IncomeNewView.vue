<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '@/api/types'
import type { IncomeType } from '@/api/onboarding'
import { useOnboardingStore } from '@/stores/onboarding'
import TextField from '@/components/common/TextField.vue'
import AmountField from '@/components/common/AmountField.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const router = useRouter()
const onboarding = useOnboardingStore()

/** 자유 입력 대신 자주 쓰는 수입 항목을 버튼으로 고르게 한다. '기타'만 이름을 직접 적는다. */
const PRESETS: { key: string; name: string; incomeType: IncomeType }[] = [
  { key: 'JOB', name: '직장', incomeType: 'SALARY' },
  { key: 'PARTTIME', name: '아르바이트', incomeType: 'SALARY' },
  { key: 'ALLOWANCE', name: '자립수당', incomeType: 'ALLOWANCE' },
  { key: 'ETC', name: '기타', incomeType: 'ETC' },
]

const DAYS = Array.from({ length: 31 }, (_, i) => i + 1)

const selectedPreset = ref<(typeof PRESETS)[number] | null>(null)
const customName = ref('')
const amountManwon = ref(0)
const selectedDay = ref<number | null>(null)
const errorMessage = ref('')
const submitting = ref(false)

const isEtc = computed(() => selectedPreset.value?.key === 'ETC')

const name = computed(() => (isEtc.value ? customName.value.trim() : (selectedPreset.value?.name ?? '')))

const canSubmit = computed(
  () => !!selectedPreset.value && !!name.value && amountManwon.value > 0 && !!selectedDay.value,
)

function selectPreset(preset: (typeof PRESETS)[number]) {
  selectedPreset.value = preset
}

async function save() {
  if (!canSubmit.value || submitting.value || !selectedPreset.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await onboarding.addIncome({
      name: name.value,
      incomeType: selectedPreset.value.incomeType,
      amount: amountManwon.value * 10000,
      paymentTiming: `매월 ${selectedDay.value}일`,
    })
    router.back()
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '알 수 없는 오류가 발생했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <header class="header">
      <button class="back" type="button" aria-label="뒤로" @click="router.back()">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="m15 5-7 7 7 7" />
        </svg>
      </button>
    </header>

    <div class="content">
      <h1 class="title">정기수입 정보를 입력해주세요</h1>

      <div class="field">
        <label class="label">수입 종류</label>
        <div class="preset-grid">
          <button
            v-for="preset in PRESETS"
            :key="preset.key"
            type="button"
            class="preset"
            :class="{ 'is-selected': selectedPreset?.key === preset.key }"
            @click="selectPreset(preset)"
          >
            {{ preset.name }}
          </button>
        </div>
      </div>

      <div v-if="isEtc" class="field">
        <label class="label">수입 이름</label>
        <TextField v-model="customName" placeholder="예: 후원금" />
      </div>

      <div class="field">
        <label class="label">예상 금액</label>
        <AmountField v-model="amountManwon" unit="만원" />
      </div>

      <div class="field">
        <label class="label">입금 시기</label>
        <div class="day-grid">
          <button
            v-for="day in DAYS"
            :key="day"
            type="button"
            class="day"
            :class="{ 'is-selected': selectedDay === day }"
            @click="selectedDay = day"
          >
            {{ day }}
          </button>
        </div>
        <p v-if="selectedDay" class="day-hint">매월 {{ selectedDay }}일에 들어와요</p>
      </div>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </div>

    <div class="actions">
      <PrimaryButton :disabled="!canSubmit" :loading="submitting" @click="save">
        저장하기
      </PrimaryButton>
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

.header {
  display: flex;
  align-items: center;
  height: 56px;
  margin-left: -8px;
}

.back {
  display: flex;
  padding: 8px;
  color: var(--color-primary);
}

.back svg {
  width: 24px;
  height: 24px;
}

.content {
  flex: 1;
}

.title {
  margin-bottom: var(--space-lg);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-primary);
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

.preset-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.preset {
  height: 48px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
}

.preset.is-selected {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: #ffffff;
}

.day-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.day {
  height: 36px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
}

.day.is-selected {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: #ffffff;
}

.day-hint {
  margin-top: 8px;
  font-size: 13px;
  color: var(--color-secondary);
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

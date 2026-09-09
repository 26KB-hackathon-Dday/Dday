<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '@/api/types'
import { useOnboardingStore } from '@/stores/onboarding'
import StepProgress from '@/components/signup/StepProgress.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const route = useRoute()
const router = useRouter()
const onboarding = useOnboardingStore()

const today = new Date()
const pickerValue = ref<string[]>([
  String(today.getFullYear()),
  String(today.getMonth() + 1).padStart(2, '0'),
  String(today.getDate()).padStart(2, '0'),
])
const selectedDate = ref('')
const showPicker = ref(false)
const errorMessage = ref('')
const submitting = ref(false)

const minDate = new Date(today.getFullYear() - 10, 0, 1)
const maxDate = new Date(today.getFullYear() + 10, 11, 31)

const displayDate = computed(() =>
  selectedDate.value ? selectedDate.value.replace(/-/g, '.') : '',
)
const canSubmit = computed(() => !!selectedDate.value)

function onConfirm({ selectedValues }: { selectedValues: string[] }) {
  pickerValue.value = selectedValues
  selectedDate.value = selectedValues.join('-')
  showPicker.value = false
}

async function next() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await onboarding.submitProtectionDate(selectedDate.value)
    router.push({ path: '/onboarding/protection-date/confirm', query: route.query })
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '알 수 없는 오류가 발생했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <StepProgress :current-step="1" :total-steps="4" />

    <div class="content">
      <h1 class="title">보호종료일을 알려주세요</h1>
      <p class="hint">보호종료일을 기준으로 5년간 자립 여정을 계산해드려요.</p>

      <button class="date-field" type="button" @click="showPicker = true">
        <span :class="{ placeholder: !displayDate }">
          {{ displayDate || 'YYYY.MM.DD' }}
        </span>
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.7"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <rect x="3" y="5" width="18" height="16" rx="2.5" />
          <path d="M3 10h18M8 3v4M16 3v4" />
        </svg>
      </button>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </div>

    <div class="actions">
      <PrimaryButton :disabled="!canSubmit" :loading="submitting" @click="next">다음</PrimaryButton>
    </div>

    <van-popup v-model:show="showPicker" position="bottom" round>
      <van-date-picker
        v-model="pickerValue"
        title="보호종료일 선택"
        :min-date="minDate"
        :max-date="maxDate"
        @confirm="onConfirm"
        @cancel="showPicker = false"
      />
    </van-popup>
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
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.hint {
  margin-top: var(--space-sm);
  font-size: 14px;
  color: var(--color-secondary);
}

.date-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 56px;
  margin-top: var(--space-lg);
  padding: 0 var(--space-md);
  background-color: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 600;
  color: var(--color-primary);
}

.date-field .placeholder {
  color: var(--color-disabled);
  font-weight: 500;
}

.date-field svg {
  width: 20px;
  height: 20px;
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

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '@/api/types'
import { HOUSING_TYPE_LABEL, type HousingType } from '@/api/onboarding'
import { useOnboardingStore } from '@/stores/onboarding'
import StepProgress from '@/components/signup/StepProgress.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const route = useRoute()
const router = useRouter()
const onboarding = useOnboardingStore()

const options: HousingType[] = [
  'LH_JEONSE',
  'MONTHLY',
  'SELF_RELIANCE_HOUSE',
  'DORM',
  'FAMILY',
  'ETC',
]

const selected = ref<HousingType | null>(onboarding.housingType)
const errorMessage = ref('')
const submitting = ref(false)

async function next() {
  if (!selected.value || submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    const result = await onboarding.submitHousingType(selected.value)
    // 주거비 입력이 필요 없는 형태면 그 화면과 다음 체크포인트를 건너뛴다.
    if (result.skipHousingCost) {
      if (route.query.from === 'mypage') {
        router.push('/mypage')
      } else {
        router.push(route.query.from === 'review' ? '/onboarding/review' : '/onboarding/income')
      }
    } else {
      router.push({ path: '/onboarding/housing-cost', query: route.query })
    }
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
      <h1 class="title">현재 어떤 곳에서<br />생활하고 있나요?</h1>

      <div class="grid">
        <button
          v-for="option in options"
          :key="option"
          type="button"
          class="card"
          :class="{ 'is-selected': selected === option }"
          @click="selected = option"
        >
          {{ HOUSING_TYPE_LABEL[option] }}
        </button>
      </div>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </div>

    <div class="actions">
      <PrimaryButton :disabled="!selected" :loading="submitting" @click="next">다음</PrimaryButton>
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

.grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.card {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88px;
  padding: 0 var(--space-sm);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  color: var(--color-primary);
  text-align: center;
}

.card.is-selected {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: #ffffff;
}

.error {
  margin-top: var(--space-md);
  font-size: 13px;
  color: var(--color-danger);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

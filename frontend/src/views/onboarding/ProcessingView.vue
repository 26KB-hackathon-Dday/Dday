<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '@/api/types'
import { useOnboardingStore } from '@/stores/onboarding'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const onboarding = useOnboardingStore()
const auth = useAuthStore()

const steps = [
  '지원 종료일까지 남은 기간 계산',
  '확정 수입 확인',
  '필요한 생활비 계산',
  '받을 수 있는 지원제도 확인',
  '자금 배분 계획 생성',
]

const doneCount = ref(0)
const errorMessage = ref('')

function playChecklist() {
  return new Promise<void>((resolve) => {
    let i = 0
    const tick = () => {
      doneCount.value = i + 1
      i += 1
      if (i >= steps.length) {
        resolve()
        return
      }
      setTimeout(tick, 700)
    }
    setTimeout(tick, 700)
  })
}

onMounted(async () => {
  try {
    const [result] = await Promise.all([onboarding.complete(), playChecklist()])
    void result
    auth.setOnboardingCompleted(true)
    router.replace('/onboarding/done')
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '알 수 없는 오류가 발생했습니다.'
  }
})
</script>

<template>
  <div class="page">
    <p class="eyebrow">Onboarding</p>

    <div class="content">
      <div class="spinner-icon">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
        >
          <circle cx="12" cy="12" r="9" stroke-opacity="0.2" />
          <path d="M21 12a9 9 0 0 0-9-9" />
        </svg>
      </div>

      <h1 class="title">나에게 맞는<br />자립 계획을 만들고 있어요</h1>

      <ul class="checklist">
        <li v-for="(step, index) in steps" :key="step" :class="{ 'is-done': index < doneCount }">
          <span class="check">
            <svg
              v-if="index < doneCount"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="3"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="m5 12.5 4.5 4.5L19 7" />
            </svg>
          </span>
          {{ step }}
        </li>
      </ul>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
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

.eyebrow {
  padding-top: var(--space-lg);
  font-size: 13px;
  font-weight: 700;
  color: var(--color-secondary);
  text-align: center;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.spinner-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin-bottom: var(--space-lg);
  color: var(--color-primary);
  animation: spin 1s linear infinite;
}

.spinner-icon svg {
  width: 40px;
  height: 40px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .spinner-icon {
    animation: none;
  }
}

.title {
  margin-bottom: var(--space-lg);
  font-size: 20px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: -0.02em;
  color: var(--color-primary);
  text-align: center;
}

.checklist {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.checklist li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--color-disabled);
  transition: color 0.2s;
}

.checklist li.is-done {
  color: var(--color-primary);
  font-weight: 600;
}

.check {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex: none;
  border: 1.5px solid var(--color-border);
  border-radius: 50%;
  color: #ffffff;
}

.checklist li.is-done .check {
  background-color: #27b87e;
  border-color: #27b87e;
}

.check svg {
  width: 12px;
  height: 12px;
}

.error {
  margin-top: var(--space-lg);
  font-size: 13px;
  color: var(--color-danger);
  text-align: center;
}
</style>

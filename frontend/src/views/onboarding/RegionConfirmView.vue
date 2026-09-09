<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '@/api/types'
import { useOnboardingStore } from '@/stores/onboarding'
import StepProgress from '@/components/signup/StepProgress.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const route = useRoute()
const router = useRouter()
const onboarding = useOnboardingStore()

const errorMessage = ref('')
const submitting = ref(false)

async function next() {
  if (submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await onboarding.submitRegion()
    router.push(route.query.from === 'mypage' ? '/mypage' : '/onboarding/housing-type')
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '알 수 없는 오류가 발생했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <StepProgress :current-step="2" :total-steps="4" />

    <div class="content">
      <div class="pin-icon">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M12 21s7-6.5 7-12a7 7 0 1 0-14 0c0 5.5 7 12 7 12Z" />
          <circle cx="12" cy="9" r="2.5" />
        </svg>
      </div>

      <h1 class="title">{{ onboarding.regionName }} {{ onboarding.districtName }}</h1>
      <p class="subtitle">이 지역을 기준으로 받을 수 있는 지원제도를 찾아드려요.</p>

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
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.pin-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin-bottom: var(--space-lg);
  color: var(--color-primary);
  background-color: var(--color-bg-soft);
  border-radius: 50%;
}

.pin-icon svg {
  width: 32px;
  height: 32px;
}

.title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.subtitle {
  margin-top: var(--space-sm);
  font-size: 14px;
  color: var(--color-secondary);
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

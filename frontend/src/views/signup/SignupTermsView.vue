<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '@/api/types'
import { useSignupStore } from '@/stores/signup'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import AgreementItem from '@/components/signup/AgreementItem.vue'

const router = useRouter()
const signup = useSignupStore()

const errorMessage = ref('')

const allAgreed = computed({
  get: () => signup.agreedTerms && signup.agreedPrivacy && signup.agreedLocation,
  set: (value: boolean) => {
    signup.agreedTerms = value
    signup.agreedPrivacy = value
    signup.agreedLocation = value
  },
})

const canSubmit = computed(() => signup.agreedTerms && signup.agreedPrivacy)

async function submit() {
  if (!canSubmit.value || signup.submitting) return

  errorMessage.value = ''
  try {
    await signup.submit()
    signup.reset()
    router.replace('/mydata')
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '회원가입에 실패했습니다.'
  }
}
</script>

<template>
  <div class="page">
    <header class="header">
      <button class="back" type="button" aria-label="뒤로" @click="router.back()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 5-7 7 7 7" />
        </svg>
      </button>
    </header>

    <div class="content">
      <h1 class="title">서비스 이용을 위해<br />동의가 필요해요</h1>

      <label class="all-agree">
        <input
          type="checkbox"
          :checked="allAgreed"
          @change="allAgreed = ($event.target as HTMLInputElement).checked"
        />
        <span class="box" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <path d="m5 12.5 4.5 4.5L19 7" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </span>
        약관 전체 동의
      </label>

      <div class="list">
        <AgreementItem v-model="signup.agreedTerms" label="서비스 이용약관" required />
        <AgreementItem v-model="signup.agreedPrivacy" label="개인정보 수집 이용 동의" required />
        <AgreementItem v-model="signup.agreedLocation" label="위치정보 이용 동의" />
      </div>

      <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>
    </div>

    <div class="actions">
      <PrimaryButton :disabled="!canSubmit" :loading="signup.submitting" @click="submit">
        동의하고 계속하기
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
  padding-top: var(--space-sm);
}

.title {
  margin-bottom: var(--space-lg);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.all-agree {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: var(--space-md);
  margin-bottom: var(--space-sm);
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary);
  background-color: var(--color-bg-soft);
  border-radius: var(--radius-md);
  cursor: pointer;
}

.all-agree input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.all-agree .box {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  color: transparent;
  transition:
    background-color 0.15s,
    border-color 0.15s,
    color 0.15s;
}

.all-agree .box svg {
  width: 13px;
  height: 13px;
}

.all-agree input:checked + .box {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: #ffffff;
}

.list {
  padding: 0 var(--space-sm);
}

.error {
  margin-top: var(--space-md);
  font-size: 14px;
  color: var(--color-danger);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

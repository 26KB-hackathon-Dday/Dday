<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { ApiError } from '@/api/types'
import { useSignupStore } from '@/stores/signup'
import StepProgress from '@/components/signup/StepProgress.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import TextField from '@/components/common/TextField.vue'

const router = useRouter()
const signup = useSignupStore()

const errorMessage = ref('')
const checking = ref(false)

const canCheck = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(signup.email))

function onInput(value: string) {
  errorMessage.value = ''
  signup.setEmail(value)
}

async function checkEmail() {
  if (!canCheck.value || checking.value) return

  checking.value = true
  errorMessage.value = ''
  try {
    const result = await authApi.checkEmail({ email: signup.email })
    if (!result.available) {
      errorMessage.value = '이미 가입된 이메일입니다.'
      return
    }
    signup.emailChecked = true
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '이메일 확인에 실패했습니다.'
  } finally {
    checking.value = false
  }
}

function next() {
  if (!signup.emailChecked) return
  router.push('/signup/password')
}
</script>

<template>
  <div class="page">
    <StepProgress :current-step="5" :total-steps="6" />

    <div class="content">
      <h1 class="title">사용하실 이메일을 입력해 주세요</h1>

      <div class="field">
        <div class="row">
          <TextField
            :model-value="signup.email"
            type="email"
            icon="mail"
            placeholder="이메일"
            inputmode="email"
            autocomplete="email"
            @update:model-value="onInput"
            @enter="checkEmail"
          />
          <button
            class="check-button"
            :class="{ checked: signup.emailChecked }"
            type="button"
            :disabled="!canCheck || checking"
            @click="checkEmail"
          >
            <svg
              v-if="signup.emailChecked"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="3"
            >
              <path d="m5 12.5 4.5 4.5L19 7" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            {{ signup.emailChecked ? '확인 완료' : '중복 확인' }}
          </button>
        </div>

        <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>
        <p v-else-if="signup.emailChecked" class="success">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="m5 12.5 4.5 4.5L19 7" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          사용 가능한 이메일이에요.
        </p>
      </div>
    </div>

    <div class="actions">
      <PrimaryButton :disabled="!signup.emailChecked" @click="next">다음</PrimaryButton>
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

.row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.check-button {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: none;
  height: 54px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-secondary);
  background-color: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  white-space: nowrap;
  transition:
    opacity 0.15s,
    background-color 0.15s,
    border-color 0.15s,
    color 0.15s;
}

.check-button:disabled {
  opacity: 0.5;
}

.check-button:not(:disabled):active {
  opacity: 0.7;
}

/* 확인이 끝났다는 걸 문구뿐 아니라 버튼 자체로도 눈에 띄게 한다 — 아래 성공 문구만으로는
   버튼을 다시 눌러도 되는지 헷갈린다는 피드백이 있었다. */
.check-button.checked {
  color: #ffffff;
  background-color: #2e9e5b;
  border-color: #2e9e5b;
}

.check-button.checked:disabled {
  opacity: 1;
}

.check-button.checked svg {
  width: 14px;
  height: 14px;
}

.error {
  margin-top: var(--space-sm);
  padding-left: 2px;
  font-size: 13px;
  color: var(--color-danger);
}

.success {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: var(--space-sm);
  padding-left: 2px;
  font-size: 13px;
  color: #2e9e5b;
}

.success svg {
  width: 16px;
  height: 16px;
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

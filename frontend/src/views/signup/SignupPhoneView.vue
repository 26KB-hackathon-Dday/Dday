<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { ApiError } from '@/api/types'
import { useSignupStore, toHyphenatedPhone } from '@/stores/signup'
import StepProgress from '@/components/signup/StepProgress.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import TextField from '@/components/common/TextField.vue'

const router = useRouter()
const signup = useSignupStore()

const errorMessage = ref('')
const submitting = ref(false)

/** 화면에는 하이픈 포함으로 보여주고, 스토어에는 숫자만(11자리) 저장한다. */
const displayValue = computed({
  get: () => toHyphenatedPhone(signup.phone),
  set: (value: string) => {
    signup.setPhone(value.replace(/\D/g, '').slice(0, 11))
  },
})

const canSubmit = computed(() => signup.phone.length >= 10 && signup.phone.length <= 11)

async function submit() {
  if (!canSubmit.value || submitting.value) return

  submitting.value = true
  errorMessage.value = ''
  try {
    await authApi.sendPhoneCode({ phone: toHyphenatedPhone(signup.phone), purpose: 'SIGNUP' })
    router.push('/signup/phone/verify')
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '인증번호 발송에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <StepProgress :current-step="3" :total-steps="6" />

    <div class="content">
      <h1 class="title">휴대폰 번호를 입력해 주세요</h1>

      <div class="field">
        <TextField
          v-model="displayValue"
          type="tel"
          placeholder="010-0000-0000"
          inputmode="numeric"
          autocomplete="tel"
          :maxlength="13"
          @enter="submit"
        />
        <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>
      </div>
    </div>

    <div class="actions">
      <PrimaryButton :disabled="!canSubmit" :loading="submitting" @click="submit">
        인증번호 받기
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

.error {
  margin-top: var(--space-sm);
  padding-left: 2px;
  font-size: 13px;
  color: var(--color-danger);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

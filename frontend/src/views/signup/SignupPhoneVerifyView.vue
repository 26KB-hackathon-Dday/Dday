<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { ApiError } from '@/api/types'
import { useSignupStore, toHyphenatedPhone } from '@/stores/signup'
import StepProgress from '@/components/signup/StepProgress.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import OtpInput from '@/components/signup/OtpInput.vue'

const router = useRouter()
const signup = useSignupStore()

const code = ref('')
const errorMessage = ref('')
const submitting = ref(false)
const expired = ref(false)

const canSubmit = computed(() => code.value.length === 6)

async function submit() {
  if (!canSubmit.value || submitting.value) return

  submitting.value = true
  errorMessage.value = ''
  try {
    const result = await authApi.verifyPhoneCode({
      phone: toHyphenatedPhone(signup.phone),
      code: code.value,
    })
    if (!result.verified) {
      errorMessage.value = '인증번호가 일치하지 않습니다.'
      return
    }
    signup.phoneVerified = true
    router.push('/signup/email')
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '인증번호가 일치하지 않습니다.'
  } finally {
    submitting.value = false
  }
}

async function resend() {
  if (submitting.value) return
  errorMessage.value = ''
  code.value = ''
  expired.value = false
  try {
    await authApi.sendPhoneCode({ phone: toHyphenatedPhone(signup.phone), purpose: 'SIGNUP' })
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '인증번호 발송에 실패했습니다.'
  }
}

function onExpire() {
  expired.value = true
  errorMessage.value = '인증번호가 만료되었습니다. 다시 발송해주세요.'
}
</script>

<template>
  <div class="page">
    <StepProgress :current-step="4" :total-steps="6" />

    <div class="content">
      <h1 class="title">인증번호 6자리를 입력해 주세요</h1>

      <OtpInput
        v-model="code"
        :error="errorMessage"
        @complete="submit"
        @expire="onExpire"
      />

      <button class="resend" type="button" @click="resend">인증번호 다시 받기</button>
    </div>

    <div class="actions">
      <PrimaryButton :disabled="!canSubmit || expired" :loading="submitting" @click="submit">
        다음
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

.resend {
  margin-top: var(--space-md);
  padding-left: 2px;
  font-size: 13px;
  color: var(--color-secondary);
  text-decoration: underline;
  text-underline-offset: 2px;
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

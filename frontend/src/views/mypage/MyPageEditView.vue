<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { userApi } from '@/api/user'
import { authApi } from '@/api/auth'
import { ApiError } from '@/api/types'
import { toHyphenatedPhone } from '@/stores/signup'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import TextField from '@/components/common/TextField.vue'
import OtpInput from '@/components/signup/OtpInput.vue'

const router = useRouter()

const loading = ref(true)
const submitting = ref(false)

const originalPhone = ref('')
const name = ref('')
/** 숫자만(11자리) 보관. 화면에는 하이픈 포함으로 보여준다 */
const phoneDigits = ref('')

const displayPhone = computed({
  get: () => toHyphenatedPhone(phoneDigits.value),
  set: (value: string) => {
    phoneDigits.value = value.replace(/\D/g, '').slice(0, 11)
    resetPhoneVerification()
  },
})

// 번호를 바꾸면 회원가입 때처럼 인증번호로 본인확인을 거친다
const phoneChanged = computed(() => toHyphenatedPhone(phoneDigits.value) !== originalPhone.value)
const phoneVerified = ref(false)
const codeSent = ref(false)
const code = ref('')
const codeError = ref('')
const expiresIn = ref(180)
const sending = ref(false)

const nameError = ref('')
const phoneFormatError = computed(() =>
  phoneDigits.value && phoneDigits.value.length < 10 ? '휴대폰 번호 형식이 올바르지 않습니다.' : '',
)

const canSubmit = computed(
  () =>
    !!name.value &&
    !phoneFormatError.value &&
    (!phoneChanged.value || phoneVerified.value) &&
    !submitting.value,
)

onMounted(async () => {
  try {
    const me = await userApi.fetchMe()
    name.value = me.name
    phoneDigits.value = me.phone.replace(/\D/g, '')
    originalPhone.value = me.phone
  } catch (e) {
    showToast(e instanceof ApiError ? e.message : '정보를 불러오지 못했습니다.')
  } finally {
    loading.value = false
  }
})

function resetPhoneVerification() {
  phoneVerified.value = false
  codeSent.value = false
  code.value = ''
  codeError.value = ''
}

/** 실제 문자 발송처럼 최소한의 로딩을 보여준다 — API가 즉시 응답해도 버튼이 깜빡이지 않게 */
const minDelay = (ms = 900) => new Promise((resolve) => setTimeout(resolve, ms))

async function sendCode() {
  if (phoneFormatError.value || !phoneDigits.value || sending.value) return
  sending.value = true
  codeError.value = ''
  try {
    const [result] = await Promise.all([
      authApi.sendPhoneCode({
        phone: toHyphenatedPhone(phoneDigits.value),
        purpose: 'SIGNUP',
      }),
      minDelay(),
    ])
    expiresIn.value = result.expiresIn
    codeSent.value = true
    code.value = ''
  } catch (e) {
    showToast(e instanceof ApiError ? e.message : '인증번호 발송에 실패했습니다.')
  } finally {
    sending.value = false
  }
}

async function verifyCode() {
  if (code.value.length !== 6) return
  codeError.value = ''
  try {
    const result = await authApi.verifyPhoneCode({
      phone: toHyphenatedPhone(phoneDigits.value),
      code: code.value,
    })
    if (!result.verified) {
      codeError.value = '인증번호가 일치하지 않습니다.'
      return
    }
    phoneVerified.value = true
  } catch (e) {
    codeError.value = e instanceof ApiError ? e.message : '인증번호가 일치하지 않습니다.'
  }
}

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  nameError.value = ''
  try {
    await userApi.updateMe({
      name: name.value,
      phone: phoneChanged.value ? toHyphenatedPhone(phoneDigits.value) : undefined,
    })
    showToast('기본 정보를 수정했어요')
    router.back()
  } catch (e) {
    if (e instanceof ApiError) {
      nameError.value = e.fieldErrors?.name ?? ''
      showToast(e.message)
    } else {
      showToast('수정에 실패했습니다.')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div v-if="!loading" class="page">
    <div class="content">
      <div class="field">
        <label class="label">이름</label>
        <TextField v-model="name" placeholder="이름" :error="nameError" />
      </div>

      <div class="field">
        <label class="label">휴대폰 번호</label>
        <div class="phone-row">
          <TextField
            v-model="displayPhone"
            type="tel"
            inputmode="numeric"
            placeholder="010-1234-5678"
            :maxlength="13"
            :error="phoneFormatError"
          />
          <button
            v-if="phoneChanged && !phoneVerified"
            type="button"
            class="verify-btn"
            :disabled="!!phoneFormatError || sending"
            @click="sendCode"
          >
            {{ sending ? '발송 중...' : codeSent ? '재발송' : '인증번호 발송' }}
          </button>
          <span v-else-if="phoneVerified" class="verified">인증완료</span>
        </div>

        <div v-if="phoneChanged && codeSent && !phoneVerified" class="otp-wrap">
          <OtpInput
            v-model="code"
            :expires-in="expiresIn"
            :error="codeError"
            @complete="verifyCode"
          />
        </div>
      </div>
    </div>

    <div class="actions">
      <PrimaryButton :disabled="!canSubmit" :loading="submitting" @click="submit">
        저장
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
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
  padding-top: var(--space-lg);
}

.label {
  display: block;
  margin-bottom: var(--space-sm);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
}

.phone-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.phone-row > .text-field {
  flex: 1;
}

.verify-btn {
  flex: none;
  height: 54px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  background: var(--color-bg-soft);
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.verify-btn:disabled {
  color: var(--color-disabled);
}

.verified {
  flex: none;
  font-size: 14px;
  font-weight: 600;
  color: #2e9e5b;
  white-space: nowrap;
}

.otp-wrap {
  margin-top: var(--space-md);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSignupStore } from '@/stores/signup'
import StepProgress from '@/components/signup/StepProgress.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import TextField from '@/components/common/TextField.vue'

const router = useRouter()
const signup = useSignupStore()

const password = ref('')
const passwordConfirm = ref('')

/** 8~16자, 영문·숫자·특수문자 모두 포함 */
const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,16}$/

const isValidFormat = computed(() => PASSWORD_RULE.test(password.value))
const isMatching = computed(() => !!password.value && password.value === passwordConfirm.value)
const canSubmit = computed(() => isValidFormat.value && isMatching.value)

const confirmError = computed(() =>
  passwordConfirm.value && !isMatching.value ? '비밀번호가 일치하지 않습니다.' : '',
)

function next() {
  if (!canSubmit.value) return
  signup.password = password.value
  router.push('/signup/terms')
}
</script>

<template>
  <div class="page">
    <StepProgress :current-step="6" :total-steps="6" />

    <div class="content">
      <h1 class="title">로그인에 사용할<br />비밀번호를 설정해 주세요</h1>

      <div class="field">
        <TextField
          v-model="password"
          type="password"
          icon="lock"
          placeholder="비밀번호"
          autocomplete="new-password"
          toggle-password
        />
        <p class="rule" :class="{ 'is-valid': isValidFormat }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="m5 12.5 4.5 4.5L19 7" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          8~16자 영문, 숫자, 특수문자 조합
        </p>
      </div>

      <div class="field">
        <TextField
          v-model="passwordConfirm"
          type="password"
          icon="lock"
          placeholder="비밀번호 확인"
          autocomplete="new-password"
          toggle-password
          :error="confirmError"
          @enter="next"
        />
      </div>
    </div>

    <div class="actions">
      <PrimaryButton :disabled="!canSubmit" @click="next">다음</PrimaryButton>
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

.title {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.rule {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: var(--space-sm);
  padding-left: 2px;
  font-size: 13px;
  color: var(--color-secondary);
  transition: color 0.15s;
}

.rule svg {
  width: 15px;
  height: 15px;
}

.rule.is-valid {
  color: #2e9e5b;
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

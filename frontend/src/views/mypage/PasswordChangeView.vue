<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { userApi } from '@/api/user'
import { ApiError } from '@/api/types'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import TextField from '@/components/common/TextField.vue'

const router = useRouter()

const currentPassword = ref('')
const newPassword = ref('')
const newPasswordConfirm = ref('')
const submitting = ref(false)
const currentPasswordError = ref('')

/** 8~16자, 영문·숫자·특수문자 모두 포함 — 회원가입 규칙과 동일 */
const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,16}$/

const isValidFormat = computed(() => PASSWORD_RULE.test(newPassword.value))
const isMatching = computed(
  () => !!newPassword.value && newPassword.value === newPasswordConfirm.value,
)
const confirmError = computed(() =>
  newPasswordConfirm.value && !isMatching.value ? '비밀번호가 일치하지 않습니다.' : '',
)

const canSubmit = computed(
  () => !!currentPassword.value && isValidFormat.value && isMatching.value && !submitting.value,
)

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  currentPasswordError.value = ''
  try {
    await userApi.changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
    })
    showToast('비밀번호를 변경했어요')
    router.back()
  } catch (e) {
    if (e instanceof ApiError) {
      currentPasswordError.value =
        e.code === 'PASSWORD_MISMATCH' || e.code === 'SAME_AS_OLD_PASSWORD' ? e.message : ''
      showToast(e.message)
    } else {
      showToast('변경에 실패했습니다.')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="content">
      <div class="field">
        <label class="label">현재 비밀번호</label>
        <TextField
          v-model="currentPassword"
          type="password"
          icon="lock"
          placeholder="현재 비밀번호"
          autocomplete="current-password"
          toggle-password
          :error="currentPasswordError"
        />
      </div>

      <div class="field">
        <label class="label">새 비밀번호</label>
        <TextField
          v-model="newPassword"
          type="password"
          icon="lock"
          placeholder="새 비밀번호"
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
        <label class="label">새 비밀번호 확인</label>
        <TextField
          v-model="newPasswordConfirm"
          type="password"
          icon="lock"
          placeholder="새 비밀번호 확인"
          autocomplete="new-password"
          toggle-password
          :error="confirmError"
          @enter="submit"
        />
      </div>
    </div>

    <div class="actions">
      <PrimaryButton :disabled="!canSubmit" :loading="submitting" @click="submit">
        변경하기
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

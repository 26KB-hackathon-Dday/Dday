<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { ApiError } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import TextField from '@/components/common/TextField.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const email = ref('')
const password = ref('')
const keepLoggedIn = ref(true)
const errorMessage = ref('')
const submitting = ref(false)

const canSubmit = computed(() => !!email.value.trim() && !!password.value)

/** 비밀번호 찾기 화면은 이번 범위 밖이다. 라우트가 생기면 router.push로 바꾼다. */
function findPassword() {
  errorMessage.value = '비밀번호 찾기는 준비 중이에요.'
}

async function submit() {
  if (!canSubmit.value || submitting.value) return

  submitting.value = true
  errorMessage.value = ''
  try {
    const result = await authApi.login({
      email: email.value.trim(),
      password: password.value,
      rememberMe: keepLoggedIn.value,
    })
    auth.setTokens(result)
    auth.setOnboardingCompleted(result.user.onboardingCompleted)

    // 가드가 붙여준 원래 목적지가 있으면 그리로, 온보딩이 안 끝났으면 온보딩으로, 아니면 홈으로.
    // replace라서 뒤로가기로 로그인 화면에 다시 오지 않는다.
    const redirect = route.query.redirect
    if (typeof redirect === 'string') router.replace(redirect)
    else if (!result.user.onboardingCompleted) router.replace('/onboarding/intro')
    else router.replace('/pockets')
  } catch (e) {
    // 백엔드 ErrorCode의 message가 문구의 정본이라 그대로 띄운다 (AGENTS.md §2).
    errorMessage.value = e instanceof ApiError ? e.message : '알 수 없는 오류가 발생했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login">
    <header class="header">
      <button class="back" type="button" aria-label="뒤로" @click="router.back()">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="m15 5-7 7 7 7" />
        </svg>
      </button>
    </header>

    <h1 class="title">다시 만나서 반가워요</h1>

    <form class="form" novalidate @submit.prevent="submit">
      <!-- 입력 묶음을 연한 회색 박스로 감싼다. 입력창은 흰색이라 대비로 떠 보인다 -->
      <div class="form-card">
        <TextField
          v-model="email"
          type="email"
          icon="mail"
          placeholder="이메일"
          inputmode="email"
          autocomplete="email"
        />
        <TextField
          v-model="password"
          type="password"
          icon="lock"
          placeholder="비밀번호"
          autocomplete="current-password"
          toggle-password
          @enter="submit"
        />

        <div class="options">
          <label class="keep">
            <input v-model="keepLoggedIn" type="checkbox" />
            <span class="box" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                <path d="m5 12.5 4.5 4.5L19 7" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </span>
            로그인 상태 유지
          </label>

          <!-- 비밀번호 찾기 화면은 아직 없다. 라우트가 생기면 RouterLink로 바꾼다 -->
          <button class="find" type="button" @click="findPassword">비밀번호 찾기</button>
        </div>
      </div>

      <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>

      <!-- 버튼은 카드 밖 화면 하단에 두지만 form 안에 있어야 엔터 제출이 먹는다 -->
      <div class="actions">
        <PrimaryButton type="submit" :disabled="!canSubmit" :loading="submitting">
          로그인
        </PrimaryButton>
      </div>
    </form>
  </div>
</template>

<style scoped>
.login {
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

.title {
  margin: var(--space-sm) 0 var(--space-lg);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-primary);
  text-align: left;
}

/* form이 남는 높이를 전부 먹고, 그 안에서 .actions가 margin-top:auto로
   바닥에 붙는다. position:fixed를 안 써서 430px 래퍼를 벗어날 일이 없다. */
.form {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.form-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: var(--space-lg) var(--space-md);
  background-color: var(--color-bg-soft);
  border-radius: var(--radius-lg);
}

.options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 14px;
}

.keep {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-secondary);
  cursor: pointer;
}

/* 기본 체크박스는 OS마다 모양이 달라 숨기고 직접 그린다.
   접근성(포커스·스페이스바)은 살아 있어야 하니 지우지 않고 가린다. */
.keep input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: transparent;
  transition:
    background-color 0.15s,
    border-color 0.15s,
    color 0.15s;
}

.box svg {
  width: 12px;
  height: 12px;
}

.keep input:checked + .box {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: #ffffff;
}

.keep input:focus-visible + .box {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}

.find {
  font-size: 14px;
  color: var(--color-secondary);
  text-decoration: underline;
  text-underline-offset: 2px;
}

.error {
  margin-top: var(--space-md);
  font-size: 14px;
  color: var(--color-danger);
}

.actions {
  margin-top: auto;
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

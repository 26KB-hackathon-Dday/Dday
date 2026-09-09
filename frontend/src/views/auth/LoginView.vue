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

    <!-- 폼을 지금 제목이 있는 자리까지 끌어올린다. 제목은 헤더 바로 아래로,
         입력 폼은 예전 제목 자리로 옮겨온다. -->
    <div class="center">
      <h1 class="title">다시 만나서<br />반가워요</h1>

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

        <div class="actions">
          <PrimaryButton type="submit" :disabled="!canSubmit" :loading="submitting">
            로그인
          </PrimaryButton>
        </div>
      </form>
    </div>
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

/* 세로 중앙 정렬을 쓰지 않고 header 바로 아래에서 시작한다 — 입력 폼이
   예전 제목 자리까지 올라오려면 이 블록 전체가 위쪽에 붙어야 한다. */
.center {
  display: flex;
  flex-direction: column;
  padding-top: var(--space-sm);
}

.title {
  margin-bottom: var(--space-lg);
  font-size: 30px;
  font-weight: 800;
  line-height: 1.3;
  letter-spacing: -0.02em;
  color: var(--color-primary);
  text-align: left;
}

.form {
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
  margin-top: var(--space-lg);
  padding-bottom: env(safe-area-inset-bottom);
}

/* 세로가 짧은 기기(SE 등)에서는 제목을 줄여 폼이 화면 밖으로 밀리지 않게 한다 */
@media (max-height: 640px) {
  .title {
    font-size: 24px;
  }
}
</style>

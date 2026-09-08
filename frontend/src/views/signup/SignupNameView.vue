<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useSignupStore } from '@/stores/signup'
import StepProgress from '@/components/signup/StepProgress.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import TextField from '@/components/common/TextField.vue'

const router = useRouter()
const signup = useSignupStore()

const canSubmit = computed(() => signup.name.trim().length > 0)

function next() {
  if (!canSubmit.value) return
  router.push('/signup/phone')
}
</script>

<template>
  <div class="page">
    <StepProgress :current-step="2" :total-steps="6" />

    <div class="content">
      <h1 class="title">성함을 입력해 주세요</h1>

      <div class="field">
        <TextField v-model="signup.name" placeholder="이름" autocomplete="name" @enter="next" />
        <p class="hint">실명 인증에 사용되므로 정확히 입력해 주세요.</p>
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
  padding-top: var(--space-lg);
}

.title {
  margin-bottom: var(--space-lg);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.hint {
  margin-top: var(--space-sm);
  padding-left: 2px;
  font-size: 13px;
  color: var(--color-secondary);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

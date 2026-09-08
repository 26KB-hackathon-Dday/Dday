<script setup lang="ts">
import { useRouter } from 'vue-router'
import StepProgress from '@/components/signup/StepProgress.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const router = useRouter()
</script>

<template>
  <div class="page">
    <StepProgress :current-step="1" :total-steps="6" />

    <div class="hero">
      <h1 class="title">회원가입을 시작할게요</h1>
      <p class="subtitle">간단한 본인 확인 후 서비스를 이용하실 수 있어요.</p>

      <!-- 실제 일러스트는 디자인 자산이 붙으면 교체한다. 지금은 방패 아이콘 자리만 잡는다. -->
      <div class="illustration">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M12 3l7 3v5c0 4.5-3 8.2-7 10-4-1.8-7-5.5-7-10V6l7-3Z" />
          <path d="M9.5 12.3l1.8 1.8 3.2-3.6" />
        </svg>
      </div>
    </div>

    <div class="actions">
      <PrimaryButton @click="router.push('/signup/name')">시작하기</PrimaryButton>
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

.hero {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  text-align: center;
}

.title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.subtitle {
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-secondary);
}

.illustration {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 140px;
  height: 140px;
  margin-top: var(--space-lg);
  border: 2px dashed var(--color-border);
  border-radius: 50%;
  color: var(--color-secondary);
}

.illustration svg {
  width: 56px;
  height: 56px;
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

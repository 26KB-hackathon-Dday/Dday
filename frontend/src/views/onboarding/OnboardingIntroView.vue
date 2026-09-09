<script setup lang="ts">
import { useRouter } from 'vue-router'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const router = useRouter()
</script>

<template>
  <div class="page">
    <div class="content">
      <p class="eyebrow">Onboarding</p>
      <h1 class="title">나에게 맞는 자립 계획을<br />만들어볼게요</h1>
      <p class="subtitle">
        보호종료일, 거주 지역, 생활 정보 몇 가지만 알려주시면<br />
        지금부터 자립까지 필요한 자금 계획을 함께 세워드려요.
      </p>
    </div>

    <div class="actions">
      <PrimaryButton @click="router.push('/onboarding/protection-date')">시작하기</PrimaryButton>
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
  justify-content: center;
}

.eyebrow {
  margin-bottom: var(--space-sm);
  font-size: 13px;
  font-weight: 700;
  color: var(--color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.title {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.subtitle {
  margin-top: var(--space-md);
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-secondary);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

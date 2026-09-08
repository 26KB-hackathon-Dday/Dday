<script setup lang="ts">
import { useRouter } from 'vue-router'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const router = useRouter()

const cards = [
  { title: '계좌 잔액', desc: '모든 계좌의 잔액을 한눈에' },
  { title: '거래 내역', desc: '입출금 내역을 자동으로 정리' },
  { title: '투자 수익률', desc: '투자 상품의 수익률까지 확인' },
]
</script>

<template>
  <div class="page">
    <header class="header">
      <button class="back" type="button" aria-label="뒤로" @click="router.back()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 5-7 7 7 7" />
        </svg>
      </button>
    </header>

    <div class="content">
      <h1 class="title">내 금융정보를 연결할게요</h1>
      <p class="subtitle">한 번에 모든 자산을 연결하여 편리하게 관리하세요.</p>

      <div class="cards">
        <div v-for="card in cards" :key="card.title" class="card">
          <span class="icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="6" width="18" height="13" rx="2.5" />
              <path d="M3 10h18" />
            </svg>
          </span>
          <span class="card-text">
            <strong>{{ card.title }}</strong>
            <span>{{ card.desc }}</span>
          </span>
        </div>
      </div>

      <p class="secure">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
          <rect x="4" y="10" width="16" height="10" rx="2.5" />
          <path d="M8 10V7.5a4 4 0 0 1 8 0V10" />
        </svg>
        안전하게 암호화되어 관리됩니다
      </p>
    </div>

    <div class="actions">
      <PrimaryButton @click="router.push('/mydata/select')">금융기관 선택하기</PrimaryButton>
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

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding-top: var(--space-md);
}

.title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.subtitle {
  margin-top: var(--space-sm);
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-secondary);
}

.cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  margin-top: var(--space-lg);
}

.card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: var(--space-md);
  text-align: left;
  background-color: var(--color-bg-soft);
  border-radius: var(--radius-md);
}

.icon {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background-color: var(--color-bg);
  border-radius: 50%;
  color: var(--color-primary);
}

.icon svg {
  width: 22px;
  height: 22px;
}

.card-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.card-text strong {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary);
}

.card-text span {
  font-size: 13px;
  color: var(--color-secondary);
}

.secure {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: var(--space-lg);
  font-size: 13px;
  color: var(--color-secondary);
}

.secure svg {
  width: 16px;
  height: 16px;
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

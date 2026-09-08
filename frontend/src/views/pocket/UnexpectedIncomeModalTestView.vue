<template>
  <div class="test-page">
    <main class="test-content">
      <h2 class="test-title">9월 계획</h2>

      <section class="dummy-card">
        <span>필수생활</span>
        <strong>1,000,000원</strong>
      </section>

      <section class="dummy-card">
        <span>자유생활</span>
        <strong>500,000원</strong>
      </section>

      <section class="dummy-card">
        <span>비상금</span>
        <strong>300,000원</strong>
      </section>

      <section class="dummy-card">
        <span>미래자산</span>
        <strong>700,000원</strong>
      </section>
    </main>

    <UnexpectedIncomeModal
      :open="showModal"
      :detected-amount="4_000_000"
      @add="handleAdd"
      @exclude="handleExclude"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import UnexpectedIncomeModal from '@/components/pocket/UnexpectedIncomeModal.vue'

const router = useRouter()

const showModal = ref(true)

const handleAdd = () => {
  showModal.value = false

  router.push({
    name: 'pocket-unexpected-income-amount',
  })
}

const handleExclude = () => {
  showModal.value = false

  console.log('이번 달 예산에 포함하지 않음')
}
</script>

<style scoped>
.test-page {
  width: 100%;
  min-height: 100vh;

  background: #ffffff;
}

.test-content {
  width: 100%;
  max-width: 430px;

  margin: 0 auto;

  padding: 28px;
}

.test-title {
  margin: 0 0 24px;

  font-size: 26px;
  font-weight: 700;
}

.dummy-card {
  display: flex;
  align-items: center;
  justify-content: space-between;

  margin-bottom: 14px;

  padding: 22px;

  border-radius: 14px;

  background: #f5f5f5;
}

.dummy-card span {
  font-size: 15px;
  font-weight: 600;
}

.dummy-card strong {
  font-size: 17px;
  font-weight: 700;
}
</style>

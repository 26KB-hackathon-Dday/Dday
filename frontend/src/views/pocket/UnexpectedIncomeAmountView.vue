<template>
  <div class="income-page">
    <main class="income-content">
      <section class="intro-section">
        <h2 class="intro-title">
          이번 달 예산에<br />
          얼마를 추가할까요?
        </h2>

        <p class="intro-subtitle">
          새로 들어온 돈
          {{ formatCurrency(store.detectedAmount) }}
        </p>
      </section>

      <section class="amount-section">
        <div class="amount-input-wrap">
          <input
            v-model="amountInput"
            class="amount-input"
            type="text"
            inputmode="numeric"
            placeholder="0"
            @input="handleAmountInput"
          />

          <span class="amount-unit"> 원 </span>
        </div>

        <div class="amount-divider" />

        <p class="excluded-description">
          나머지
          <strong>
            {{ formatCurrency(excludedPreview) }}
          </strong>
          은 이번 달 포켓 예산에 포함하지 않아요.
        </p>

        <p v-if="errorMessage" class="amount-error">
          {{ errorMessage }}
        </p>
      </section>

      <button class="next-button" type="button" @click="handleNext">
        {{ buttonAmountText }}을 예산에 추가하기
      </button>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useUnexpectedIncomeStore } from '@/stores/unexpectedIncome'

const router = useRouter()

const store = useUnexpectedIncomeStore()

const amountInput = ref(store.includedAmount.toLocaleString('ko-KR'))

const errorMessage = ref('')

const parsedAmount = computed(() => {
  const raw = amountInput.value.replace(/,/g, '')

  return Number(raw) || 0
})

const excludedPreview = computed(() => {
  return Math.max(store.detectedAmount - parsedAmount.value, 0)
})

const buttonAmountText = computed(() => {
  return parsedAmount.value.toLocaleString('ko-KR')
})

const handleAmountInput = (event: Event) => {
  const target = event.target as HTMLInputElement

  const onlyNumbers = target.value.replace(/[^0-9]/g, '')

  if (!onlyNumbers) {
    amountInput.value = ''
    return
  }

  amountInput.value = Number(onlyNumbers).toLocaleString('ko-KR')
}

const handleNext = () => {
  errorMessage.value = ''

  const amount = parsedAmount.value

  if (amount <= 0) {
    errorMessage.value = '추가할 금액을 입력해 주세요.'

    return
  }

  if (amount > store.detectedAmount) {
    errorMessage.value = '새로 들어온 금액보다 많이 추가할 수 없어요.'

    return
  }

  store.setIncludedAmount(amount)

  router.push({
    name: 'pocket-unexpected-income-allocate',
  })
}

const formatCurrency = (value: number) => {
  return `${value.toLocaleString('ko-KR')}원`
}
</script>

<style scoped>
.income-page {
  width: 100%;
  min-height: 100vh;

  background: #ffffff;
  color: #171717;
}

.income-content {
  display: flex;
  flex-direction: column;

  width: 100%;
  max-width: 430px;
  min-height: calc(100vh - 56px);

  margin: 0 auto;

  padding: 28px 28px 40px;
}

.intro-title {
  margin: 0;

  color: #171717;

  font-size: 27px;
  font-weight: 700;
  line-height: 1.35;

  letter-spacing: -0.8px;
}

.intro-subtitle {
  margin: 12px 0 0;

  color: #777777;

  font-size: 13px;
}

.amount-section {
  margin-top: 66px;
}

.amount-input-wrap {
  display: flex;
  align-items: center;

  gap: 8px;

  padding: 0 8px;
}

.amount-input {
  flex: 1;

  min-width: 0;

  border: 0;
  outline: none;

  color: #111111;
  background: transparent;

  text-align: right;

  font-size: 32px;
  font-weight: 800;

  letter-spacing: -1px;
}

.amount-unit {
  flex-shrink: 0;

  color: #171717;

  font-size: 19px;
  font-weight: 700;
}

.amount-divider {
  width: 100%;
  height: 1px;

  margin-top: 24px;

  background: #dddddd;
}

.excluded-description {
  margin: 20px 0 0;

  color: #777777;

  font-size: 12px;
  line-height: 1.6;
}

.excluded-description strong {
  color: #555555;
  font-weight: 600;
}

.amount-error {
  margin: 12px 0 0;

  color: #e05252;

  font-size: 12px;
  font-weight: 500;
}

.next-button {
  width: 100%;
  height: 60px;

  margin-top: auto;

  border: 0;
  border-radius: 10px;

  color: #ffffff;
  background: #111111;

  font-size: 15px;
  font-weight: 700;

  cursor: pointer;
}

.next-button:active {
  opacity: 0.85;
}
</style>

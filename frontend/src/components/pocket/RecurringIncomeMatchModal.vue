<template>
  <div v-if="open" class="modal-overlay">
    <!-- =========================
         고정수입으로 추정
    ========================== -->
    <section
      v-if="incomeType === 'RECURRING_LIKELY'"
      class="income-modal"
      role="dialog"
      aria-modal="true"
    >
      <h2 class="income-modal__title">고정수입으로 보여요</h2>

      <p class="income-modal__description">
        <strong>{{ senderName }}</strong
        >님에게
        <strong>{{ formatCurrency(amount) }}</strong>
        이 들어왔어요.
      </p>

      <div class="income-modal__registered">
        <span>등록한 고정수입</span>

        <strong>
          {{ recurringIncomeName || '고정수입' }}
          {{ formatCurrency(expectedAmount || 0) }}
        </strong>

        <small v-if="depositTiming">
          {{ depositTiming }}
        </small>
      </div>

      <div class="income-modal__notice">
        <p>이미 등록했던 고정 수입이라면 이번 달 예산에 포함하지 않아도 돼요.</p>
      </div>

      <p v-if="errorMessage" class="income-modal__error">
        {{ errorMessage }}
      </p>

      <div class="income-modal__buttons">
        <button
          class="income-modal__primary"
          type="button"
          :disabled="processing"
          @click="handleExclude"
        >
          {{ processing ? '처리 중...' : '이번 달 예산에 포함하지 않기' }}
        </button>

        <button
          class="income-modal__secondary"
          type="button"
          :disabled="processing"
          @click="handleNotRecurring"
        >
          고정수입이 아니에요
        </button>
      </div>
    </section>

    <!-- =========================
         고정수입보다 초과 입금
    ========================== -->
    <section
      v-else-if="incomeType === 'RECURRING_OVER'"
      class="income-modal"
      role="dialog"
      aria-modal="true"
    >
      <h2 class="income-modal__title">고정수입보다 돈이 더 들어왔어요</h2>

      <p class="income-modal__description">
        <strong>{{ senderName }}</strong
        >님에게
        <strong>{{ formatCurrency(amount) }}</strong>
        이 들어왔어요.
      </p>

      <div class="income-modal__registered">
        <span>
          {{ recurringIncomeName || '등록한 고정수입' }}
        </span>

        <strong> 예상 {{ formatCurrency(expectedAmount || 0) }} </strong>

        <small v-if="depositTiming">
          {{ depositTiming }}
        </small>
      </div>

      <div class="income-modal__notice">
        <p>
          고정수입으로 추정되지만,
          <strong>{{ formatCurrency(excessAmount) }}</strong>
          이 더 들어온 것으로 보여요.
        </p>
      </div>

      <p v-if="errorMessage" class="income-modal__error">
        {{ errorMessage }}
      </p>

      <div class="income-modal__buttons">
        <button
          class="income-modal__primary"
          type="button"
          :disabled="processing"
          @click="handleAddExcess"
        >
          {{ formatCurrency(excessAmount) }}만 포켓에 추가하기
        </button>

        <button
          class="income-modal__secondary"
          type="button"
          :disabled="processing"
          @click="handleExclude"
        >
          이번 달 예산에 포함하지 않기
        </button>

        <button
          class="income-modal__text-button"
          type="button"
          :disabled="processing"
          @click="handleNotRecurring"
        >
          고정수입이 아니에요
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

import type { UnexpectedIncomeType } from '@/api/unexpectedIncome'

interface Props {
  open: boolean

  incomeType: UnexpectedIncomeType

  senderName: string

  amount: number

  recurringIncomeName: string | null

  expectedAmount: number | null

  depositTiming: string | null

  excessAmount: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  exclude: []
  notRecurring: []
  addExcess: []
}>()

const processing = ref(false)

const errorMessage = ref('')

watch(
  () => props.open,
  (open) => {
    if (open) {
      processing.value = false

      errorMessage.value = ''
    }
  },
)

const handleExclude = () => {
  if (processing.value) {
    return
  }

  emit('exclude')
}

const handleNotRecurring = () => {
  if (processing.value) {
    return
  }

  emit('notRecurring')
}

const handleAddExcess = () => {
  if (processing.value) {
    return
  }

  emit('addExcess')
}

const formatCurrency = (value: number) => {
  return `${Math.max(value, 0).toLocaleString('ko-KR')}원`
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;

  display: flex;
  align-items: flex-end;
  justify-content: center;

  padding: 0 14px 82px;

  background: rgba(0, 0, 0, 0.38);
}

.income-modal {
  width: 100%;
  max-width: 402px;

  padding: 24px 22px 22px;

  border-radius: 14px;

  background: #ffffff;

  box-shadow: 0 14px 40px rgba(0, 0, 0, 0.12);
}

.income-modal__title {
  margin: 0;

  color: #171717;

  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;

  letter-spacing: -0.4px;
}

.income-modal__description {
  margin: 14px 0 0;

  color: #777777;

  font-size: 13px;
  line-height: 1.65;
}

.income-modal__description strong {
  color: #444444;

  font-weight: 700;
}

.income-modal__registered {
  display: flex;
  flex-direction: column;

  gap: 4px;

  margin-top: 18px;
  padding: 14px;

  border-radius: 9px;

  background: #fafafa;
}

.income-modal__registered span {
  color: #888888;

  font-size: 11px;
}

.income-modal__registered strong {
  color: #333333;

  font-size: 14px;
  font-weight: 700;
}

.income-modal__registered small {
  color: #888888;

  font-size: 11px;
}

.income-modal__notice {
  margin-top: 14px;

  padding: 13px 14px;

  border-radius: 8px;

  background: #f6f6f7;
}

.income-modal__notice p {
  margin: 0;

  color: #777777;

  font-size: 12px;
  line-height: 1.6;
}

.income-modal__notice strong {
  color: #444444;
}

.income-modal__error {
  margin: 12px 0 0;

  color: #e05252;

  font-size: 12px;
}

.income-modal__buttons {
  display: flex;
  flex-direction: column;

  gap: 10px;

  margin-top: 22px;
}

.income-modal__primary,
.income-modal__secondary {
  width: 100%;
  min-height: 54px;

  padding: 0 14px;

  border: 0;
  border-radius: 10px;

  font-size: 14px;
  font-weight: 700;

  cursor: pointer;
}

.income-modal__primary {
  color: #ffffff;

  background: #111111;
}

.income-modal__secondary {
  color: #555555;

  background: #f3f3f5;
}

.income-modal__text-button {
  width: 100%;

  padding: 10px 0;

  border: 0;

  color: #888888;
  background: transparent;

  font-size: 13px;
  font-weight: 600;

  cursor: pointer;
}

.income-modal__primary:disabled,
.income-modal__secondary:disabled,
.income-modal__text-button:disabled {
  opacity: 0.55;

  cursor: default;
}
</style>

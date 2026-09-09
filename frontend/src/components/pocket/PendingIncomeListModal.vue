<template>
  <div v-if="open" class="modal-overlay">
    <section
      class="pending-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="pending-income-title"
    >
      <div class="pending-modal__header">
        <div>
          <p class="pending-modal__eyebrow">새 입금 확인</p>

          <h2 id="pending-income-title" class="pending-modal__title">
            새로 들어온 돈이
            {{ incomes.length }}건 있어요
          </h2>
        </div>
      </div>

      <div class="pending-modal__total">
        <span>총 입금액</span>

        <strong>
          {{ formatCurrency(totalAmount) }}
        </strong>
      </div>

      <div class="pending-modal__list">
        <div v-for="income in incomes" :key="income.transactionId" class="pending-modal__item">
          <div class="pending-modal__item-info">
            <strong>
              {{ income.senderName || '알 수 없는 입금자' }}
            </strong>

            <span>
              {{ formatDate(income.transactionAt) }}
            </span>
          </div>

          <strong class="pending-modal__item-amount"> +{{ formatCurrency(income.amount) }} </strong>
        </div>
      </div>

      <p class="pending-modal__description">
        각 입금마다 이번 달 예산에 포함할지 하나씩 확인할 수 있어요.
      </p>

      <button class="pending-modal__button" type="button" @click="emit('confirm')">
        하나씩 확인하기
      </button>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { UnexpectedIncome } from '@/api/unexpectedIncome'

interface Props {
  open: boolean
  incomes: UnexpectedIncome[]
  totalAmount: number
}

defineProps<Props>()

const emit = defineEmits<{
  confirm: []
}>()

const formatCurrency = (value: number) => {
  return `${Number(value ?? 0).toLocaleString('ko-KR')}원`
}

const formatDate = (value: string) => {
  if (!value) {
    return ''
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return ''
  }

  return `${date.getMonth() + 1}월 ${date.getDate()}일`
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 20px;

  background: rgba(0, 0, 0, 0.38);
}

.pending-modal {
  width: 100%;
  max-width: 374px;

  padding: 26px 22px 22px;

  border-radius: 16px;

  background: #ffffff;

  box-shadow: 0 14px 40px rgba(0, 0, 0, 0.14);
}

.pending-modal__header {
  display: flex;
  flex-direction: column;
}

.pending-modal__eyebrow {
  margin: 0 0 6px;

  color: #888888;

  font-size: 12px;
  font-weight: 600;
}

.pending-modal__title {
  margin: 0;

  color: #171717;

  font-size: 20px;
  font-weight: 800;
  line-height: 1.4;

  letter-spacing: -0.5px;
}

.pending-modal__total {
  display: flex;
  align-items: center;
  justify-content: space-between;

  margin-top: 20px;
  padding: 16px;

  border-radius: 12px;

  background: #f6f7f9;
}

.pending-modal__total span {
  color: #777777;

  font-size: 13px;
}

.pending-modal__total strong {
  color: #171717;

  font-size: 18px;
  font-weight: 800;
}

.pending-modal__list {
  display: flex;
  flex-direction: column;

  max-height: 230px;

  margin-top: 12px;

  overflow-y: auto;
}

.pending-modal__item {
  display: flex;
  align-items: center;
  justify-content: space-between;

  gap: 16px;

  min-height: 62px;

  border-bottom: 1px solid #eeeeee;
}

.pending-modal__item:last-child {
  border-bottom: 0;
}

.pending-modal__item-info {
  display: flex;
  flex-direction: column;

  min-width: 0;

  gap: 4px;
}

.pending-modal__item-info strong {
  overflow: hidden;

  color: #333333;

  font-size: 14px;
  font-weight: 700;

  text-overflow: ellipsis;
  white-space: nowrap;
}

.pending-modal__item-info span {
  color: #999999;

  font-size: 11px;
}

.pending-modal__item-amount {
  flex-shrink: 0;

  color: #356df3;

  font-size: 14px;
  font-weight: 800;
}

.pending-modal__description {
  margin: 16px 0 0;

  color: #777777;

  font-size: 12px;
  line-height: 1.6;
}

.pending-modal__button {
  width: 100%;
  height: 54px;

  margin-top: 20px;

  border: 0;
  border-radius: 10px;

  color: #ffffff;
  background: #111111;

  font-size: 15px;
  font-weight: 700;

  cursor: pointer;
}

.pending-modal__button:active {
  opacity: 0.85;
}
</style>

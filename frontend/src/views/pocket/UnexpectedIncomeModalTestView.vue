<template>
  <div v-if="open" class="modal-overlay" @click.self="handleClose">
    <section
      class="income-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="unexpected-income-title"
    >
      <div class="income-modal__header">
        <h2 id="unexpected-income-title" class="income-modal__title">새로 들어온 돈</h2>

        <strong class="income-modal__amount">
          {{ formatCurrency(detectedAmount) }}
        </strong>
      </div>

      <p class="income-modal__sender">
        <strong>{{ senderName }}</strong
        >님에게 들어온 돈이에요.
      </p>

      <div class="income-modal__notice">
        <p>정기수입이라면 이번 달 예산에 포함하지 않아도 됩니다.</p>
      </div>

      <div class="income-modal__buttons">
        <button class="income-modal__primary" type="button" @click="handleAdd">
          포켓에 추가하기
        </button>

        <button class="income-modal__secondary" type="button" @click="handleExclude">
          이번 달 예산에 포함하지 않기
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
interface Props {
  open: boolean
  detectedAmount: number
  senderName: string
}

defineProps<Props>()

const emit = defineEmits<{
  close: []
  add: []
  exclude: []
}>()

const handleClose = () => {
  emit('close')
}

const handleAdd = () => {
  emit('add')
}

const handleExclude = () => {
  emit('exclude')
}

const formatCurrency = (value: number) => {
  return `${value.toLocaleString('ko-KR')}원`
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

.income-modal {
  width: 100%;
  max-width: 374px;

  padding: 28px 24px 24px;

  border-radius: 16px;

  background: #ffffff;

  box-shadow: 0 14px 40px rgba(0, 0, 0, 0.14);
}

.income-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  gap: 12px;
}

.income-modal__title {
  margin: 0;

  color: #171717;

  font-size: 18px;
  font-weight: 700;
}

.income-modal__amount {
  flex-shrink: 0;

  color: #356df3;

  font-size: 19px;
  font-weight: 800;

  letter-spacing: -0.5px;
}

.income-modal__sender {
  margin: 16px 0 0;

  color: #666666;

  font-size: 14px;
  line-height: 1.65;
}

.income-modal__sender strong {
  color: #333333;

  font-weight: 700;
}

.income-modal__notice {
  margin-top: 18px;

  padding: 14px;

  border-radius: 8px;

  background: #f6f6f7;
}

.income-modal__notice p {
  margin: 0;

  color: #777777;

  font-size: 12px;
  line-height: 1.6;
}

.income-modal__buttons {
  display: flex;
  flex-direction: column;

  gap: 10px;

  margin-top: 24px;
}

.income-modal__primary,
.income-modal__secondary {
  width: 100%;
  height: 54px;

  border: 0;
  border-radius: 10px;

  font-size: 15px;
  font-weight: 700;

  cursor: pointer;
}

.income-modal__primary {
  color: #ffffff;
  background: #111111;
}

.income-modal__secondary {
  color: #666666;
  background: #f5f5f7;
}

.income-modal__primary:active,
.income-modal__secondary:active {
  opacity: 0.85;
}
</style>

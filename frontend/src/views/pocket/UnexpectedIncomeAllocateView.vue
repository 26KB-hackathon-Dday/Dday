<template>
  <div class="allocate-page">
    <main class="allocate-content">
      <section class="intro-section">
        <h2 class="intro-title">
          {{ formatCurrency(store.includedAmount) }}
          <br />
          포켓에 배분하기
        </h2>

        <p class="intro-description">이 돈을 어디에 사용할까요?</p>
      </section>

      <section class="pocket-list">
        <div v-for="pocket in pockets" :key="pocket.type" class="pocket-row">
          <div class="pocket-info">
            <span class="pocket-dot" :class="`pocket-dot--${pocket.type}`" />

            <span class="pocket-name">
              {{ pocket.label }}
            </span>
          </div>

          <div class="pocket-input-wrap">
            <input
              class="pocket-input"
              type="text"
              inputmode="numeric"
              :value="formatInputValue(pocket.amount)"
              @input="handlePocketInput(pocket.type, $event)"
            />

            <span class="pocket-unit"> 원 </span>
          </div>
        </div>
      </section>

      <section class="allocation-status" :class="statusClass">
        <div class="allocation-status__row">
          <span> 남은 금액 </span>

          <strong>
            {{ formatCurrency(store.remainingAmount) }}
          </strong>
        </div>

        <p v-if="store.remainingAmount > 0" class="allocation-message">
          남은 금액을 포켓에 더 배분해 주세요.
        </p>

        <p
          v-else-if="store.remainingAmount < 0"
          class="allocation-message allocation-message--error"
        >
          {{ formatCurrency(Math.abs(store.remainingAmount)) }}
          만큼 초과했어요.
        </p>

        <p v-else class="allocation-message allocation-message--success">
          추가할 금액을 모두 배분했어요.
        </p>

        <p v-if="errorMessage" class="allocation-message allocation-message--error">
          {{ errorMessage }}
        </p>
      </section>

      <button
        class="confirm-button"
        :class="{
          'confirm-button--disabled': !store.isAllocationComplete || saving,
        }"
        type="button"
        :disabled="!store.isAllocationComplete || saving"
        @click="handleConfirm"
      >
        {{ saving ? '반영 중...' : '배분 확정' }}
      </button>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { unexpectedIncomeApi } from '@/api/unexpectedIncome'

import { useUnexpectedIncomeStore, type IncomePocketType } from '@/stores/unexpectedIncome'

const router = useRouter()

const store = useUnexpectedIncomeStore()

const saving = ref(false)

const errorMessage = ref('')

const pockets = computed(() => [
  {
    type: 'essential' as IncomePocketType,

    label: '필수 포켓',

    amount: store.essentialAmount,
  },

  {
    type: 'free' as IncomePocketType,

    label: '자유 포켓',

    amount: store.freeAmount,
  },

  {
    type: 'future' as IncomePocketType,

    label: '미래자산 포켓',

    amount: store.futureAmount,
  },

  {
    type: 'emergency' as IncomePocketType,

    label: '비상금 포켓',

    amount: store.emergencyAmount,
  },
])

const statusClass = computed(() => {
  if (store.remainingAmount < 0) {
    return 'allocation-status--error'
  }

  if (store.remainingAmount === 0) {
    return 'allocation-status--success'
  }

  return ''
})

const handlePocketInput = (type: IncomePocketType, event: Event) => {
  const target = event.target as HTMLInputElement

  const onlyNumbers = target.value.replace(/[^0-9]/g, '')

  const value = Number(onlyNumbers) || 0

  store.setPocketAmount(type, value)

  /*
   * 입력 직후 천 단위 콤마 표시.
   */
  target.value = formatInputValue(value)

  errorMessage.value = ''
}

const formatInputValue = (value: number) => {
  if (value === 0) {
    return '0'
  }

  return value.toLocaleString('ko-KR')
}

const formatCurrency = (value: number) => {
  const prefix = value < 0 ? '-' : ''

  return `${prefix}${Math.abs(Number(value ?? 0)).toLocaleString('ko-KR')}원`
}

const handleConfirm = async () => {
  errorMessage.value = ''

  if (!store.isAllocationComplete || saving.value) {
    return
  }

  if (store.detectedTransactionId == null) {
    errorMessage.value = '처리할 입금 정보를 찾을 수 없어요.'

    return
  }

  /*
   * 포켓에 배분한 총액 계산.
   */
  const allocationTotal =
    store.essentialAmount + store.freeAmount + store.futureAmount + store.emergencyAmount

  /*
   * 선택한 추가 금액과
   * 포켓별 배분 합계가 정확히 같아야 한다.
   */
  if (allocationTotal !== store.includedAmount) {
    errorMessage.value = '포켓 배분 금액과 추가할 금액이 일치하지 않아요.'

    return
  }

  saving.value = true

  try {
    await unexpectedIncomeApi.addToBudget(store.detectedTransactionId, {
      addAmount: store.includedAmount,

      allocations: [
        {
          pocketType: 'ESSENTIAL',

          amount: store.essentialAmount,
        },

        {
          pocketType: 'FREE',

          amount: store.freeAmount,
        },

        {
          pocketType: 'FUTURE_ASSET',

          amount: store.futureAmount,
        },

        {
          pocketType: 'EMERGENCY',

          amount: store.emergencyAmount,
        },
      ],
    })

    /*
     * 중요:
     *
     * 예전에는 여기서 resetAll()을 해서
     * pending queue 전체가 날아갔다.
     *
     * 이제는 방금 처리한 거래만 제거한다.
     */
    store.removeCurrentIncome()

    /*
     * 현재 거래용 입력값만 초기화.
     *
     * 아직 남은 pending 거래는 그대로 유지된다.
     */
    store.resetDetectedIncome()

    /*
     * PocketView로 돌아가면
     * 남아있는 queue가 있을 경우
     * 서버를 다시 조회하지 않고
     * 다음 거래 모달을 바로 띄운다.
     */
    await router.push({
      name: 'pockets',
    })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '포켓 예산에 반영하지 못했어요.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.allocate-page {
  width: 100%;

  min-height: 100vh;

  background: #ffffff;

  color: #171717;
}

.allocate-content {
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

  font-size: 27px;

  font-weight: 700;

  line-height: 1.35;

  letter-spacing: -0.8px;
}

.intro-description {
  margin: 26px 0 0;

  color: #777777;

  font-size: 14px;
}

.pocket-list {
  display: flex;

  flex-direction: column;

  gap: 12px;

  margin-top: 28px;
}

.pocket-row {
  display: flex;

  align-items: center;

  justify-content: space-between;

  min-height: 64px;

  padding: 0 16px;

  border: 1px solid #e1e1e1;

  border-radius: 10px;

  background: #ffffff;
}

.pocket-info {
  display: flex;

  align-items: center;

  gap: 10px;
}

.pocket-dot {
  width: 10px;

  height: 10px;

  border-radius: 50%;
}

.pocket-dot--essential {
  background: #397bc7;
}

.pocket-dot--free {
  background: #ef8e3d;
}

.pocket-dot--future {
  background: #6ab78f;
}

.pocket-dot--emergency {
  background: #22a9ad;
}

.pocket-name {
  color: #171717;

  font-size: 14px;

  font-weight: 700;
}

.pocket-input-wrap {
  display: flex;

  align-items: center;

  max-width: 145px;
}

.pocket-input {
  width: 100%;

  border: 0;

  outline: none;

  color: #171717;

  background: transparent;

  text-align: right;

  font-size: 15px;

  font-weight: 700;
}

.pocket-unit {
  margin-left: 4px;

  color: #777777;

  font-size: 13px;
}

.allocation-status {
  display: flex;

  flex-direction: column;

  gap: 8px;

  margin-top: auto;

  padding-top: 32px;
}

.allocation-status__row {
  display: flex;

  align-items: center;

  justify-content: space-between;

  color: #777777;

  font-size: 14px;
}

.allocation-status__row strong {
  color: #171717;

  font-size: 18px;

  font-weight: 700;
}

.allocation-message {
  margin: 0;

  color: #777777;

  font-size: 12px;
}

.allocation-message--error {
  color: #e05252;
}

.allocation-message--success {
  color: #15936f;
}

.confirm-button {
  width: 100%;

  height: 60px;

  margin-top: 22px;

  border: 0;

  border-radius: 10px;

  color: #ffffff;

  background: #111111;

  font-size: 15px;

  font-weight: 700;

  cursor: pointer;
}

.confirm-button--disabled {
  color: #999999;

  background: #e7e7e7;

  cursor: not-allowed;
}

.confirm-button:active:not(:disabled) {
  opacity: 0.85;
}
</style>

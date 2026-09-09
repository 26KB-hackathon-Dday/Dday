import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import type { UnexpectedIncome, UnexpectedIncomeType } from '@/api/unexpectedIncome'

export type IncomePocketType = 'essential' | 'free' | 'future' | 'emergency'

export const useUnexpectedIncomeStore = defineStore('unexpectedIncome', () => {
  /*
   * 현재 처리 중인 입금 거래.
   */
  const detectedTransactionId = ref<number | null>(null)

  const detectedAmount = ref(0)

  const senderName = ref<string | null>(null)

  const transactionAt = ref<string | null>(null)

  const incomeType = ref<UnexpectedIncomeType>('NEW_INCOME')

  /*
   * 정기수입 추정 정보.
   */
  const recurringIncomeId = ref<number | null>(null)

  const recurringIncomeName = ref<string | null>(null)

  const expectedAmount = ref<number | null>(null)

  const depositTiming = ref<string | null>(null)

  const excessAmount = ref(0)

  /*
   * 실제 입금액 중
   * 이번 달 포켓 예산에 추가할 금액.
   */
  const includedAmount = ref(0)

  /*
   * 포켓별 추가 배분액.
   */
  const essentialAmount = ref(0)

  const freeAmount = ref(0)

  const futureAmount = ref(0)

  const emergencyAmount = ref(0)

  /*
   * 이번 달 예산에 넣지 않는 금액.
   */
  const excludedAmount = computed(() => {
    return Math.max(detectedAmount.value - includedAmount.value, 0)
  })

  /*
   * 현재 네 포켓에 배분한 총액.
   */
  const allocatedAmount = computed(() => {
    return essentialAmount.value + freeAmount.value + futureAmount.value + emergencyAmount.value
  })

  /*
   * 아직 배분해야 할 금액.
   */
  const remainingAmount = computed(() => {
    return includedAmount.value - allocatedAmount.value
  })

  const isAllocationComplete = computed(() => {
    return remainingAmount.value === 0
  })

  /*
   * 백엔드에서 조회한 입금 거래 저장.
   */
  const setDetectedIncome = (income: UnexpectedIncome) => {
    detectedTransactionId.value = income.transactionId

    detectedAmount.value = income.amount

    senderName.value = income.senderName

    transactionAt.value = income.transactionAt

    incomeType.value = income.type

    recurringIncomeId.value = income.recurringIncomeId

    recurringIncomeName.value = income.recurringIncomeName

    expectedAmount.value = income.expectedAmount

    depositTiming.value = income.depositTiming

    excessAmount.value = income.excessAmount ?? 0

    includedAmount.value = 0

    resetAllocation()
  }

  /*
   * RECURRING_LIKELY / RECURRING_OVER에서
   * "고정수입이 아니에요" 선택 시
   * 같은 거래를 일반 새 입금으로 전환.
   *
   * 백엔드 상태는 아직 바꾸지 않는다.
   */
  const convertToNewIncome = () => {
    incomeType.value = 'NEW_INCOME'

    recurringIncomeId.value = null
    recurringIncomeName.value = null
    expectedAmount.value = null
    depositTiming.value = null
    excessAmount.value = 0

    includedAmount.value = 0

    resetAllocation()
  }

  /*
   * 실제 입금액 중
   * 예산에 추가할 금액 지정.
   */
  const setIncludedAmount = (amount: number) => {
    const normalized = Math.max(0, Math.min(amount, detectedAmount.value))

    includedAmount.value = normalized

    resetAllocation()
  }

  const getPocketAmount = (type: IncomePocketType) => {
    switch (type) {
      case 'essential':
        return essentialAmount.value

      case 'free':
        return freeAmount.value

      case 'future':
        return futureAmount.value

      case 'emergency':
        return emergencyAmount.value
    }
  }

  const setPocketAmount = (type: IncomePocketType, amount: number) => {
    const normalized = Math.max(0, amount)

    switch (type) {
      case 'essential':
        essentialAmount.value = normalized
        break

      case 'free':
        freeAmount.value = normalized
        break

      case 'future':
        futureAmount.value = normalized
        break

      case 'emergency':
        emergencyAmount.value = normalized
        break
    }
  }

  const resetAllocation = () => {
    essentialAmount.value = 0
    freeAmount.value = 0
    futureAmount.value = 0
    emergencyAmount.value = 0
  }

  const resetAll = () => {
    detectedTransactionId.value = null

    detectedAmount.value = 0

    senderName.value = null

    transactionAt.value = null

    incomeType.value = 'NEW_INCOME'

    recurringIncomeId.value = null
    recurringIncomeName.value = null
    expectedAmount.value = null
    depositTiming.value = null
    excessAmount.value = 0

    includedAmount.value = 0

    resetAllocation()
  }

  return {
    detectedTransactionId,
    detectedAmount,
    senderName,
    transactionAt,
    incomeType,

    recurringIncomeId,
    recurringIncomeName,
    expectedAmount,
    depositTiming,
    excessAmount,

    includedAmount,
    excludedAmount,

    essentialAmount,
    freeAmount,
    futureAmount,
    emergencyAmount,

    allocatedAmount,
    remainingAmount,
    isAllocationComplete,

    setDetectedIncome,
    convertToNewIncome,

    setIncludedAmount,

    getPocketAmount,
    setPocketAmount,

    resetAllocation,
    resetAll,
  }
})

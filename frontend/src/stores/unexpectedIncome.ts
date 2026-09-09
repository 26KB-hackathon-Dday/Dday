import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import type { UnexpectedIncome, UnexpectedIncomeType } from '@/api/unexpectedIncome'

export type IncomePocketType = 'essential' | 'free' | 'future' | 'emergency'

export const useUnexpectedIncomeStore = defineStore('unexpectedIncome', () => {
  /*
   * =========================
   * 미처리 입금 Queue
   * =========================
   */

  const pendingIncomes = ref<UnexpectedIncome[]>([])

  const currentIncomeIndex = ref(0)

  const hasPendingIncomes = computed(() => {
    return pendingIncomes.value.length > 0
  })

  const pendingCount = computed(() => {
    return pendingIncomes.value.length
  })

  const currentPosition = computed(() => {
    if (!hasPendingIncomes.value) {
      return 0
    }

    return currentIncomeIndex.value + 1
  })

  const currentIncome = computed<UnexpectedIncome | null>(() => {
    return pendingIncomes.value[currentIncomeIndex.value] ?? null
  })

  const remainingIncomeCount = computed(() => {
    if (!hasPendingIncomes.value) {
      return 0
    }

    return Math.max(pendingIncomes.value.length - currentIncomeIndex.value - 1, 0)
  })

  const totalPendingAmount = computed(() => {
    return pendingIncomes.value.reduce((sum, income) => sum + income.amount, 0)
  })

  const setPendingIncomes = (incomes: UnexpectedIncome[]) => {
    pendingIncomes.value = [...incomes]
    currentIncomeIndex.value = 0
  }

  /*
   * 현재 거래를 Queue에서 제거한다.
   *
   * 처리 완료된 거래가 빠지기 때문에
   * index는 그대로 두면 다음 거래가
   * 같은 index 위치로 올라온다.
   */
  const removeCurrentIncome = () => {
    if (!hasPendingIncomes.value) {
      return
    }

    pendingIncomes.value.splice(currentIncomeIndex.value, 1)

    if (currentIncomeIndex.value >= pendingIncomes.value.length) {
      currentIncomeIndex.value = Math.max(pendingIncomes.value.length - 1, 0)
    }
  }

  const clearPendingIncomes = () => {
    pendingIncomes.value = []
    currentIncomeIndex.value = 0
  }

  /*
   * =========================
   * 현재 처리 중인 입금
   * =========================
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
   * Queue의 현재 거래를
   * 처리 대상으로 세팅한다.
   */
  const setCurrentPendingIncome = () => {
    if (!currentIncome.value) {
      resetDetectedIncome()
      return
    }

    setDetectedIncome(currentIncome.value)
  }

  /*
   * RECURRING_LIKELY / RECURRING_OVER에서
   * "고정수입이 아니에요" 선택 시
   * 같은 거래를 일반 새 입금으로 전환.
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

  /*
   * 현재 처리 중인 거래 정보만 초기화한다.
   * Queue는 유지한다.
   */
  const resetDetectedIncome = () => {
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

  /*
   * Queue까지 전부 초기화.
   */
  const resetAll = () => {
    clearPendingIncomes()
    resetDetectedIncome()
  }

  return {
    /*
     * Queue
     */
    pendingIncomes,
    currentIncomeIndex,

    hasPendingIncomes,
    pendingCount,
    currentPosition,
    currentIncome,
    remainingIncomeCount,
    totalPendingAmount,

    setPendingIncomes,
    removeCurrentIncome,
    clearPendingIncomes,
    setCurrentPendingIncome,

    /*
     * 현재 거래
     */
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
    resetDetectedIncome,
    resetAll,
  }
})

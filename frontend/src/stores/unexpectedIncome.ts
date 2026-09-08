import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export type IncomePocketType = 'essential' | 'free' | 'future' | 'emergency'

export const useUnexpectedIncomeStore = defineStore('unexpectedIncome', () => {
  /*
   * 새로 감지된 입금액
   *
   * TODO:
   * 실제로는 백엔드 / MyData 응답값 사용
   */
  const detectedAmount = ref(4_000_000)

  /*
   * 새 입금 거래 식별값
   *
   * TODO:
   * 실제 transactionId 사용
   */
  const detectedTransactionId = ref<string | null>(null)

  /*
   * 이번 달 포켓 예산에
   * 실제 포함할 금액
   */
  const includedAmount = ref(500_000)

  /*
   * 포켓별 추가 배분
   */
  const essentialAmount = ref(0)
  const freeAmount = ref(0)
  const futureAmount = ref(100_000)
  const emergencyAmount = ref(400_000)

  /*
   * 이번 달 포켓 예산에
   * 포함하지 않는 금액
   */
  const excludedAmount = computed(() => {
    return Math.max(detectedAmount.value - includedAmount.value, 0)
  })

  /*
   * 현재 배분한 금액 합계
   */
  const allocatedAmount = computed(() => {
    return essentialAmount.value + freeAmount.value + futureAmount.value + emergencyAmount.value
  })

  /*
   * 아직 배분해야 할 금액
   *
   * 양수 = 남음
   * 0 = 배분 완료
   * 음수 = 초과
   */
  const remainingAmount = computed(() => {
    return includedAmount.value - allocatedAmount.value
  })

  const isAllocationComplete = computed(() => {
    return remainingAmount.value === 0
  })

  /*
   * 새로운 예상 밖 입금을
   * 현재 플로우의 대상 거래로 등록
   */
  const setDetectedIncome = (amount: number, transactionId?: string) => {
    detectedAmount.value = Math.max(0, amount)

    detectedTransactionId.value = transactionId ?? null

    /*
     * 기본적으로 전체 금액을
     * 이번 달에 포함시키지는 않는다.
     */
    includedAmount.value = 0

    resetAllocation()
  }

  /*
   * 이번 달 예산에 넣을 금액 지정
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
    detectedAmount.value = 4_000_000

    detectedTransactionId.value = null

    includedAmount.value = 500_000

    essentialAmount.value = 0
    freeAmount.value = 0
    futureAmount.value = 100_000
    emergencyAmount.value = 400_000
  }

  return {
    detectedAmount,
    detectedTransactionId,

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
    setIncludedAmount,

    getPocketAmount,
    setPocketAmount,

    resetAllocation,
    resetAll,
  }
})

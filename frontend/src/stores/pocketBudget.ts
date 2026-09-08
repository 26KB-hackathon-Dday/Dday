import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export type PocketType = 'essential' | 'free' | 'future' | 'emergency'

export const usePocketBudgetStore = defineStore('pocketBudget', () => {
  // =========================
  // 이번 달 총 예산
  // =========================

  const totalBudget = ref(2_500_000)

  // =========================
  // 포켓별 예산
  // =========================

  const essentialBudget = ref(1_050_000)
  const freeBudget = ref(650_000)
  const futureBudget = ref(500_000)
  const emergencyBudget = ref(300_000)

  // =========================
  // 예상 자산
  // TODO: 추후 백엔드 API 연동
  // =========================

  const previousAsset = ref(24_000_000)

  const expectedAsset = computed(() => {
    return Math.round(previousAsset.value + futureBudget.value * 14.4)
  })

  const difference = computed(() => {
    return expectedAsset.value - previousAsset.value
  })

  // =========================
  // 현재 포켓 배분 합계
  // =========================

  const allocatedTotal = computed(() => {
    return essentialBudget.value + freeBudget.value + futureBudget.value + emergencyBudget.value
  })

  /*
   * 양수:
   * 총 예산보다 많이 배분됨
   *
   * 음수:
   * 아직 배분할 돈이 남음
   *
   * 0:
   * 정확히 맞음
   */
  const budgetGap = computed(() => {
    return allocatedTotal.value - totalBudget.value
  })

  const isBudgetBalanced = computed(() => {
    return budgetGap.value === 0
  })

  // =========================
  // 포켓 값 조회
  // =========================

  const getPocketValue = (type: PocketType): number => {
    switch (type) {
      case 'essential':
        return essentialBudget.value

      case 'free':
        return freeBudget.value

      case 'future':
        return futureBudget.value

      case 'emergency':
        return emergencyBudget.value
    }
  }

  // =========================
  // 포켓 값 변경
  // =========================

  const setPocketValue = (type: PocketType, value: number) => {
    const normalizedValue = Math.max(0, value)

    switch (type) {
      case 'essential':
        essentialBudget.value = normalizedValue
        break

      case 'free':
        freeBudget.value = normalizedValue
        break

      case 'future':
        futureBudget.value = normalizedValue
        break

      case 'emergency':
        emergencyBudget.value = normalizedValue
        break
    }
  }

  // =========================
  // 사용자가 슬라이더 직접 조정
  // =========================

  const updatePocket = (type: PocketType, value: number) => {
    /*
     * 개별 포켓 하나가
     * 총 예산보다 클 수는 없도록 제한
     */
    const normalizedValue = Math.max(0, Math.min(value, totalBudget.value))

    setPocketValue(type, normalizedValue)
  }

  // =========================
  // 총 예산 등록
  // =========================

  const setTotalBudget = (newTotal: number) => {
    if (!Number.isFinite(newTotal) || newTotal <= 0) {
      return
    }

    /*
     * 총예산만 변경한다.
     *
     * 기존 포켓 금액은 그대로 두고
     * 사용자가 직접 조정하거나
     * 자동으로 맞추기를 누르도록 한다.
     */
    totalBudget.value = newTotal
  }

  // =========================
  // 나머지 포켓 목록
  // =========================

  const getOtherTypes = (fixedType: PocketType): PocketType[] => {
    const allTypes: PocketType[] = ['essential', 'free', 'future', 'emergency']

    return allTypes.filter((type) => type !== fixedType)
  }

  // =========================
  // 자동으로 맞추기
  // =========================

  const autoBalance = (fixedType: PocketType) => {
    /*
     * 마지막으로 사용자가 만진 포켓은
     * 그대로 유지한다.
     */
    const fixedValue = getPocketValue(fixedType)

    /*
     * 나머지 포켓들에 배분할 수 있는 금액
     */
    const remainingBudget = totalBudget.value - fixedValue

    const otherTypes = getOtherTypes(fixedType)

    /*
     * 해당 포켓 하나만으로
     * 총예산을 넘긴 경우
     */
    if (remainingBudget < 0) {
      setPocketValue(fixedType, totalBudget.value)

      otherTypes.forEach((type) => {
        setPocketValue(type, 0)
      })

      return
    }

    const otherTotal = otherTypes.reduce((sum, type) => {
      return sum + getPocketValue(type)
    }, 0)

    /*
     * 나머지 3개가 전부 0이면
     * 남는 금액을 비상금에 넣는다.
     */
    if (otherTotal === 0) {
      otherTypes.forEach((type) => {
        setPocketValue(type, 0)
      })

      const fallbackType = otherTypes.includes('emergency') ? 'emergency' : otherTypes[0]

      if (fallbackType) {
        setPocketValue(fallbackType, remainingBudget)
      }

      return
    }

    let assignedAmount = 0

    /*
     * 나머지 포켓의 기존 비율을 기준으로
     * remainingBudget 재분배
     */
    otherTypes.forEach((type, index) => {
      const currentValue = getPocketValue(type)

      let nextValue = 0

      /*
       * 마지막 포켓은
       * 반올림 오차까지 전부 받는다.
       */
      if (index === otherTypes.length - 1) {
        nextValue = remainingBudget - assignedAmount
      } else {
        const ratio = currentValue / otherTotal

        nextValue = Math.round((remainingBudget * ratio) / 50_000) * 50_000
      }

      nextValue = Math.max(0, nextValue)

      setPocketValue(type, nextValue)

      assignedAmount += nextValue
    })

    /*
     * 혹시 발생할 수 있는
     * 합계 오차 최종 보정
     */
    normalizeTotal(fixedType)
  }

  // =========================
  // 합계 보정
  // =========================

  const normalizeTotal = (fixedType: PocketType) => {
    const gap = totalBudget.value - allocatedTotal.value

    if (gap === 0) {
      return
    }

    const candidates = getOtherTypes(fixedType)

    const target = candidates.find((type) => {
      return getPocketValue(type) + gap >= 0
    })

    if (!target) {
      return
    }

    setPocketValue(target, getPocketValue(target) + gap)
  }

  // =========================
  // 초기화
  // =========================

  const resetBudget = () => {
    totalBudget.value = 2_500_000

    essentialBudget.value = 1_050_000

    freeBudget.value = 650_000

    futureBudget.value = 500_000

    emergencyBudget.value = 300_000
  }

  return {
    totalBudget,

    essentialBudget,
    freeBudget,
    futureBudget,
    emergencyBudget,

    allocatedTotal,
    budgetGap,
    isBudgetBalanced,

    previousAsset,
    expectedAsset,
    difference,

    updatePocket,
    setTotalBudget,
    autoBalance,
    resetBudget,
  }
})

import { computed, ref } from 'vue'

import { defineStore } from 'pinia'

import type { BudgetRecommendationResponse } from '@/api/budget'

export type PocketType = 'essential' | 'free' | 'future' | 'emergency'

export const usePocketBudgetStore = defineStore('pocketBudget', () => {
  const totalBudget = ref(0)

  const essentialBudget = ref(0)

  const freeBudget = ref(0)

  const futureBudget = ref(0)

  const emergencyBudget = ref(0)

  /*
   * 추천값을 이미 불러왔는지.
   */
  const initializedFromRecommendation = ref(false)

  /*
   * 추천 계산 근거.
   *
   * 화면에 당장 표시하지 않더라도
   * 디버깅과 향후 "계산 기준 보기"에서 사용 가능.
   */
  const monthlyIncome = ref(0)

  const monthlyHousingCost = ref(0)

  const currentAsset = ref(0)

  const emergencyReserveTarget = ref(0)

  const emergencyReserveNeeded = ref(false)

  const toSafeAmount = (value: unknown): number => {
    const numberValue = Number(value)

    if (!Number.isFinite(numberValue)) {
      return 0
    }

    return Math.max(0, Math.round(numberValue))
  }

  const allocatedTotal = computed(() => {
    return essentialBudget.value + freeBudget.value + futureBudget.value + emergencyBudget.value
  })

  const budgetGap = computed(() => {
    return allocatedTotal.value - totalBudget.value
  })

  const isBudgetBalanced = computed(() => {
    return totalBudget.value > 0 && budgetGap.value === 0
  })

  /*
   * =========================
   * 추천 API 응답 적용
   * =========================
   */

  const initializeFromRecommendation = (recommendation: BudgetRecommendationResponse) => {
    totalBudget.value = toSafeAmount(recommendation.totalBudgetAmount)

    monthlyIncome.value = toSafeAmount(recommendation.monthlyIncome)

    monthlyHousingCost.value = toSafeAmount(recommendation.monthlyHousingCost)

    currentAsset.value = toSafeAmount(recommendation.currentAsset)

    emergencyReserveTarget.value = toSafeAmount(recommendation.emergencyReserveTarget)

    emergencyReserveNeeded.value = Boolean(recommendation.emergencyReserveNeeded)

    essentialBudget.value = 0

    freeBudget.value = 0

    futureBudget.value = 0

    emergencyBudget.value = 0

    const pockets = recommendation.pockets ?? []

    pockets.forEach((pocket) => {
      const amount = toSafeAmount(pocket.amount)

      switch (pocket.pocketType) {
        case 'ESSENTIAL':
          essentialBudget.value = amount
          break

        case 'FREE':
          freeBudget.value = amount
          break

        case 'FUTURE_ASSET':
          futureBudget.value = amount
          break

        case 'EMERGENCY':
          emergencyBudget.value = amount
          break
      }
    })

    /*
     * 혹시 서버 반올림 등으로
     * 1원 차이가 발생해도
     * 마지막 비상금에 맞춘다.
     */
    const difference = totalBudget.value - allocatedTotal.value

    if (difference !== 0) {
      emergencyBudget.value = Math.max(0, emergencyBudget.value + difference)
    }

    initializedFromRecommendation.value = true
  }

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

  const setPocketValue = (type: PocketType, value: number) => {
    const normalized = toSafeAmount(value)

    switch (type) {
      case 'essential':
        essentialBudget.value = normalized
        break

      case 'free':
        freeBudget.value = normalized
        break

      case 'future':
        futureBudget.value = normalized
        break

      case 'emergency':
        emergencyBudget.value = normalized
        break
    }
  }

  const updatePocket = (type: PocketType, value: number) => {
    const normalized = Math.min(toSafeAmount(value), totalBudget.value)

    setPocketValue(type, normalized)
  }

  const setTotalBudget = (value: number) => {
    const normalized = toSafeAmount(value)

    if (normalized <= 0) {
      return
    }

    totalBudget.value = normalized
  }

  const getOtherTypes = (fixedType: PocketType): PocketType[] => {
    const all: PocketType[] = ['essential', 'free', 'future', 'emergency']

    return all.filter((type) => type !== fixedType)
  }

  /*
   * 초기 조정 화면의
   * 자동 맞추기 기능.
   *
   * 사용자가 건드린 포켓은 유지하고
   * 나머지 포켓 비율을 유지하며 재배분.
   */
  const autoBalance = (fixedType: PocketType) => {
    const fixedValue = getPocketValue(fixedType)

    const remaining = totalBudget.value - fixedValue

    const otherTypes = getOtherTypes(fixedType)

    if (remaining < 0) {
      setPocketValue(fixedType, totalBudget.value)

      otherTypes.forEach((type) => {
        setPocketValue(type, 0)
      })

      return
    }

    const previousOtherTotal = otherTypes.reduce((sum, type) => sum + getPocketValue(type), 0)

    if (previousOtherTotal <= 0) {
      otherTypes.forEach((type) => {
        setPocketValue(type, 0)
      })

      const target = otherTypes.includes('emergency') ? 'emergency' : otherTypes[0]

      if (target) {
        setPocketValue(target, remaining)
      }

      return
    }

    let assigned = 0

    otherTypes.forEach((type, index) => {
      let next = 0

      if (index === otherTypes.length - 1) {
        next = remaining - assigned
      } else {
        const ratio = getPocketValue(type) / previousOtherTotal

        next = Math.round(remaining * ratio)
      }

      next = Math.max(0, next)

      setPocketValue(type, next)

      assigned += next
    })
  }

  const resetBudget = () => {
    totalBudget.value = 0

    essentialBudget.value = 0

    freeBudget.value = 0

    futureBudget.value = 0

    emergencyBudget.value = 0

    monthlyIncome.value = 0

    monthlyHousingCost.value = 0

    currentAsset.value = 0

    emergencyReserveTarget.value = 0

    emergencyReserveNeeded.value = false

    initializedFromRecommendation.value = false
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

    monthlyIncome,
    monthlyHousingCost,
    currentAsset,
    emergencyReserveTarget,
    emergencyReserveNeeded,

    initializedFromRecommendation,

    initializeFromRecommendation,

    updatePocket,
    setTotalBudget,
    autoBalance,

    resetBudget,
  }
})

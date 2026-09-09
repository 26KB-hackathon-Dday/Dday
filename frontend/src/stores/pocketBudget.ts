import { computed, ref } from 'vue'

import { defineStore } from 'pinia'

export type PocketType = 'essential' | 'free' | 'future' | 'emergency'

export const usePocketBudgetStore = defineStore('pocketBudget', () => {
  const toSafeAmount = (value: unknown) => {
    const numberValue = Number(value)

    if (!Number.isFinite(numberValue)) {
      return 0
    }

    return Math.max(0, Math.round(numberValue))
  }

  /*
   * =========================
   * 총 예산
   * =========================
   */

  const totalBudget = ref(2_500_000)

  /*
   * =========================
   * 포켓별 예산
   * =========================
   */

  const essentialBudget = ref(1_050_000)

  const freeBudget = ref(650_000)

  const futureBudget = ref(500_000)

  const emergencyBudget = ref(300_000)

  /*
   * 기존 화면 호환용.
   *
   * 실제 예상자산 화면에서는
   * assetForecast API를 사용한다.
   */
  const previousAsset = ref(24_000_000)

  const expectedAsset = computed(() => {
    return Math.round(previousAsset.value + futureBudget.value * 14.4)
  })

  const difference = computed(() => {
    return expectedAsset.value - previousAsset.value
  })

  const allocatedTotal = computed(() => {
    return (
      toSafeAmount(essentialBudget.value) +
      toSafeAmount(freeBudget.value) +
      toSafeAmount(futureBudget.value) +
      toSafeAmount(emergencyBudget.value)
    )
  })

  const budgetGap = computed(() => {
    return allocatedTotal.value - toSafeAmount(totalBudget.value)
  })

  const isBudgetBalanced = computed(() => {
    return budgetGap.value === 0 && totalBudget.value > 0
  })

  const getPocketValue = (type: PocketType): number => {
    switch (type) {
      case 'essential':
        return toSafeAmount(essentialBudget.value)

      case 'free':
        return toSafeAmount(freeBudget.value)

      case 'future':
        return toSafeAmount(futureBudget.value)

      case 'emergency':
        return toSafeAmount(emergencyBudget.value)
    }
  }

  const setPocketValue = (type: PocketType, value: number) => {
    const normalizedValue = toSafeAmount(value)

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

  const updatePocket = (type: PocketType, value: number) => {
    const safeValue = toSafeAmount(value)

    const normalizedValue = Math.min(safeValue, toSafeAmount(totalBudget.value))

    setPocketValue(type, normalizedValue)
  }

  const setTotalBudget = (newTotal: number) => {
    const normalized = toSafeAmount(newTotal)

    if (normalized <= 0) {
      return
    }

    totalBudget.value = normalized
  }

  const getOtherTypes = (fixedType: PocketType): PocketType[] => {
    const allTypes: PocketType[] = ['essential', 'free', 'future', 'emergency']

    return allTypes.filter((type) => type !== fixedType)
  }

  const autoBalance = (fixedType: PocketType) => {
    const fixedValue = getPocketValue(fixedType)

    const remainingBudget = totalBudget.value - fixedValue

    const otherTypes = getOtherTypes(fixedType)

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

    otherTypes.forEach((type, index) => {
      let nextValue = 0

      if (index === otherTypes.length - 1) {
        nextValue = remainingBudget - assignedAmount
      } else {
        const ratio = getPocketValue(type) / otherTotal

        nextValue = Math.round(remainingBudget * ratio)
      }

      nextValue = Math.max(0, nextValue)

      setPocketValue(type, nextValue)

      assignedAmount += nextValue
    })

    normalizeTotal(fixedType)
  }

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

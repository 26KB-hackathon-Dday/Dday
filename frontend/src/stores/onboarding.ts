import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  onboardingApi,
  type HousingType,
  type IncomeItem,
  type IncomeType,
  type ProtectionStatus,
  type SettlementReceived,
} from '@/api/onboarding'

/**
 * 온보딩(자립 계획 생성) 화면 간 공유 스토어.
 *
 * 각 단계는 "다음" 클릭 시 그 단계의 API를 직접 부르고(11번 정기수입 목록만 예외),
 * 응답값을 여기 담아 다음 화면에서 읽는다. 새로고침하면 비워진다 — 해커톤 범위라
 * 이어하기는 구현하지 않는다(온보딩 중 새로고침 시 /onboarding/intro로 되돌아간다).
 */
export const useOnboardingStore = defineStore('onboarding', () => {
  // 1. 자립 시작 — 보호종료일
  const protectionEndDate = ref('')
  const supportEndDate = ref('')
  const remainingMonths = ref(0)
  const dDay = ref(0)
  const protectionStatus = ref<ProtectionStatus | null>(null)

  // 1. 자립 시작 — 거주 지역
  const regionCode = ref('')
  const regionName = ref('')
  const districtName = ref('')

  // 2. 나의 생활 — 주거
  const housingType = ref<HousingType | null>(null)
  const skipHousingCost = ref(false)
  const deposit = ref(0)
  const monthlyRent = ref(0)
  const maintenanceFee = ref(0)
  const estimatedMonthly = ref(0)

  // 3. 들어오는 돈 — 정기수입
  const incomes = ref<IncomeItem[]>([])
  const totalMonthly = ref(0)

  // 4. 모아둔 돈
  const settlementReceived = ref<SettlementReceived | null>(null)
  const totalSaved = ref(0)

  // 계획 생성 결과
  const monthlyIncome = ref(0)
  const monthlyExpense = ref(0)

  async function submitProtectionDate(date: string) {
    const result = await onboardingApi.saveProtectionDate({ protectionEndDate: date })
    protectionEndDate.value = result.protectionEndDate
    supportEndDate.value = result.supportEndDate
    remainingMonths.value = result.remainingMonths
    dDay.value = result.dDay
    protectionStatus.value = result.protectionStatus
    return result
  }

  /**
   * 지역-코드 매핑 테이블이 아직 없어, 시/도+구 이름 조합을 임시 코드로 쓴다.
   * TODO: 백엔드에 법정동 코드 매핑이 생기면 이 값을 실제 regionCode로 교체한다.
   */
  async function submitRegion() {
    regionCode.value = `${regionName.value}-${districtName.value}`
    return onboardingApi.saveRegion({
      regionCode: regionCode.value,
      regionName: regionName.value,
      districtName: districtName.value,
    })
  }

  async function submitHousingType(type: HousingType) {
    const result = await onboardingApi.saveHousingType({ housingType: type })
    housingType.value = result.housingType
    skipHousingCost.value = result.skipHousingCost
    return result
  }

  async function submitHousingCost() {
    const result = await onboardingApi.saveHousingCost({
      deposit: deposit.value || undefined,
      monthlyRent: monthlyRent.value || undefined,
      maintenanceFee: maintenanceFee.value || undefined,
    })
    estimatedMonthly.value = result.estimatedMonthly
    return result
  }

  async function fetchIncomes() {
    const result = await onboardingApi.getIncomes()
    incomes.value = result.items
    totalMonthly.value = result.totalMonthly
    return result
  }

  async function addIncome(body: {
    name: string
    incomeType: IncomeType
    amount: number
    paymentTiming?: string
  }) {
    const result = await onboardingApi.addIncome(body)
    totalMonthly.value = result.totalMonthly
    await fetchIncomes()
    return result
  }

  async function removeIncome(incomeId: number) {
    const result = await onboardingApi.deleteIncome(incomeId)
    totalMonthly.value = result.totalMonthly
    incomes.value = incomes.value.filter((item) => item.incomeId !== incomeId)
    return result
  }

  async function submitAssets() {
    return onboardingApi.saveAssets({
      totalSaved: totalSaved.value,
      settlementReceived: settlementReceived.value ?? 'NONE',
    })
  }

  async function complete() {
    const result = await onboardingApi.complete()
    dDay.value = result.dDay
    monthlyIncome.value = result.monthlyIncome
    monthlyExpense.value = result.monthlyExpense
    return result
  }

  return {
    protectionEndDate,
    supportEndDate,
    remainingMonths,
    dDay,
    protectionStatus,
    regionCode,
    regionName,
    districtName,
    housingType,
    skipHousingCost,
    deposit,
    monthlyRent,
    maintenanceFee,
    estimatedMonthly,
    incomes,
    totalMonthly,
    settlementReceived,
    totalSaved,
    monthlyIncome,
    monthlyExpense,
    submitProtectionDate,
    submitRegion,
    submitHousingType,
    submitHousingCost,
    fetchIncomes,
    addIncome,
    removeIncome,
    submitAssets,
    complete,
  }
})

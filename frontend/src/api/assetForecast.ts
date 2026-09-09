import { api } from '@/api/client'

export interface AssetForecastResponse {
  /**
   * 현재 보유 자산
   *
   * 선택된 활성 마이데이터 계좌 잔액 합계.
   * 선택 계좌가 없으면 온보딩 initialAsset.
   */
  currentAsset: number
  monthlyFutureAmount: number
  achievedAmount: number
  remainingAmount: number
  achievementRate: number
  remainingMonths: number
  supportEndDate: string
  expectedAdditionalAsset: number
  expectedAsset: number
  investmentReturnIncluded: boolean
  currentTotalAsset: number
  currentCashAsset: number
  currentSavingInvestmentAsset: number
  currentHousingDeposit: number
  forecastTotalAsset: number
  forecastNetAsset: number
  forecastShortageAmount: number
  forecastCashAsset: number
  forecastSavingInvestmentAsset: number
  forecastHousingDeposit: number
  monthlyConvertedNetIncome: number
  averageMonthlySavingInvestment: number
  totalAssetChange: number
  savingInvestmentChange: number
  cashDepletionMonth: number | null
  liquidAssetDepletionMonth: number | null
  housingDepositDepletionMonth: number | null
  cashRatio: number
  savingInvestmentRatio: number
  housingDepositRatio: number
  calculationBasis: {
    monthlyIncome: number[]
    monthlyExpense: number[]
    currentFixedIncome: number
    monthlySavingInvestment: number[]
    validIncomeMonthCount: number
  }
  dataSufficient: boolean
}

export const assetForecastApi = {
  find: () => api.get<AssetForecastResponse>('/api/asset-forecast'),
}

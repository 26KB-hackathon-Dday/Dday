import { api } from '@/api/client'

export interface AssetForecastResponse {
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
}

export const assetForecastApi = {
  find: () => api.get<AssetForecastResponse>('/api/asset-forecast'),
}

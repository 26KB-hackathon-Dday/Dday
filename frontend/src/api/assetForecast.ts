import { api } from '@/api/client'

export interface AssetForecastResponse {
  /**
   * 현재 보유 자산
   *
   * 선택된 활성 마이데이터 계좌 잔액 합계.
   * 선택 계좌가 없으면 온보딩 initialAsset.
   */
  currentAsset: number

  /**
   * 지원 종료까지 남은 개월 수
   */
  remainingMonths: number
}

export const assetForecastApi = {
  find: () => api.get<AssetForecastResponse>('/api/asset-forecast'),
}

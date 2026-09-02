import { api } from './client'

/**
 * 포켓 용도. 백엔드 `PocketType` enum과 값이 같아야 한다.
 * 정본은 `backend/src/main/java/com/dday/domain/pocket/entity/PocketType.java`다.
 */
export type PocketType = 'HOUSING' | 'LIVING' | 'EMERGENCY' | 'ASSET'

export interface Pocket {
  pocketId: number
  type: PocketType
  /** 화면에 그대로 띄우는 이름 (주거, 생활, …). 서버가 내려주므로 프론트에 매핑표를 두지 않는다 */
  label: string
  description: string
  monthlyBudget: number
  spentThisMonth: number
  /** 남은 금액. 예산을 넘겼으면 음수다 */
  remaining: number
  /** 소진율(%). 소수점 1자리 */
  usageRate: number
}

export const pocketApi = {
  findAll: () => api.get<Pocket[]>('/api/pockets'),
  findById: (pocketId: number) => api.get<Pocket>(`/api/pockets/${pocketId}`),
}

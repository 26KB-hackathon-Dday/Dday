import { ref } from 'vue'
import { mydataApi } from '@/api/mydata'
import { ApiError } from '@/api/types'

type Reload = () => Promise<unknown>

export function useMydataRefresh(reload: Reload) {
  const lastSyncedAt = ref<string | null>(null)
  const refreshing = ref(false)
  const refreshError = ref<string | null>(null)

  async function loadLastSyncedAt() {
    const [accountResult, cardResult] = await Promise.allSettled([
      mydataApi.fetchAccounts(),
      mydataApi.fetchCards(),
    ])
    const candidates = [
      ...(accountResult.status === 'fulfilled' ? accountResult.value.accounts : []),
      ...(cardResult.status === 'fulfilled' ? cardResult.value.cards : []),
    ]
      .map((item) => item.lastSyncedAt)
      .filter((value): value is string => Boolean(value))

    lastSyncedAt.value = candidates.reduce<string | null>((latest, value) => {
      if (!latest) return value
      return new Date(value).getTime() > new Date(latest).getTime() ? value : latest
    }, null)
  }

  async function refresh() {
    if (refreshing.value) return false
    refreshing.value = true
    refreshError.value = null
    try {
      const result = await mydataApi.sync()
      lastSyncedAt.value = result.syncedAt
      await reload()
      return true
    } catch (error) {
      refreshError.value =
        error instanceof ApiError ? error.message : '마이데이터를 갱신하지 못했습니다.'
      return false
    } finally {
      refreshing.value = false
    }
  }

  return { lastSyncedAt, refreshing, refreshError, loadLastSyncedAt, refresh }
}

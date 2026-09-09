<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'

const props = defineProps<{
  lastSyncedAt: string | null
  refreshing: boolean
  error?: string | null
}>()
const emit = defineEmits<{ refresh: [] }>()

const syncedAtLabel = computed(() => {
  if (!props.lastSyncedAt) return '동기화 이력 없음'
  const syncedAt = new Date(props.lastSyncedAt)
  if (Number.isNaN(syncedAt.getTime())) return '동기화 이력 없음'

  const now = new Date()
  const time = syncedAt.toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
  const isToday =
    syncedAt.getFullYear() === now.getFullYear() &&
    syncedAt.getMonth() === now.getMonth() &&
    syncedAt.getDate() === now.getDate()

  return isToday
    ? `실제 데이터 동기화 ${time}`
    : `실제 데이터 동기화 ${syncedAt.getMonth() + 1}월 ${syncedAt.getDate()}일 ${time}`
})
</script>

<template>
  <div class="refresh-status" :aria-busy="refreshing">
    <div class="refresh-status__control">
      <span>{{ refreshing ? '갱신 중…' : syncedAtLabel }}</span>
      <button
        type="button"
        :disabled="refreshing"
        :aria-label="refreshing ? '마이데이터 갱신 중' : '마이데이터 새로고침'"
        @click="emit('refresh')"
      >
        <AppIcon name="refresh" :size="15" :class="{ spinning: refreshing }" aria-hidden="true" />
      </button>
    </div>
    <p v-if="error" role="alert">{{ error }}</p>
  </div>
</template>

<style scoped>
.refresh-status {
  display: grid;
  justify-items: end;
  min-width: 0;
}
.refresh-status__control {
  display: flex;
  align-items: center;
  gap: 6px;
}
.refresh-status__control > span {
  color: var(--c-text-3);
  font-family: var(--font-num);
  font-size: 10px;
  white-space: nowrap;
}
button {
  display: grid;
  flex: none;
  width: 30px;
  height: 30px;
  place-items: center;
  border: 0;
  background: transparent;
  color: var(--c-text-2);
  cursor: pointer;
}
button:disabled {
  cursor: wait;
  opacity: 0.55;
}
p {
  max-width: 190px;
  margin-top: 3px;
  color: var(--c-danger);
  font-size: 9px;
  text-align: right;
}
.spinning {
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>

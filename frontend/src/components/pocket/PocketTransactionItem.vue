<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import type { PocketTransaction } from '@/api/pocket'
import { formatWon } from '@/utils/format'

const props = defineProps<{ transaction: PocketTransaction }>()

const title = computed(
  () => props.transaction.merchantName || props.transaction.memo || '거래 내역',
)
const dateLabel = computed(() => {
  const date = new Date(props.transaction.transactionAt)
  if (Number.isNaN(date.getTime())) return props.transaction.transactionAt
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
})
const signedAmount = computed(() => {
  const prefix = props.transaction.transactionType === 'EXPENSE' ? '-' : ''
  return `${prefix}${formatWon(props.transaction.amount)}`
})
</script>

<template>
  <li class="transaction-item">
    <span class="transaction-item__icon" aria-hidden="true">
      <AppIcon name="pocket" :size="18" />
    </span>
    <span class="transaction-item__copy">
      <strong>{{ title }}</strong>
      <small>{{ dateLabel }}</small>
    </span>
    <strong class="transaction-item__amount">{{ signedAmount }}</strong>
  </li>
</template>

<style scoped>
.transaction-item {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
  padding: 11px 0;
}
.transaction-item__icon {
  display: grid;
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 50%;
  background: var(--c-surface);
  color: var(--c-text-2);
}
.transaction-item__copy {
  display: grid;
  flex: 1;
  min-width: 0;
  gap: 2px;
}
.transaction-item__copy strong {
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.transaction-item__copy small {
  color: var(--c-text-3);
  font-size: 10px;
}
.transaction-item__amount {
  flex: none;
  font-family: var(--font-num);
  font-size: 13px;
  white-space: nowrap;
}
</style>

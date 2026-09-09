<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import type { PocketTransaction } from '@/api/pocket'
import { formatWon } from '@/utils/format'

const props = defineProps<{ transaction: PocketTransaction }>()
const emit = defineEmits<{ select: [transactionId: number] }>()

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
const categoryCode = computed(() => {
  if (props.transaction.category?.categoryCode) return props.transaction.category.categoryCode

  const codesByName: Record<string, string> = {
    '주거·관리비': 'HOUSING',
    주거비: 'HOUSING',
    공과금: 'UTILITY',
    통신비: 'TELECOM',
    보험료: 'INSURANCE',
    식비: 'FOOD',
    쇼핑: 'SHOPPING',
    의료: 'MEDICAL',
    '의료·건강': 'MEDICAL',
    취미: 'CULTURE',
    '문화·여가': 'CULTURE',
    미용: 'BEAUTY',
    '뷰티·미용': 'BEAUTY',
    여행: 'TRAVEL',
    기타: 'ETC',
    교통: 'TRANSPORT',
    교통비: 'TRANSPORT',
  }
  return codesByName[props.transaction.category?.categoryName ?? ''] ?? 'ETC'
})
const categoryPresentation = computed(() => {
  const presentations: Record<string, { icon: string; color: string; background: string }> = {
    HOUSING: { icon: 'category-housing', color: '#3b82f6', background: '#eef4ff' },
    UTILITY: { icon: 'category-utility', color: '#e5a000', background: '#fff8dc' },
    TELECOM: { icon: 'category-telecom', color: '#6366f1', background: '#eef2ff' },
    INSURANCE: { icon: 'category-insurance', color: '#16a085', background: '#eaf9f5' },
    FOOD: { icon: 'category-food', color: '#e85d5d', background: '#fff0f0' },
    SHOPPING: { icon: 'category-shopping', color: '#e5484d', background: '#fff0f0' },
    MEDICAL: { icon: 'category-medical', color: '#16a085', background: '#eaf9f5' },
    CULTURE: { icon: 'category-hobby', color: '#8b5cf6', background: '#f5f0ff' },
    BEAUTY: { icon: 'category-beauty', color: '#db3e91', background: '#fff0f7' },
    TRAVEL: { icon: 'category-travel', color: '#3b82f6', background: '#eef4ff' },
    ETC: { icon: 'category-etc', color: '#64748b', background: '#f1f5f9' },
    TRANSPORT: { icon: 'category-transport', color: '#0891b2', background: '#eaf9fa' },
  }
  return (
    presentations[categoryCode.value] ?? {
      icon: 'category-etc',
      color: '#64748b',
      background: '#f1f5f9',
    }
  )
})
</script>

<template>
  <li class="transaction-item">
    <button type="button" @click="emit('select', transaction.transactionId)">
      <span
        class="transaction-item__icon"
        :style="{
          color: categoryPresentation.color,
          backgroundColor: categoryPresentation.background,
        }"
        aria-hidden="true"
      >
        <AppIcon :name="categoryPresentation.icon" :size="18" />
      </span>
      <span class="transaction-item__copy">
        <strong>{{ title }}</strong>
        <small>{{ dateLabel }}</small>
      </span>
      <strong class="transaction-item__amount">{{ signedAmount }}</strong>
    </button>
  </li>
</template>

<style scoped>
.transaction-item {
  min-width: 0;
}
.transaction-item button {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 11px;
  min-width: 0;
  padding: 11px 0;
  text-align: left;
}
.transaction-item button:focus-visible {
  outline: 2px solid var(--c-blue);
  outline-offset: 2px;
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

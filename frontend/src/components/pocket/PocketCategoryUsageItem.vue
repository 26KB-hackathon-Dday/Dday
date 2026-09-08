<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import { formatWon } from '@/utils/format'

defineProps<{
  name: string
  description: string
  usedAmount: number
  budgetAmount: number
  icon: string
}>()
</script>

<template>
  <li class="category-item">
    <span class="category-item__icon" aria-hidden="true">
      <AppIcon :name="icon" :size="19" />
    </span>
    <span class="category-item__copy">
      <strong>{{ name }}</strong>
      <small>{{ description }}</small>
    </span>
    <span class="category-item__amount">
      <strong>{{ formatWon(usedAmount) }}</strong>
      <small>예산 {{ formatWon(budgetAmount) }}</small>
    </span>
  </li>
</template>

<style scoped>
.category-item {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
  padding: 15px 14px;
}
.category-item + .category-item {
  border-top: 1px solid var(--c-border);
}
.category-item__icon {
  display: grid;
  flex: 0 0 40px;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 50%;
  background: #eef4ff;
  color: var(--c-blue);
}
.category-item__copy,
.category-item__amount {
  display: grid;
  min-width: 0;
  gap: 2px;
}
.category-item__copy {
  flex: 1;
}
.category-item__copy strong,
.category-item__amount strong {
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.category-item__copy small,
.category-item__amount small {
  color: var(--c-text-3);
  font-size: 10px;
  white-space: nowrap;
}
.category-item__amount {
  flex: 0 0 auto;
  text-align: right;
}
.category-item__amount strong {
  font-family: var(--font-num);
}
@media (max-width: 340px) {
  .category-item {
    gap: 8px;
    padding-inline: 10px;
  }
  .category-item__icon {
    flex-basis: 36px;
    width: 36px;
    height: 36px;
  }
}
</style>

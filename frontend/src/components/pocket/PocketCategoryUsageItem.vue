<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { formatWon } from '@/utils/format'

const props = defineProps<{
  name: string
  categoryCode: string
  usedAmount: number
  selected?: boolean
}>()
const emit = defineEmits<{ select: [] }>()

const icon = computed(() => {
  const icons: Record<string, string> = {
    HOUSING: 'category-housing',
    UTILITY: 'category-utility',
    TELECOM: 'category-telecom',
    INSURANCE: 'category-insurance',
    TRANSPORT: 'category-transport',
    MEDICAL: 'category-medical',
    UNCLASSIFIED: 'category-etc',
  }
  return icons[props.categoryCode] ?? 'category-etc'
})
</script>

<template>
  <li class="category-item">
    <button type="button" :aria-pressed="selected" @click="emit('select')">
      <span class="category-item__icon" aria-hidden="true">
        <AppIcon :name="icon" :size="19" />
      </span>
      <span class="category-item__copy">
        <strong>{{ name }}</strong>
      </span>
      <span class="category-item__amount">
        <strong>{{ formatWon(usedAmount) }}</strong>
      </span>
      <AppIcon class="category-item__chevron" name="chevron-right" :size="14" aria-hidden="true" />
    </button>
  </li>
</template>

<style scoped>
.category-item {
  min-width: 0;
}
.category-item + .category-item {
  border-top: 1px solid var(--c-border);
}
.category-item button {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 11px;
  min-width: 0;
  padding: 15px 14px;
  text-align: left;
}
.category-item button[aria-pressed='true'] {
  background: #f7faff;
}
.category-item button:focus-visible {
  outline: 2px solid var(--c-blue);
  outline-offset: -2px;
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
.category-item__amount {
  flex: 0 0 auto;
  text-align: right;
}
.category-item__amount strong {
  font-family: var(--font-num);
}
.category-item__chevron {
  flex: none;
  color: var(--c-text-3);
}
@media (max-width: 340px) {
  .category-item button {
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

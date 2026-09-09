<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import { formatWon } from '@/utils/format'

defineProps<{ title: string; budget: number }>()
defineEmits<{ adjust: [] }>()
</script>

<template>
  <article class="card">
    <header>
      <AppIcon name="pocket-emergency" :size="20" aria-hidden="true" />
      <h2>{{ title }}</h2>
    </header>
    <dl>
      <div>
        <dt>예산</dt>
        <dd>{{ formatWon(budget) }}</dd>
      </div>
    </dl>
    <button type="button" @click="$emit('adjust')">
      포켓 예산 조정하기
      <AppIcon name="chevron-right" :size="16" aria-hidden="true" />
    </button>
  </article>
</template>

<style scoped>
.card {
  padding: 18px;
  border-radius: 14px;
  background: #eaf9fa;
}
header {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #27b8b8;
}
h2 {
  color: var(--c-text);
  font-size: 16px;
  font-weight: 700;
}
dl {
  margin-top: 20px;
}
dl div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}
dt,
dd {
  color: var(--c-text-2);
  font-size: 12px;
}
dd {
  font-family: var(--font-num);
  white-space: nowrap;
}
button {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 16px;
  padding: 11px 14px;
  border: 0;
  border-radius: 10px;
  background: rgb(255 255 255 / 72%);
  color: #168f8f;
  font-size: 13px;
  font-weight: 700;
}
button:hover {
  filter: brightness(0.98);
}
</style>

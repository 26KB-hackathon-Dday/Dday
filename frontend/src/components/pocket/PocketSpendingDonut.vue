<script setup lang="ts">
import { computed } from 'vue'

export interface SpendingCategory {
  name: string
  amount: number
}
const props = defineProps<{ categories: SpendingCategory[] }>()
const DONUT_COLORS = ['#ff914d', '#222222', '#858585', '#c9c9c9', '#f5b487', '#626262']
const total = computed(() => props.categories.reduce((sum, category) => sum + category.amount, 0))
const segments = computed(() =>
  [...props.categories]
    .sort((a, b) => b.amount - a.amount)
    .map((category, index) => ({
      ...category,
      color: DONUT_COLORS[index % DONUT_COLORS.length],
      ratio: total.value === 0 ? 0 : (category.amount / total.value) * 100,
    })),
)
const leader = computed(() => segments.value[0] ?? null)
const gradient = computed(() => {
  if (total.value === 0) return '#dedede'
  let start = 0
  return `conic-gradient(${segments.value
    .map((segment) => {
      const end = start + segment.ratio
      const stop = `${segment.color} ${start}% ${end}%`
      start = end
      return stop
    })
    .join(', ')})`
})
const roundedRatio = (ratio: number) => Math.round(ratio)
</script>

<template>
  <section class="spending" aria-labelledby="spending-title">
    <h2 id="spending-title">지출 비중</h2>
    <p v-if="total === 0" class="spending__empty">이번 달 지출 내역이 없어요</p>
    <template v-else>
      <div
        class="donut"
        role="img"
        :aria-label="segments.map((item) => `${item.name} ${roundedRatio(item.ratio)}%`).join(', ')"
        :style="{ background: gradient }"
      >
        <div class="donut__center">
          <strong>{{ leader?.name }}</strong
          ><span>{{ roundedRatio(leader?.ratio ?? 0) }}%</span>
        </div>
      </div>
      <ul class="legend">
        <li v-for="segment in segments" :key="segment.name">
          <span class="legend__dot" :style="{ backgroundColor: segment.color }" /><span>{{
            segment.name
          }}</span
          ><strong>{{ roundedRatio(segment.ratio) }}%</strong>
        </li>
      </ul>
    </template>
  </section>
</template>

<style scoped>
.spending {
  width: 100%;
  padding: 22px;
  border-radius: 14px;
  background: var(--c-surface);
}
.spending h2 {
  font-size: 17px;
  font-weight: 700;
}
.donut {
  display: grid;
  width: 156px;
  height: 156px;
  margin: 24px auto;
  place-items: center;
  border-radius: 50%;
}
.donut__center {
  display: grid;
  width: 96px;
  height: 96px;
  place-content: center;
  border-radius: 50%;
  background: var(--c-surface);
  text-align: center;
}
.donut__center strong {
  max-width: 78px;
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.donut__center span {
  font-family: var(--font-num);
  font-size: 21px;
  font-weight: 700;
}
.legend {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 18px;
  list-style: none;
}
.legend li {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 6px;
  font-size: 12px;
}
.legend li > span:nth-child(2) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.legend strong {
  margin-left: auto;
  font-family: var(--font-num);
}
.legend__dot {
  flex: 0 0 8px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.spending__empty {
  padding: 48px 0 34px;
  color: var(--c-text-3);
  text-align: center;
  font-size: 12px;
}
@media (max-width: 340px) {
  .spending {
    padding-inline: 16px;
  }
  .legend {
    gap-inline: 10px;
  }
}
</style>

<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { formatPercent, formatWon } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    title: string
    budget: number
    achievedAmount?: number | null
    achievementRate?: number | null
    currentAsset?: number | null
  }>(),
  {
    achievedAmount: null,
    achievementRate: null,
    currentAsset: null,
  },
)
const emit = defineEmits<{ select: [] }>()
const fillWidth = computed(() =>
  props.achievementRate == null ? '0%' : `${Math.min(Math.max(props.achievementRate, 0), 100)}%`,
)
</script>
<template>
  <button type="button" class="card" @click="emit('select')">
    <header>
      <AppIcon name="recommend" :size="20" aria-hidden="true" />
      <h2>{{ title }}</h2>
    </header>
    <div class="progress-row">
      <div
        class="progress"
        :role="achievementRate == null ? undefined : 'progressbar'"
        aria-label="미래자산 계획 달성률"
        :aria-valuenow="
          achievementRate == null ? undefined : Math.min(Math.max(achievementRate, 0), 100)
        "
        aria-valuemin="0"
        aria-valuemax="100"
      >
        <span :style="{ width: fillWidth }" />
      </div>
      <small>
        {{ achievementRate == null ? '달성률 집계 전' : `${formatPercent(achievementRate)}% 달성` }}
      </small>
    </div>
    <dl>
      <div>
        <dt>예산</dt>
        <dd>{{ formatWon(budget) }}</dd>
      </div>
      <div>
        <dt>현재 달성액</dt>
        <dd :class="{ emphasis: achievedAmount != null }">
          {{ achievedAmount == null ? '집계 전' : formatWon(achievedAmount) }}
        </dd>
      </div>
      <div v-if="currentAsset != null" class="valuation">
        <dt>현재 미래자산</dt>
        <dd>{{ formatWon(currentAsset) }}</dd>
      </div>
    </dl>
  </button>
</template>
<style scoped>
.card {
  display: block;
  width: 100%;
  padding: 18px;
  border-radius: 14px;
  background: #f8f0ff;
  text-align: left;
  transition:
    transform 0.15s ease,
    filter 0.15s ease;
}
.card:hover {
  filter: brightness(0.98);
}
.card:active {
  transform: scale(0.99);
}
header {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #a66ee5;
}
h2 {
  color: var(--c-text);
  font-size: 16px;
  font-weight: 700;
}
.progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}
.progress {
  flex: 1;
  height: 5px;
  overflow: hidden;
  border-radius: 999px;
  background: #dedede;
}
.progress span {
  display: block;
  width: 0;
  height: 100%;
  background: #b17ae9;
}
dl {
  display: grid;
  gap: 5px;
  margin-top: 20px;
}
dl div {
  display: flex;
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
.progress-row small {
  flex: none;
  color: var(--c-text-2);
  font-size: 9px;
  white-space: nowrap;
}
.emphasis {
  color: var(--c-text);
  font-size: 15px;
  font-weight: 700;
}
.valuation {
  margin-top: 8px;
}
</style>

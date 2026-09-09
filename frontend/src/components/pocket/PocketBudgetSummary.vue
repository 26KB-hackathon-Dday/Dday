<script setup lang="ts">
import { computed } from 'vue'
import type { PocketMonthlySummary } from '@/api/pocket'
import { formatPercent, formatWon } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    summary: PocketMonthlySummary
    theme: 'blue' | 'orange' | 'teal'
    pillLabel?: string
  }>(),
  { pillLabel: '이번 달 남은 예산' },
)
const rate = computed(() => props.summary.usageRate ?? 0)
const fillWidth = computed(() => `${Math.min(Math.max(rate.value, 0), 100)}%`)
</script>

<template>
  <section class="summary" :class="`summary--${theme}`" aria-labelledby="remaining-title">
    <p id="remaining-title" class="summary__pill">{{ pillLabel }}</p>
    <strong class="summary__remaining">{{ formatWon(summary.remainingAmount ?? 0) }}</strong>
    <dl class="summary__amounts">
      <div>
        <dt>사용</dt>
        <dd>{{ summary.usedAmount == null ? '집계 전' : formatWon(summary.usedAmount) }}</dd>
      </div>
      <div>
        <dt>총 예산</dt>
        <dd>{{ formatWon(summary.targetAmount) }}</dd>
      </div>
    </dl>
    <div class="summary__progress-row">
      <div
        class="summary__progress"
        role="progressbar"
        :aria-label="`${summary.pocketName} 사용률`"
        :aria-valuenow="Math.min(Math.max(rate, 0), 100)"
        aria-valuemin="0"
        aria-valuemax="100"
      >
        <span :style="{ width: fillWidth }" />
      </div>
      <small>
        {{ summary.usageRate == null ? '사용률 집계 전' : `${formatPercent(rate)}% 사용` }}
      </small>
    </div>
  </section>
</template>

<style scoped>
.summary {
  --summary-accent: var(--c-blue);
  --summary-soft: #eef4ff;
  padding: 22px 0 8px;
  text-align: center;
}
.summary--orange {
  --summary-accent: #ff914d;
  --summary-soft: #fff5ef;
}
.summary--teal {
  --summary-accent: var(--c-teal);
  --summary-soft: #eaf9fa;
}
.summary__pill {
  display: inline-flex;
  padding: 5px 12px;
  border-radius: 999px;
  background: var(--summary-soft);
  color: var(--summary-accent);
  font-size: 12px;
  font-weight: 600;
}
.summary__remaining {
  display: block;
  margin-top: 8px;
  font-family: var(--font-num);
  font-size: 30px;
  line-height: 1.25;
  letter-spacing: -0.5px;
}
.summary__amounts {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 26px;
  color: var(--c-text-2);
  font-size: 12px;
}
.summary__amounts div {
  display: flex;
  gap: 5px;
}
.summary__amounts dd {
  font-family: var(--font-num);
  font-weight: 600;
}
.summary__progress-row {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 9px;
}
.summary__progress {
  flex: 1;
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: #dedede;
}
.summary__progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--summary-accent);
}
.summary__progress-row small {
  flex: none;
  color: var(--c-text-2);
  font-size: 10px;
  white-space: nowrap;
}
@media (max-width: 340px) {
  .summary__remaining {
    font-size: 27px;
  }
  .summary__amounts {
    gap: 8px;
  }
}
</style>

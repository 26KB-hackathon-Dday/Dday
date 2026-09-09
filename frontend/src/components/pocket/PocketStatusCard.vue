<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import type { PocketType } from '@/api/pocket'
import { formatWon } from '@/utils/format'
const props = defineProps<{
  pocketType: Exclude<PocketType, 'FUTURE_ASSET'>
  title: string
  budget: number
  used: number | null
  remaining: number | null
  usageRate: number | null
  overAmount?: number | null
}>()
const emit = defineEmits<{ select: [pocketType: Exclude<PocketType, 'FUTURE_ASSET'>] }>()
// 상세 화면을 지원하는 일반 포켓은 모두 버튼으로 렌더링해 부모에게 선택 이벤트를 전달한다.
const clickable = computed(() => ['ESSENTIAL', 'FREE', 'EMERGENCY'].includes(props.pocketType))
const iconName = computed(() =>
  props.pocketType === 'ESSENTIAL' ? 'pocket' : props.pocketType === 'FREE' ? 'benefit' : 'credit',
)
const rate = computed(() => props.usageRate ?? 0)
const fillWidth = computed(() => `${Math.min(Math.max(rate.value, 0), 100)}%`)
</script>
<template>
  <component
    :is="clickable ? 'button' : 'article'"
    class="card"
    :class="[`card--${pocketType.toLowerCase()}`, { 'card--clickable': clickable }]"
    :type="clickable ? 'button' : undefined"
    @click="clickable && emit('select', pocketType)"
  >
    <header>
      <AppIcon :name="iconName" :size="20" aria-hidden="true" />
      <h2>{{ title }}</h2>
    </header>
    <div class="progress-row">
      <div
        class="progress"
        role="progressbar"
        aria-label="사용률"
        :aria-valuenow="Math.min(Math.max(rate, 0), 100)"
        aria-valuemin="0"
        aria-valuemax="100"
      >
        <span :style="{ width: fillWidth }" />
      </div>
      <small v-if="usageRate != null">{{ usageRate }}% 사용</small>
    </div>
    <dl>
      <div>
        <dt>예산</dt>
        <dd>{{ formatWon(budget) }}</dd>
      </div>
      <div v-if="used != null">
        <dt>사용액</dt>
        <dd>{{ formatWon(used) }}</dd>
      </div>
      <div v-if="overAmount">
        <dt>초과 금액</dt>
        <dd class="emphasis over">{{ formatWon(overAmount) }}</dd>
      </div>
      <div v-else-if="remaining != null">
        <dt>남은 금액</dt>
        <dd class="emphasis">{{ formatWon(remaining) }}</dd>
      </div>
    </dl>
  </component>
</template>
<style scoped>
.card {
  --accent: #397bc7;
  display: block;
  width: 100%;
  padding: 18px;
  border-radius: 14px;
  background: #eef4ff;
  text-align: left;
}
.card--clickable {
  cursor: pointer;
  transition:
    transform 0.15s ease,
    filter 0.15s ease;
}
.card--clickable:hover {
  filter: brightness(0.98);
}
.card--clickable:active {
  transform: scale(0.99);
}
.card--free {
  --accent: #ff914d;
  background: #fff5ef;
}
.card--emergency {
  --accent: #27b8b8;
  background: #eaf9fa;
}
header {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--accent);
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
  height: 100%;
  border-radius: inherit;
  background: var(--accent);
}
small {
  flex: none;
  color: var(--c-text-2);
  font-size: 9px;
  white-space: nowrap;
}
dl {
  display: grid;
  gap: 4px;
  margin-top: 20px;
}
dl div {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 16px;
}
dt,
dd {
  color: var(--c-text-2);
  font-size: 12px;
}
dd {
  flex: none;
  font-family: var(--font-num);
  white-space: nowrap;
}
dl div:last-child {
  margin-top: 7px;
}
.emphasis {
  color: var(--c-text);
  font-size: 15px;
  font-weight: 700;
}
.over {
  color: var(--c-danger);
}
</style>

<template>
  <section class="pocket-card" :class="`pocket-card--${variant}`">
    <div class="pocket-card__top">
      <span class="pocket-badge" :class="`pocket-badge--${variant}`">
        {{ label }}
      </span>

      <strong class="pocket-amount">
        {{ formatCurrency(value) }}
      </strong>
    </div>

    <input
      class="pocket-slider"
      type="range"
      :min="min"
      :max="max"
      :step="step"
      :value="value"
      :style="sliderStyle"
      @input="handleInput"
    />
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type PocketVariant = 'essential' | 'free' | 'future' | 'emergency'

interface Props {
  label: string
  value: number
  variant: PocketVariant
  min?: number
  max: number
  step?: number
}

const props = withDefaults(defineProps<Props>(), {
  min: 0,
  step: 50_000,
})

const emit = defineEmits<{
  change: [value: number]
}>()

const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement

  emit('change', Number(target.value))
}

const sliderPercentage = computed(() => {
  if (props.max <= props.min) {
    return 0
  }

  return ((props.value - props.min) / (props.max - props.min)) * 100
})

const sliderStyle = computed(() => ({
  '--slider-progress': `${sliderPercentage.value}%`,
}))

const formatCurrency = (value: number) => {
  return `${value.toLocaleString('ko-KR')}원`
}
</script>

<style scoped>
.pocket-card {
  width: 100%;

  padding: 22px 18px 20px;

  border-radius: 16px;
}

.pocket-card--essential {
  background: #f0f6ff;
}

.pocket-card--free {
  background: #fff7f1;
}

.pocket-card--future {
  background: #faf2ff;
}

.pocket-card--emergency {
  background: #ebfafa;
}

.pocket-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;

  margin-bottom: 24px;
}

.pocket-badge {
  display: inline-flex;
  align-items: center;

  padding: 6px 10px;

  border-radius: 999px;

  font-size: 14px;
  font-weight: 600;
  line-height: 1.2;
}

.pocket-badge--essential {
  color: #397bc7;
  background: #d5e9ff;
}

.pocket-badge--free {
  color: #ef8e3d;
  background: #ffe1c7;
}

.pocket-badge--future {
  color: #bd6bea;
  background: #eed6ff;
}

.pocket-badge--emergency {
  color: #22a9ad;
  background: #c2eeee;
}

.pocket-amount {
  color: #171717;

  font-size: 18px;
  font-weight: 700;
}

.pocket-slider {
  width: 100%;
  height: 5px;

  margin: 0;

  appearance: none;
  -webkit-appearance: none;

  border-radius: 999px;

  background: linear-gradient(
    to right,
    #111111 0%,
    #111111 var(--slider-progress),
    #d7d7d7 var(--slider-progress),
    #d7d7d7 100%
  );

  cursor: pointer;
}

.pocket-slider::-webkit-slider-thumb {
  width: 22px;
  height: 22px;

  appearance: none;
  -webkit-appearance: none;

  border: 0;
  border-radius: 50%;

  background: #111111;

  cursor: pointer;
}

.pocket-slider::-moz-range-thumb {
  width: 22px;
  height: 22px;

  border: 0;
  border-radius: 50%;

  background: #111111;

  cursor: pointer;
}
</style>

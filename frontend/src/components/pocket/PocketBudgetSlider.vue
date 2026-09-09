<template>
  <section class="pocket-slider" :class="`pocket-slider--${variant}`">
    <div class="pocket-slider__header">
      <div class="pocket-slider__info">
        <span class="pocket-slider__label">
          {{ label }}
        </span>

        <span v-if="showUsedMarker" class="pocket-slider__minimum">
          최소 {{ formatCurrency(min) }} 이상으로 조정 가능
        </span>

        <span v-else class="pocket-slider__minimum">
          최대 {{ formatCurrency(max) }}까지 조정 가능
        </span>
      </div>

      <div class="pocket-slider__amount-wrap">
        <input
          v-model="amountInput"
          class="pocket-slider__amount-input"
          type="text"
          inputmode="numeric"
          @focus="handleAmountFocus"
          @input="handleAmountInput"
          @blur="applyAmountInput"
          @keyup.enter="handleAmountEnter"
        />

        <span class="pocket-slider__unit"> 원 </span>
      </div>
    </div>

    <div class="pocket-slider__range-area" :style="rangeStyle">
      <input
        class="pocket-slider__range"
        type="range"
        min="0"
        :max="safeMax"
        :step="step"
        :value="safeValue"
        @input="handleSliderInput"
      />

      <!-- 최소 달성액 구분선 -->
      <div v-if="showUsedMarker" class="pocket-slider__used-marker">
        <div class="pocket-slider__used-line" />

        <div class="pocket-slider__used-label">
          <span class="pocket-slider__lock"> 🔒 </span>

          <span>
            현재 달성액
            {{ formatCurrency(usedAmount) }}
          </span>
        </div>
      </div>

      <div class="pocket-slider__range-labels">
        <span>0원</span>

        <span>
          {{ formatCurrency(max) }}
        </span>
      </div>
    </div>

    <p v-if="inputError" class="pocket-slider__error">
      {{ inputError }}
    </p>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

type PocketVariant = 'essential' | 'free' | 'future' | 'emergency'

interface Props {
  label: string
  variant: PocketVariant

  value: number

  min?: number
  max: number

  usedAmount?: number

  showUsedMarker?: boolean

  step?: number
}

const props = withDefaults(defineProps<Props>(), {
  min: 0,
  usedAmount: 0,
  showUsedMarker: false,
  step: 500,
})

const emit = defineEmits<{
  (event: 'change', value: number): void
}>()

const amountInput = ref('')

const inputError = ref('')

const isEditing = ref(false)

const safeMax = computed(() => {
  return Math.max(0, Number(props.max) || 0)
})

/*
 * 슬라이더 동그라미는
 * 항상 현재 포켓 할당액 위치.
 */
const safeValue = computed(() => {
  const rawValue = Number(props.value) || 0

  return Math.min(safeMax.value, Math.max(0, rawValue))
})

/*
 * 잠금선 위치.
 *
 * 슬라이더 전체는 항상
 * 0원 ~ max.
 *
 * min 값의 위치를 %로 계산한다.
 */
const usedPercent = computed(() => {
  if (!props.showUsedMarker || safeMax.value <= 0) {
    return 0
  }

  const minimum = Math.max(0, Number(props.min) || 0)

  return Math.min(100, Math.max(0, (minimum / safeMax.value) * 100))
})

const rangeStyle = computed(() => {
  return {
    '--used-percent': `${usedPercent.value}%`,
  }
})

watch(
  () => props.value,

  (newValue) => {
    if (!isEditing.value) {
      amountInput.value = formatNumber(newValue)
    }
  },

  {
    immediate: true,
  },
)

watch(
  () => props.max,

  () => {
    if (props.value > safeMax.value) {
      emit('change', safeMax.value)
    }
  },
)

function formatNumber(value: unknown) {
  const numberValue = Number(value) || 0

  return Math.round(numberValue).toLocaleString('ko-KR')
}

function formatCurrency(value: unknown) {
  return `${formatNumber(value)}원`
}

function handleSliderInput(event: Event) {
  inputError.value = ''

  const target = event.target as HTMLInputElement

  const rawValue = Number(target.value) || 0

  const minimum = Math.max(0, Number(props.min) || 0)

  const maximum = safeMax.value

  /*
   * 사용자가 잠금선보다 왼쪽으로
   * 드래그하면 min에서 멈춘다.
   */
  const nextValue = Math.min(maximum, Math.max(minimum, rawValue))

  target.value = String(nextValue)

  amountInput.value = formatNumber(nextValue)

  emit('change', Math.round(nextValue))
}

function handleAmountFocus() {
  isEditing.value = true

  inputError.value = ''
}

function handleAmountInput(event: Event) {
  const target = event.target as HTMLInputElement

  const onlyNumbers = target.value.replace(/[^0-9]/g, '')

  if (!onlyNumbers) {
    amountInput.value = ''

    return
  }

  amountInput.value = Number(onlyNumbers).toLocaleString('ko-KR')
}

function applyAmountInput() {
  const rawValue = amountInput.value.replace(/,/g, '')

  let nextValue = Number(rawValue)

  if (!Number.isFinite(nextValue)) {
    nextValue = Number(props.value) || 0
  }

  const minimum = Math.max(0, Number(props.min) || 0)

  const maximum = safeMax.value

  if (nextValue < minimum) {
    if (props.showUsedMarker) {
      inputError.value = `${formatCurrency(minimum)} 아래로는 줄일 수 없어요.`
    }

    nextValue = minimum
  }

  if (nextValue > maximum) {
    inputError.value = `최대 ${formatCurrency(maximum)}까지 설정할 수 있어요.`

    nextValue = maximum
  }

  nextValue = Math.round(nextValue)

  amountInput.value = formatNumber(nextValue)

  emit('change', nextValue)

  isEditing.value = false
}

function handleAmountEnter(event: KeyboardEvent) {
  applyAmountInput()

  const target = event.target as HTMLInputElement

  target.blur()
}
</script>

<style scoped>
.pocket-slider {
  width: 100%;

  padding: 22px 18px 20px;

  border-radius: 18px;

  box-sizing: border-box;
}

.pocket-slider--essential {
  background: #eef5ff;
}

.pocket-slider--free {
  background: #fff5ed;
}

.pocket-slider--future {
  background: #fbf0ff;
}

.pocket-slider--emergency {
  background: #eafafa;
}

.pocket-slider__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;

  gap: 16px;
}

.pocket-slider__info {
  display: flex;
  flex-direction: column;

  min-width: 0;

  gap: 8px;
}

.pocket-slider__label {
  display: inline-flex;
  align-items: center;

  width: fit-content;

  padding: 7px 11px;

  border-radius: 999px;

  font-size: 15px;
  font-weight: 700;

  white-space: nowrap;
}

.pocket-slider--essential .pocket-slider__label {
  color: #5c8dcc;
  background: #dceaff;
}

.pocket-slider--free .pocket-slider__label {
  color: #f18746;
  background: #ffe3cf;
}

.pocket-slider--future .pocket-slider__label {
  color: #c56df0;
  background: #efd8ff;
}

.pocket-slider--emergency .pocket-slider__label {
  color: #20aab5;
  background: #c7f1f2;
}

.pocket-slider__minimum {
  font-size: 11px;
  line-height: 1.4;

  white-space: nowrap;
}

.pocket-slider--essential .pocket-slider__minimum {
  color: #719bcf;
}

.pocket-slider--free .pocket-slider__minimum {
  color: #d99162;
}

.pocket-slider--future .pocket-slider__minimum {
  color: #bf83d7;
}

.pocket-slider--emergency .pocket-slider__minimum {
  color: #61abb1;
}

.pocket-slider__amount-wrap {
  display: flex;
  align-items: center;
  justify-content: flex-end;

  flex-shrink: 0;

  width: 150px;

  border-bottom: 1px solid rgba(0, 0, 0, 0.13);
}

.pocket-slider__amount-input {
  width: 100%;
  min-width: 0;

  padding: 4px 3px 10px;

  border: 0;
  outline: none;

  color: #151515;
  background: transparent;

  text-align: right;

  font-size: 20px;
  font-weight: 800;
}

.pocket-slider__unit {
  padding-bottom: 10px;

  color: #151515;

  font-size: 16px;
  font-weight: 700;
}

.pocket-slider__range-area {
  position: relative;

  width: 100%;

  margin-top: 28px;

  padding-bottom: 42px;
}

.pocket-slider__range {
  width: 100%;
  height: 6px;

  margin: 0;

  appearance: none;
  -webkit-appearance: none;

  border: 0;
  border-radius: 999px;

  outline: none;

  /*
   * 잠긴 구간과
   * 조정 가능한 구간을 색으로 구분.
   */
  background: linear-gradient(
    to right,
    #b6bbc3 0%,
    #b6bbc3 var(--used-percent),
    #dedede var(--used-percent),
    #dedede 100%
  );

  cursor: pointer;
}

.pocket-slider__range::-webkit-slider-runnable-track {
  height: 6px;

  border-radius: 999px;

  background: transparent;
}

.pocket-slider__range::-webkit-slider-thumb {
  width: 24px;
  height: 24px;

  margin-top: -9px;

  appearance: none;
  -webkit-appearance: none;

  border: 0;
  border-radius: 50%;

  background: #111111;

  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.18);

  cursor: grab;
}

.pocket-slider__range::-webkit-slider-thumb:active {
  cursor: grabbing;
}

.pocket-slider__range::-moz-range-track {
  height: 6px;

  border-radius: 999px;

  background: linear-gradient(
    to right,
    #b6bbc3 0%,
    #b6bbc3 var(--used-percent),
    #dedede var(--used-percent),
    #dedede 100%
  );
}

.pocket-slider__range::-moz-range-thumb {
  width: 24px;
  height: 24px;

  border: 0;
  border-radius: 50%;

  background: #111111;
}

.pocket-slider__used-marker {
  position: absolute;

  top: -7px;

  left: var(--used-percent);

  transform: translateX(-50%);

  pointer-events: none;
}

.pocket-slider__used-line {
  width: 2px;
  height: 20px;

  margin: 0 auto;

  border-radius: 999px;

  background: #777777;
}

.pocket-slider__used-label {
  position: absolute;

  top: 25px;
  left: 50%;

  display: flex;
  align-items: center;

  gap: 3px;

  transform: translateX(-50%);

  color: #777777;

  font-size: 10px;

  white-space: nowrap;
}

.pocket-slider__lock {
  font-size: 9px;
}

.pocket-slider__range-labels {
  position: absolute;

  top: 18px;
  left: 0;

  display: flex;
  align-items: center;
  justify-content: space-between;

  width: 100%;

  color: #999999;

  font-size: 10px;

  pointer-events: none;
}

.pocket-slider__error {
  margin: 2px 0 0;

  color: #e25454;

  font-size: 11px;
  line-height: 1.45;
}
</style>

<template>
  <section class="pocket-slider" :class="`pocket-slider--${variant}`">
    <div class="pocket-slider__header">
      <div class="pocket-slider__info">
        <span class="pocket-slider__label">
          {{ label }}
        </span>

        <span class="pocket-slider__guide">
          <template v-if="safeMin > 0">
            최소 {{ formatCurrency(safeMin) }} 이상으로 조정 가능
          </template>

          <template v-else-if="safeAllowedMax < safeMax">
            최대 {{ formatCurrency(safeAllowedMax) }}까지 조정 가능
          </template>

          <template v-else> 0원부터 조정 가능 </template>
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

      <div class="pocket-slider__range-labels">
        <span> 0원 </span>

        <span>
          {{ formatCurrency(safeMax) }}
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

  /**
   * 현재 포켓 할당액
   */
  value: number

  /**
   * 실제로 내려갈 수 있는 최소값
   */
  min?: number

  /**
   * 화면에 표시되는 슬라이더의 오른쪽 끝.
   *
   * 모든 포켓에서 총 예산.
   */
  max: number

  /**
   * 실제로 사용자가 올릴 수 있는 최대값.
   *
   * 일반 포켓:
   * 총 예산
   *
   * 비상금:
   * 총예산 - 필수 - 자유 - 미래자산
   */
  allowedMax?: number

  step?: number
}

const props = withDefaults(defineProps<Props>(), {
  min: 0,
  step: 500,
})

const emit = defineEmits<{
  (event: 'change', value: number): void
}>()

const amountInput = ref('')

const inputError = ref('')

const isEditing = ref(false)

/**
 * 주의:
 *
 * watch immediate보다 먼저 정의해야 한다.
 * 이전 코드에서는 아래 함수가 watch 뒤에 있어서
 * 컴포넌트 setup 단계에서 에러가 발생했다.
 */
function formatNumber(value: unknown): string {
  const numberValue = Number(value) || 0

  return Math.round(numberValue).toLocaleString('ko-KR')
}

function formatCurrency(value: unknown): string {
  return `${formatNumber(value)}원`
}

/**
 * 화면상 슬라이더 최대값.
 *
 * 항상 총예산.
 */
const safeMax = computed(() => {
  return Math.max(0, Number(props.max) || 0)
})

/**
 * 실제 최소값.
 */
const safeMin = computed(() => {
  return Math.min(safeMax.value, Math.max(0, Number(props.min) || 0))
})

/**
 * 실제 최대값.
 *
 * allowedMax가 없으면 총예산.
 */
const safeAllowedMax = computed(() => {
  const rawMaximum = props.allowedMax === undefined ? safeMax.value : Number(props.allowedMax) || 0

  return Math.min(safeMax.value, Math.max(safeMin.value, rawMaximum))
})

/**
 * 현재 동그라미 위치.
 *
 * 최소값 아래로 내려갈 수 없고
 * 실제 최대값보다 올라갈 수 없다.
 */
const safeValue = computed(() => {
  const rawValue = Number(props.value) || 0

  return Math.min(safeAllowedMax.value, Math.max(safeMin.value, rawValue))
})

/**
 * 최소값까지의 구간 비율.
 *
 * 이 구간만 진한 색으로 보여준다.
 */
const lockedPercent = computed(() => {
  if (safeMax.value <= 0 || safeMin.value <= 0) {
    return 0
  }

  return Math.min(100, Math.max(0, (safeMin.value / safeMax.value) * 100))
})

const rangeStyle = computed(() => {
  return {
    '--locked-percent': `${lockedPercent.value}%`,
  }
})

/**
 * 현재 할당액과 금액 입력칸 동기화.
 */
watch(
  () => props.value,

  (value) => {
    if (!isEditing.value) {
      amountInput.value = formatNumber(value)
    }
  },

  {
    immediate: true,
  },
)

/**
 * 총예산이나 허용범위가 바뀌었을 때
 * 현재 값이 범위를 벗어나면 자동 보정.
 */
watch(
  [() => props.min, () => props.max, () => props.allowedMax],

  () => {
    if (props.value < safeMin.value) {
      emit('change', safeMin.value)

      return
    }

    if (props.value > safeAllowedMax.value) {
      emit('change', safeAllowedMax.value)
    }
  },
)

const handleSliderInput = (event: Event) => {
  inputError.value = ''

  const target = event.target as HTMLInputElement

  const rawValue = Number(target.value) || 0

  /**
   * 슬라이더 전체 UI는
   * 0 ~ 총예산.
   *
   * 하지만 실제 값은
   * min ~ allowedMax 범위에서만 움직인다.
   */
  const nextValue = Math.min(safeAllowedMax.value, Math.max(safeMin.value, rawValue))

  /**
   * 금지된 구간으로 드래그한 경우
   * thumb를 즉시 허용 위치로 되돌린다.
   */
  target.value = String(nextValue)

  amountInput.value = formatNumber(nextValue)

  emit('change', Math.round(nextValue))
}

const handleAmountFocus = () => {
  isEditing.value = true

  inputError.value = ''
}

const handleAmountInput = (event: Event) => {
  const target = event.target as HTMLInputElement

  const onlyNumbers = target.value.replace(/[^0-9]/g, '')

  if (!onlyNumbers) {
    amountInput.value = ''

    return
  }

  amountInput.value = Number(onlyNumbers).toLocaleString('ko-KR')
}

const applyAmountInput = () => {
  const rawValue = amountInput.value.replace(/,/g, '')

  let nextValue = Number(rawValue)

  if (!Number.isFinite(nextValue)) {
    nextValue = Number(props.value) || 0
  }

  if (nextValue < safeMin.value) {
    inputError.value = `최소 ${formatCurrency(safeMin.value)} 이상으로 설정할 수 있어요.`

    nextValue = safeMin.value
  }

  if (nextValue > safeAllowedMax.value) {
    inputError.value = `최대 ${formatCurrency(safeAllowedMax.value)}까지 설정할 수 있어요.`

    nextValue = safeAllowedMax.value
  }

  nextValue = Math.round(nextValue)

  amountInput.value = formatNumber(nextValue)

  emit('change', nextValue)

  isEditing.value = false
}

const handleAmountEnter = (event: KeyboardEvent) => {
  applyAmountInput()

  const target = event.target as HTMLInputElement

  target.blur()
}
</script>

<style scoped>
.pocket-slider {
  width: 100%;

  padding: 22px 18px 24px;

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

.pocket-slider__guide {
  font-size: 11px;
  line-height: 1.4;

  white-space: nowrap;
}

.pocket-slider--essential .pocket-slider__guide {
  color: #719bcf;
}

.pocket-slider--free .pocket-slider__guide {
  color: #d99162;
}

.pocket-slider--future .pocket-slider__guide {
  color: #bf83d7;
}

.pocket-slider--emergency .pocket-slider__guide {
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

  margin-top: 30px;

  padding-bottom: 24px;
}

.pocket-slider__range {
  display: block;

  width: 100%;
  height: 7px;

  margin: 0;

  appearance: none;
  -webkit-appearance: none;

  border: 0;
  border-radius: 999px;

  outline: none;

  /*
   * 최소값 왼쪽 구간만 진하게.
   *
   * 세로 잠금선이나
   * 현재 달성액 텍스트는 표시하지 않는다.
   */
  background: linear-gradient(
    to right,
    #858c96 0%,
    #858c96 var(--locked-percent),
    #d9d9d9 var(--locked-percent),
    #d9d9d9 100%
  );

  cursor: pointer;
}

.pocket-slider__range::-webkit-slider-runnable-track {
  height: 7px;

  border-radius: 999px;

  background: transparent;
}

.pocket-slider__range::-webkit-slider-thumb {
  width: 26px;
  height: 26px;

  margin-top: -9.5px;

  appearance: none;
  -webkit-appearance: none;

  border: 0;
  border-radius: 50%;

  background: #111111;

  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.18);

  cursor: grab;
}

.pocket-slider__range::-webkit-slider-thumb:active {
  cursor: grabbing;
}

.pocket-slider__range::-moz-range-track {
  height: 7px;

  border-radius: 999px;

  background: linear-gradient(
    to right,
    #858c96 0%,
    #858c96 var(--locked-percent),
    #d9d9d9 var(--locked-percent),
    #d9d9d9 100%
  );
}

.pocket-slider__range::-moz-range-thumb {
  width: 26px;
  height: 26px;

  border: 0;
  border-radius: 50%;

  background: #111111;

  cursor: grab;
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
  margin: 5px 0 0;

  color: #e25454;

  font-size: 11px;
  line-height: 1.45;
}
</style>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

/**
 * 인증번호 6자리 입력 + 카운트다운 타이머.
 *
 * 타이머는 이 컴포넌트가 직접 들고 돈다 — 부모는 `expiresIn`(초)만 넘기면 된다.
 * 0에 닿으면 `expire`를 한 번만 쏘고 멈춘다 (재발송은 부모가 화면에서 처리).
 */
const props = withDefaults(
  defineProps<{
    /** v-model. 항상 숫자만, 최대 6자리 */
    modelValue: string
    /** 인증번호 유효시간(초). 바뀌면(재발송) 타이머를 그 값으로 되돌린다 */
    expiresIn?: number
    error?: string
    disabled?: boolean
  }>(),
  { expiresIn: 180, disabled: false },
)

const emit = defineEmits<{
  'update:modelValue': [string]
  complete: [string]
  expire: []
}>()

const DIGITS = 6
const digits = computed(() => {
  const chars = props.modelValue.split('')
  return Array.from({ length: DIGITS }, (_, i) => chars[i] ?? '')
})

const inputs = ref<HTMLInputElement[]>([])
const setInputRef = (el: unknown, i: number) => {
  if (el) inputs.value[i] = el as HTMLInputElement
}

function focusAt(index: number) {
  nextTick(() => inputs.value[Math.min(Math.max(index, 0), DIGITS - 1)]?.focus())
}

function setDigitAt(index: number, value: string) {
  const next = digits.value.slice()
  next[index] = value
  const joined = next.join('').slice(0, DIGITS)
  emit('update:modelValue', joined)
  if (joined.length === DIGITS) emit('complete', joined)
}

function onInput(index: number, event: Event) {
  const raw = (event.target as HTMLInputElement).value.replace(/\D/g, '')
  if (!raw) {
    setDigitAt(index, '')
    return
  }
  // 붙여넣기로 여러 자리가 한 번에 들어오면 이어서 채운다
  const chars = raw.split('')
  const next = digits.value.slice()
  chars.forEach((char, offset) => {
    if (index + offset < DIGITS) next[index + offset] = char
  })
  const joined = next.join('').slice(0, DIGITS)
  emit('update:modelValue', joined)
  if (joined.length === DIGITS) emit('complete', joined)
  focusAt(index + chars.length)
}

function onKeydown(index: number, event: KeyboardEvent) {
  if (event.key === 'Backspace' && !digits.value[index] && index > 0) {
    focusAt(index - 1)
  }
}

// ── 타이머 ──────────────────────────────────────────
const remaining = ref(props.expiresIn)
let timer: ReturnType<typeof setInterval> | undefined

function startTimer() {
  clearInterval(timer)
  remaining.value = props.expiresIn
  timer = setInterval(() => {
    remaining.value -= 1
    if (remaining.value <= 0) {
      clearInterval(timer)
      emit('expire')
    }
  }, 1000)
}

watch(() => props.expiresIn, startTimer)
onMounted(startTimer)
onBeforeUnmount(() => clearInterval(timer))

const timerLabel = computed(() => {
  const s = Math.max(remaining.value, 0)
  const mm = String(Math.floor(s / 60)).padStart(2, '0')
  const ss = String(s % 60).padStart(2, '0')
  return `${mm}:${ss}`
})

defineExpose({ startTimer })
</script>

<template>
  <div class="otp">
    <div class="row">
      <div class="boxes">
        <input
          v-for="(digit, i) in digits"
          :key="i"
          :ref="(el) => setInputRef(el, i)"
          class="box"
          :class="{ 'is-filled': !!digit, 'is-error': !!error }"
          type="text"
          inputmode="numeric"
          maxlength="1"
          :value="digit"
          :disabled="disabled"
          @input="onInput(i, $event)"
          @keydown="onKeydown(i, $event)"
        />
      </div>
      <span class="timer" :class="{ 'is-done': remaining <= 0 }">{{ timerLabel }}</span>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
  </div>
</template>

<style scoped>
.otp {
  width: 100%;
}

.row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.boxes {
  display: flex;
  flex: 1;
  gap: 8px;
}

.box {
  width: 100%;
  aspect-ratio: 1;
  min-width: 0;
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-primary);
  background-color: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  outline: none;
  transition: border-color 0.15s;
}

.box:focus {
  border-color: var(--color-primary);
}

.box.is-filled {
  border-color: var(--color-primary);
}

.box.is-error {
  border-color: var(--color-danger);
}

.timer {
  flex: none;
  font-size: 15px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--color-danger);
}

.timer.is-done {
  color: var(--color-secondary);
}

.error {
  margin-top: var(--space-sm);
  padding-left: 2px;
  font-size: 13px;
  color: var(--color-danger);
}
</style>

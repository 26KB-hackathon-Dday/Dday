<script setup lang="ts">
/**
 * 금액 입력창. 천단위 콤마를 표시하되 v-model은 순수 숫자(number)로 주고받는다.
 *
 * 주거비·정기수입·자산 등 원 단위 금액을 입력받는 화면에서 공통으로 쓴다.
 */
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    /** v-model. 숫자(원 단위) */
    modelValue: number
    placeholder?: string
    readonly?: boolean
    error?: string
    /** 큰 금액 강조용(모아둔 자산 입력 화면 등) */
    size?: 'md' | 'lg'
    /** 단위 표시. 만원 단위로 입력받는 화면은 '만원'으로 바꿔 쓴다 */
    unit?: string
  }>(),
  { placeholder: '0', readonly: false, size: 'md', unit: '원' },
)

const emit = defineEmits<{ 'update:modelValue': [number] }>()

const display = computed(() =>
  props.modelValue > 0 ? props.modelValue.toLocaleString('ko-KR') : '',
)

function onInput(event: Event) {
  const raw = (event.target as HTMLInputElement).value.replace(/[^0-9]/g, '')
  emit('update:modelValue', raw ? Number(raw) : 0)
}
</script>

<template>
  <div class="amount-field">
    <div
      class="field"
      :class="[`field--${size}`, { 'is-error': !!error, 'is-readonly': readonly }]"
    >
      <input
        class="input"
        type="text"
        inputmode="numeric"
        :value="display"
        :placeholder="placeholder"
        :readonly="readonly"
        @input="onInput"
      />
      <span class="unit">{{ unit }}</span>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<style scoped>
.amount-field {
  width: 100%;
}

.field {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 54px;
  padding: 0 var(--space-md);
  background-color: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.field.is-error {
  border-color: var(--color-danger);
}

.field.is-readonly {
  background-color: var(--color-bg-soft);
}

.field--lg {
  height: 72px;
}

.input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: none;
  outline: none;
  background: transparent;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-primary);
  text-align: right;
}

.field--lg .input {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.input::placeholder {
  color: var(--color-disabled);
  font-weight: 500;
}

.unit {
  flex: none;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-secondary);
}

.field--lg .unit {
  font-size: 18px;
}

.error {
  margin: 6px 2px 0;
  font-size: 13px;
  color: var(--color-danger);
}
</style>

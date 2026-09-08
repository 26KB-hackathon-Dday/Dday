<script setup lang="ts">
import { computed, ref } from 'vue'

/**
 * 공통 입력창.
 *
 * 왼쪽 아이콘은 프로젝트에 아이콘 라이브러리를 새로 들이지 않으려고
 * 인라인 SVG로 두 개만 갖고 있다. `currentColor`를 쓰므로 포커스 색을 따라간다.
 * 다른 모양이 필요하면 `#icon` 슬롯으로 직접 넣는다.
 */
const props = withDefaults(
  defineProps<{
    /** v-model */
    modelValue: string
    type?: 'text' | 'email' | 'password' | 'tel'
    placeholder?: string
    icon?: 'mail' | 'lock'
    /** 문구가 있으면 빨간 테두리 + 아래에 그대로 출력. 정본은 백엔드 message다 */
    error?: string
    disabled?: boolean
    /** 비밀번호일 때 눈 아이콘으로 평문 전환 */
    togglePassword?: boolean
    autocomplete?: string
    inputmode?: 'text' | 'email' | 'numeric' | 'tel'
    maxlength?: number
  }>(),
  { type: 'text', togglePassword: false },
)

const emit = defineEmits<{
  'update:modelValue': [string]
  enter: []
}>()

const focused = ref(false)
const revealed = ref(false)

/** 평문 전환 중이면 text로 바꾼다. `type`을 직접 바인딩하면 IME/자동완성이 꼬인다 */
const inputType = computed(() =>
  props.type === 'password' && revealed.value ? 'text' : props.type,
)

const showToggle = computed(
  () => props.togglePassword && props.type === 'password' && !!props.modelValue,
)

function onInput(event: Event) {
  emit('update:modelValue', (event.target as HTMLInputElement).value)
}
</script>

<template>
  <div class="text-field">
    <div
      class="field"
      :class="{ 'is-focused': focused, 'is-error': !!error, 'is-disabled': disabled }"
    >
      <span v-if="icon || $slots.icon" class="icon" aria-hidden="true">
        <slot name="icon">
          <!-- mail -->
          <svg
            v-if="icon === 'mail'"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <rect x="3" y="5" width="18" height="14" rx="2.5" />
            <path d="m3.5 7 8.5 6 8.5-6" />
          </svg>
          <!-- lock -->
          <svg
            v-else-if="icon === 'lock'"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <rect x="4" y="10" width="16" height="10" rx="2.5" />
            <path d="M8 10V7.5a4 4 0 0 1 8 0V10" />
          </svg>
        </slot>
      </span>

      <input
        class="input"
        :type="inputType"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :autocomplete="autocomplete"
        :inputmode="inputmode"
        :maxlength="maxlength"
        :aria-invalid="!!error"
        @input="onInput"
        @focus="focused = true"
        @blur="focused = false"
        @keyup.enter="emit('enter')"
      />

      <button
        v-if="showToggle"
        class="reveal"
        type="button"
        :aria-label="revealed ? '비밀번호 숨기기' : '비밀번호 보기'"
        @click="revealed = !revealed"
      >
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.7"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12Z" />
          <circle cx="12" cy="12" r="2.75" />
          <path v-if="revealed" d="m4 20 16-16" />
        </svg>
      </button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<style scoped>
.text-field {
  width: 100%;
}

.field {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 54px;
  padding: 0 var(--space-md);
  background-color: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-secondary);
  transition:
    border-color 0.15s,
    color 0.15s;
}

.field.is-focused {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.field.is-error {
  border-color: var(--color-danger);
  color: var(--color-danger);
}

.field.is-disabled {
  background-color: var(--color-bg-soft);
}

.icon,
.reveal {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
}

.icon svg,
.reveal svg {
  width: 20px;
  height: 20px;
}

.reveal {
  color: var(--color-secondary);
  padding: 4px;
  margin-right: -4px;
}

.input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: none;
  outline: none;
  background: transparent;
  /* 16px 미만이면 iOS 사파리가 포커스 때 화면을 확대한다 */
  font-size: 16px;
  color: var(--color-primary);
}

.input::placeholder {
  color: var(--color-secondary);
}

.error {
  margin: 6px 2px 0;
  font-size: 13px;
  color: var(--color-danger);
}
</style>

<script setup lang="ts">
/**
 * 화면 하단에 놓는 주 액션 버튼.
 *
 * `variant`로 생김새만 바꾼다 — 크기와 높이는 어느 화면에서나 같아야
 * 단계를 넘어갈 때 버튼이 튀지 않는다.
 *
 * `fixed`를 켜면 화면 하단에 붙는다. 앱 폭(430px)에 맞춰 가운데 정렬되므로
 * 데스크톱에서도 래퍼 밖으로 나가지 않는다.
 */
withDefaults(
  defineProps<{
    /** solid = 검정 배경 · 흰 글씨 (기본) · ghost = 배경 없는 텍스트 버튼 */
    variant?: 'solid' | 'ghost'
    disabled?: boolean
    /** 처리 중이면 스피너를 띄우고 클릭을 막는다 */
    loading?: boolean
    type?: 'button' | 'submit'
  }>(),
  { variant: 'solid', disabled: false, loading: false, type: 'button' },
)

defineEmits<{ click: [MouseEvent] }>()
</script>

<template>
  <button
    class="primary-button"
    :class="[`primary-button--${variant}`, { 'is-loading': loading }]"
    :type="type"
    :disabled="disabled || loading"
    @click="$emit('click', $event)"
  >
    <span v-if="loading" class="spinner" aria-hidden="true" />
    <slot />
  </button>
</template>

<style scoped>
.primary-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  width: 100%;
  height: 56px;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 600;
  transition:
    background-color 0.15s,
    color 0.15s,
    opacity 0.15s;
}

.primary-button:disabled {
  cursor: default;
}

.primary-button--solid {
  background-color: var(--color-primary);
  color: #ffffff;
}

.primary-button--solid:disabled {
  background-color: var(--color-disabled);
  color: #ffffff;
}

/* 모바일은 hover가 없다. 눌린 느낌은 :active로 준다 */
.primary-button--solid:not(:disabled):active {
  opacity: 0.85;
}

.primary-button--ghost {
  background-color: transparent;
  color: var(--color-secondary);
  font-weight: 500;
}

.primary-button--ghost:not(:disabled):active {
  color: var(--color-primary);
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .spinner {
    animation-duration: 2s;
  }
}
</style>

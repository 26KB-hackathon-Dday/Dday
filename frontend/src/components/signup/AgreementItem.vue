<script setup lang="ts">
/**
 * 약관 동의 한 줄. [필수]/[선택] 배지 + 체크박스 + 상세보기 꺾쇠.
 *
 * 상세 약관 전문 화면은 이번 범위 밖이다 — `detail` 이벤트만 내보내고,
 * 화면이 생기면 부모가 라우팅하면 된다.
 */
defineProps<{
  modelValue: boolean
  label: string
  required?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  detail: []
}>()
</script>

<template>
  <div class="agreement">
    <label class="check">
      <input
        type="checkbox"
        :checked="modelValue"
        @change="emit('update:modelValue', ($event.target as HTMLInputElement).checked)"
      />
      <span class="box" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
          <path d="m5 12.5 4.5 4.5L19 7" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </span>
      <span class="text">
        <span class="tag" :class="{ 'is-required': required }">{{ required ? '필수' : '선택' }}</span>
        {{ label }}
      </span>
    </label>

    <button class="detail" type="button" aria-label="약관 상세보기" @click="emit('detail')">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="m9 5 7 7-7 7" />
      </svg>
    </button>
  </div>
</template>

<style scoped>
.agreement {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
}

.check {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.check input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.box {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: transparent;
  transition:
    background-color 0.15s,
    border-color 0.15s,
    color 0.15s;
}

.box svg {
  width: 12px;
  height: 12px;
}

.check input:checked + .box {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: #ffffff;
}

.check input:focus-visible + .box {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}

.text {
  font-size: 14px;
  color: var(--color-primary);
}

.tag {
  margin-right: 4px;
  font-weight: 700;
  color: var(--color-secondary);
}

.tag.is-required {
  color: var(--color-primary);
}

.detail {
  display: flex;
  padding: 8px;
  margin-right: -8px;
  color: var(--color-secondary);
}

.detail svg {
  width: 16px;
  height: 16px;
}
</style>

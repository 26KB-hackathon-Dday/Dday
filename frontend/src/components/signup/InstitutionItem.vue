<script setup lang="ts">
import type { Institution } from '@/api/mydata'

/**
 * 기관 선택 리스트 한 줄. 좌측 로고 원 + 이름, 우측 체크박스.
 *
 * 로고 이미지가 없으면(`logoUrl` 없음) 이름 첫 글자로 대체한다 — 아이콘 자산을
 * 새로 준비하지 않아도 리스트가 비어 보이지 않는다.
 */
defineProps<{
  institution: Institution
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
}>()
</script>

<template>
  <label class="item">
    <span class="logo">
      <img v-if="institution.logoUrl" :src="institution.logoUrl" :alt="institution.name" />
      <span v-else class="initial">{{ institution.name.slice(0, 1) }}</span>
    </span>

    <span class="name">{{ institution.name }}</span>

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
  </label>
</template>

<style scoped>
.item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  cursor: pointer;
}

.logo {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 6px;
  overflow: hidden;
  background-color: #ffffff;
  border: 1px solid var(--color-border);
  border-radius: 50%;
}

.logo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.initial {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-secondary);
}

.name {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-primary);
}

.item input {
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
  width: 22px;
  height: 22px;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  color: transparent;
  transition:
    background-color 0.15s,
    border-color 0.15s,
    color 0.15s;
}

.box svg {
  width: 13px;
  height: 13px;
}

.item input:checked + .box {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: #ffffff;
}

.item input:focus-visible + .box {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}
</style>

<script setup lang="ts">
import { useRouter } from 'vue-router'

/**
 * 회원가입 6단계 공통 헤더. 뒤로가기 + "n / 6 단계" + 진행 바.
 *
 * 각 단계 화면 맨 위에 놓는다. 뒤로가기는 router.back()만 호출하고
 * 스토어 값은 건드리지 않는다 — 되돌아가도 입력값은 남아 있어야 한다.
 */
const props = defineProps<{
  currentStep: number
  totalSteps: number
}>()

const router = useRouter()
</script>

<template>
  <div class="step-progress">
    <button class="back" type="button" aria-label="뒤로" @click="router.back()">
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="m15 5-7 7 7 7" />
      </svg>
    </button>

    <p class="label">{{ props.currentStep }} / {{ props.totalSteps }} 단계</p>

    <div class="bars">
      <span
        v-for="step in props.totalSteps"
        :key="step"
        class="bar"
        :class="{ 'is-done': step <= props.currentStep }"
      />
    </div>
  </div>
</template>

<style scoped>
.step-progress {
  padding-top: var(--space-sm);
}

.back {
  display: flex;
  padding: 8px;
  margin-left: -8px;
  color: var(--color-primary);
}

.back svg {
  width: 24px;
  height: 24px;
}

.label {
  margin-top: var(--space-sm);
  font-size: 13px;
  font-weight: 700;
  color: var(--color-primary);
}

.bars {
  display: flex;
  gap: 4px;
  margin-top: 8px;
}

.bar {
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background-color: var(--color-border);
}

.bar.is-done {
  background-color: var(--color-primary);
}
</style>

<script setup lang="ts">
/**
 * 공용 확인 모달. `showConfirmDialog`(vant)는 부제목처럼 흐린 보조 문구를 넣을 자리가
 * 없어서, 그 자리가 필요한 화면(로그아웃 등)엔 이 컴포넌트를 쓴다.
 */
withDefaults(
  defineProps<{
    /** v-model */
    modelValue: boolean
    title: string
    /** 제목 아래 흐리게 뜨는 보조 문구. 없으면 생략 */
    description?: string
    confirmText?: string
    cancelText?: string
  }>(),
  { confirmText: '확인', cancelText: '취소' },
)

const emit = defineEmits<{
  'update:modelValue': [boolean]
  confirm: []
  cancel: []
}>()

function confirm() {
  emit('update:modelValue', false)
  emit('confirm')
}

function cancel() {
  emit('update:modelValue', false)
  emit('cancel')
}
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="overlay" @click.self="cancel">
      <div class="card" role="alertdialog" aria-modal="true">
        <p class="title">{{ title }}</p>
        <p v-if="description" class="description">{{ description }}</p>

        <div class="actions">
          <button type="button" class="btn btn--confirm" @click="confirm">
            {{ confirmText }}
          </button>
          <button type="button" class="btn btn--cancel" @click="cancel">
            {{ cancelText }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-page);
  background-color: rgba(0, 0, 0, 0.45);
}

.card {
  width: 100%;
  max-width: 320px;
  padding: 28px var(--space-lg) var(--space-md);
  text-align: center;
  background-color: var(--color-bg);
  border-radius: var(--radius-md);
}

.title {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-primary);
}

.description {
  margin-top: 6px;
  font-size: 13px;
  color: var(--color-secondary);
  opacity: 0.7;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: var(--space-lg);
}

.btn {
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
}

.btn--confirm {
  background-color: var(--color-primary);
  color: #ffffff;
}

.btn--confirm:active {
  opacity: 0.85;
}

.btn--cancel {
  color: var(--color-secondary);
}

.btn--cancel:active {
  color: var(--color-primary);
}
</style>

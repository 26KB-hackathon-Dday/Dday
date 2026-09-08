<script setup lang="ts">
/**
 * 아래에서 올라오는 바텀시트. 배경 딤 클릭·ESC로 닫힌다.
 * 앱 셸이 가운데 정렬된 폰 폭이라, 시트도 셸 폭 안에 맞춰 띄운다.
 */
import { onBeforeUnmount, watch } from 'vue'

const open = defineModel<boolean>({ required: true })

function close() {
  open.value = false
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') close()
}

watch(
  open,
  (isOpen) => {
    document.body.style.overflow = isOpen ? 'hidden' : ''
    if (isOpen) window.addEventListener('keydown', onKey)
    else window.removeEventListener('keydown', onKey)
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  window.removeEventListener('keydown', onKey)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="sheet">
      <div v-if="open" class="sheet-root" role="dialog" aria-modal="true">
        <div class="sheet__backdrop" @click="close" />
        <div class="sheet__panel">
          <div class="sheet__handle" />
          <slot :close="close" />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.sheet-root {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.sheet__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
}

.sheet__panel {
  position: relative;
  width: 100%;
  max-width: var(--app-max);
  margin: 0 auto;
  max-height: 90dvh;
  overflow-y: auto;
  background: var(--c-bg);
  border-radius: 24px 24px 0 0;
  padding-bottom: env(safe-area-inset-bottom);
}

.sheet__handle {
  width: 48px;
  height: 6px;
  margin: 16px auto 8px;
  border-radius: 9999px;
  background: #e2e2e2;
}

/* 진입/이탈: 배경은 페이드, 패널은 슬라이드업 */
.sheet-enter-active,
.sheet-leave-active {
  transition: opacity 0.2s ease;
}
.sheet-enter-active .sheet__panel,
.sheet-leave-active .sheet__panel {
  transition: transform 0.25s ease;
}
.sheet-enter-from,
.sheet-leave-to {
  opacity: 0;
}
.sheet-enter-from .sheet__panel,
.sheet-leave-to .sheet__panel {
  transform: translateY(100%);
}
</style>

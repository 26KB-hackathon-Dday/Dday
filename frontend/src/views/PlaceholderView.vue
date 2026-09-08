<script setup lang="ts">
/** 아직 화면이 없는 탭. 제목은 상단바(라우트 meta)가 보여준다. */
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<template>
  <div class="placeholder">
    <p>{{ route.meta.title }} 화면은 준비 중입니다.</p>
  </div>
</template>

<style scoped>
.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 24px;
  color: var(--c-text-3);
  font-size: 14px;
}
</style>

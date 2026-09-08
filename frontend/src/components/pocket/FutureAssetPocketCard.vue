<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
defineProps<{ title: string; budget: number }>()
const formatWon = (value: number) => `${Math.round(value).toLocaleString('ko-KR')}원`
</script>
<template>
  <article class="card">
    <header>
      <AppIcon name="recommend" :size="20" aria-hidden="true" />
      <h2>{{ title }}</h2>
    </header>
    <div class="progress" aria-hidden="true"><span /></div>
    <dl>
      <div>
        <dt>예산</dt>
        <dd>{{ formatWon(budget) }}</dd>
      </div>
    </dl>
    <!-- TODO: 월별 API가 달성액/평가금액을 제공하면 관련 UI를 표시한다. -->
  </article>
</template>
<style scoped>
.card {
  padding: 18px;
  border-radius: 14px;
  background: #f8f0ff;
}
header {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #a66ee5;
}
h2 {
  color: var(--c-text);
  font-size: 16px;
  font-weight: 700;
}
.progress {
  height: 5px;
  margin-top: 12px;
  overflow: hidden;
  border-radius: 999px;
  background: #dedede;
}
.progress span {
  display: block;
  width: 0;
  height: 100%;
  background: #b17ae9;
}
dl {
  margin-top: 20px;
}
dl div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
dt,
dd {
  color: var(--c-text-2);
  font-size: 12px;
}
dd {
  font-family: var(--font-num);
  white-space: nowrap;
}
</style>

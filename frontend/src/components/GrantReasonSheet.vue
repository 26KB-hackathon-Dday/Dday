<script setup lang="ts">
/**
 * "나에게 추천된 이유" 바텀시트. 홈의 "놓치고 있을 수 있는 지원" 카드에서
 * "왜 추천됐나요?"를 누르면 올라온다. 읽기 전용.
 *
 * 추천 이유는 `/me` 자격판별 결과라 상세 페이지(전체 지원제도와 공유)에는 넣지 않는다.
 */
import type { GrantReason } from '@/api/grant'
import AppIcon from '@/components/AppIcon.vue'
import BottomSheet from '@/components/BottomSheet.vue'

const open = defineModel<boolean>({ required: true })

defineProps<{
  programName: string
  reasons: GrantReason[]
  footnote?: string
}>()
</script>

<template>
  <BottomSheet v-model="open">
    <div class="reason">
      <h3 class="reason__heading">
        <AppIcon name="recommend" :size="20" />
        나에게 추천된 이유
      </h3>
      <p class="reason__program">{{ programName }}</p>

      <ul class="reason__list">
        <li
          v-for="(r, i) in reasons"
          :key="i"
          class="reason__item"
          :class="{ 'reason__item--pending': !r.met }"
        >
          <AppIcon :name="r.met ? 'check-green' : 'dash'" :size="16" class="reason__mark" />
          <div class="reason__text">
            <span class="reason__title">{{ r.title }}</span>
            <span class="reason__detail">{{ r.detail }}</span>
          </div>
        </li>
      </ul>

      <p v-if="footnote" class="reason__footnote">* {{ footnote }}</p>
    </div>
  </BottomSheet>
</template>

<style scoped>
.reason {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 24px 40px;
}
.reason__heading {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 500;
  line-height: 28px;
  letter-spacing: -0.2px;
  color: #000;
}
.reason__program {
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  color: var(--c-text-2);
}
.reason__list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.reason__item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 12px;
  border-radius: 8px;
  background: var(--c-surface);
}
.reason__item--pending {
  padding: 13px;
  background: transparent;
  border: 1px solid var(--c-border);
  opacity: 0.6;
}
.reason__mark {
  margin-top: 4px;
  color: #000;
}
.reason__item--pending .reason__mark {
  color: var(--c-text-2);
}
.reason__text {
  display: flex;
  flex-direction: column;
}
.reason__title {
  font-size: 16px;
  font-weight: 500;
  line-height: 24px;
  color: #000;
}
.reason__detail {
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  color: var(--c-text-2);
}
.reason__footnote {
  text-align: center;
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  color: var(--c-text-2);
}
</style>

<script setup lang="ts">
/**
 * 지원 카드 목록. "놓치고 있을 수 있어요" / "받고 있는 지원" 등에서 같은 레이아웃으로 쓴다.
 * 카드나 "상세 정보 보기"를 누르면 select 이벤트로 id를 올린다.
 * `reasons`가 있는 카드는 "왜 추천됐나요?" 버튼을 띄우고, 누르면 reasons 이벤트를 올린다.
 */
import type { GrantCard } from '@/api/grant'
import AppIcon from '@/components/AppIcon.vue'

defineProps<{ cards: GrantCard[] }>()
defineEmits<{ select: [id: string]; reasons: [id: string] }>()
</script>

<template>
  <ul class="card-list">
    <li v-for="card in cards" :key="card.id" class="grant">
      <button type="button" class="grant__top" @click="$emit('select', card.id)">
        <span class="grant__tags">
          <span v-for="tag in card.tags" :key="tag.label" class="tag" :class="`tag--${tag.tone}`">
            {{ tag.label }}
          </span>
        </span>
        <AppIcon name="chevron-right" :size="12" class="grant__chevron" />
      </button>
      <h4 class="grant__title">{{ card.title }}</h4>
      <p class="grant__amount">{{ card.amountText }}</p>
      <button
        v-if="card.reasons?.length"
        type="button"
        class="grant__why"
        @click="$emit('reasons', card.id)"
      >
        왜 추천됐나요?
        <AppIcon name="chevron-right" :size="11" />
      </button>
      <button type="button" class="grant__detail" @click="$emit('select', card.id)">상세 정보 보기</button>
    </li>
  </ul>
</template>

<style scoped>
.card-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.grant {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 20px;
  border-radius: 12px;
  background: var(--c-surface);
}
.grant__top {
  width: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  text-align: left;
}
.grant__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.grant__chevron {
  color: var(--c-text-2);
  margin-top: 4px;
}
.tag {
  display: inline-flex;
  align-items: center;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  white-space: nowrap;
}
.tag--teal {
  padding: 4px 10px;
  background: rgba(39, 184, 184, 0.1);
  color: var(--c-teal);
}
.tag--blue {
  padding: 4px 10px;
  background: rgba(57, 123, 199, 0.1);
  color: var(--c-blue);
}
.tag--neutral {
  padding: 5px 11px;
  border: 1px solid var(--c-border);
  color: var(--c-text-2);
}
.grant__title {
  margin-top: 8px;
  font-size: 20px;
  font-weight: 500;
  line-height: 28px;
  letter-spacing: -0.2px;
  color: #000;
}
.grant__amount {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 300;
  line-height: 24px;
  color: var(--c-text-2);
}
.grant__why {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin: -4px 0 12px;
  font-size: 13px;
  font-weight: 500;
  line-height: 18px;
  color: var(--c-blue);
}
.grant__why :deep(.app-icon) {
  color: var(--c-blue);
}
.grant__detail {
  height: 48px;
  border: 1px solid var(--c-border);
  border-radius: 8px;
  background: var(--c-bg);
  font-size: 14px;
  font-weight: 500;
  line-height: 18px;
  letter-spacing: 0.14px;
  color: #000;
}
</style>

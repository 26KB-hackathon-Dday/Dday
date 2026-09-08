<script setup lang="ts">
/**
 * "확인 필요한 지원" 되묻기 바텀시트. 큐를 하나씩 보여주고,
 * 맞아요/아니에요를 받으면 다음 항목으로 넘어간다. 마지막이면 닫힌다.
 *
 * 실제 반영(수급 상태 변경)은 백엔드 API가 없어 아직 이벤트만 올린다.
 */
import { computed, ref, watch } from 'vue'
import type { GrantReviewItem } from '@/api/grant'
import AppIcon from '@/components/AppIcon.vue'
import BottomSheet from '@/components/BottomSheet.vue'

const open = defineModel<boolean>({ required: true })

const props = defineProps<{ items: GrantReviewItem[] }>()
const emit = defineEmits<{ answer: [item: GrantReviewItem, receiving: boolean] }>()

const index = ref(0)
watch(open, (isOpen) => {
  if (isOpen) index.value = 0
})

const current = computed(() => props.items[index.value])
const total = computed(() => props.items.length)

function answer(receiving: boolean) {
  const item = current.value
  if (!item) return
  emit('answer', item, receiving)
  if (index.value + 1 < total.value) index.value += 1
  else open.value = false
}
</script>

<template>
  <BottomSheet v-model="open">
    <div v-if="current" class="review">
      <div class="review__header">
        <span class="review__progress">{{ index + 1 }} / {{ total }}</span>
        <p class="review__question">
          <b>{{ current.programName }} {{ current.amountText }}</b>
          수급 중인 것으로 보여요. 맞나요?
        </p>
      </div>

      <div class="review__note">
        <AppIcon name="info" :size="20" class="review__note-icon" />
        <p>
          <span class="review__note-plain">'아니에요'를 선택하시면 </span>
          <strong>아직 받고 있지 않아요</strong>
          <span class="review__note-plain">
            상태로 변경되어, 앞으로 수급 관련 안내를 받으실 수 있습니다.
          </span>
        </p>
      </div>

      <div class="review__actions">
        <button type="button" class="review__btn review__btn--primary" @click="answer(true)">
          맞아요
        </button>
        <button type="button" class="review__btn review__btn--ghost" @click="answer(false)">
          아니에요
        </button>
      </div>
    </div>
  </BottomSheet>
</template>

<style scoped>
.review {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 16px 24px 40px;
}

.review__header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}
.review__progress {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 66px;
  height: 42px;
  padding: 0 17px;
  border: 1px solid var(--c-border);
  border-radius: 9999px;
  background: var(--c-surface);
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: #1c1f20;
}
.review__question {
  text-align: center;
  font-size: 20px;
  font-weight: 500;
  line-height: 28px;
  letter-spacing: -0.2px;
  color: #171717;
}
.review__question b {
  font-weight: 400;
  color: #000;
}

.review__note {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 17px;
  border: 1px solid var(--c-border);
  border-radius: 12px;
  background: var(--c-surface);
  font-size: 14px;
  line-height: 20px;
}
.review__note-icon {
  flex-shrink: 0;
  margin-top: 1px;
  color: #747878;
}
.review__note-plain {
  color: var(--c-text-2);
}
.review__note strong {
  font-weight: 500;
  color: #171717;
}

.review__actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 8px;
}
.review__btn {
  height: 56px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  line-height: 18px;
  letter-spacing: 0.14px;
}
.review__btn--primary {
  background: #000;
  color: #fff;
}
.review__btn--ghost {
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  color: #171717;
}
</style>

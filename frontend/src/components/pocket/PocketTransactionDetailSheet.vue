<script setup lang="ts">
import { computed, watch } from 'vue'
import BottomSheet from '@/components/BottomSheet.vue'
import type { PocketCategory, PocketTransactionDetail } from '@/api/pocket'
import { formatWon } from '@/utils/format'

const props = defineProps<{
  detail: PocketTransactionDetail | null
  categories: PocketCategory[]
  loading: boolean
  saving: boolean
  error: string | null
}>()
const emit = defineEmits<{ retry: []; save: [categoryId: number] }>()
const open = defineModel<boolean>({ required: true })
const selectedCategoryId = defineModel<number | null>('categoryId', { required: true })

const title = computed(() => props.detail?.merchantName || props.detail?.memo || '거래 내역')
const dateLabel = computed(() => {
  if (!props.detail) return ''
  const date = new Date(props.detail.transactionAt)
  if (Number.isNaN(date.getTime())) return props.detail.transactionAt
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
})
const sourceLabel = computed(() => (props.detail?.sourceType === 'CARD' ? '카드' : '계좌'))
const canSave = computed(
  () =>
    !!props.detail &&
    selectedCategoryId.value != null &&
    selectedCategoryId.value !== props.detail.category?.categoryId &&
    !props.saving,
)

watch(
  () => props.detail,
  (detail) => {
    selectedCategoryId.value = detail?.category?.categoryId ?? null
  },
)
</script>

<template>
  <BottomSheet v-model="open">
    <section class="transaction-sheet" aria-labelledby="transaction-sheet-title">
      <header>
        <div>
          <p>거래 상세</p>
          <h2 id="transaction-sheet-title">{{ title }}</h2>
        </div>
        <button type="button" class="close" aria-label="거래 상세 닫기" @click="open = false">
          ×
        </button>
      </header>

      <p v-if="loading" class="state" role="status">거래 정보를 불러오는 중…</p>
      <div v-else-if="error && !detail" class="state state--error" role="alert">
        <p>{{ error }}</p>
        <button type="button" @click="emit('retry')">다시 시도</button>
      </div>
      <template v-else-if="detail">
        <strong class="amount">-{{ formatWon(detail.amount) }}</strong>
        <dl class="details">
          <div>
            <dt>거래 일시</dt>
            <dd>{{ dateLabel }}</dd>
          </div>
          <div>
            <dt>거래 수단</dt>
            <dd>{{ sourceLabel }}</dd>
          </div>
          <div>
            <dt>포켓</dt>
            <dd>{{ detail.pocket?.pocketName ?? '미분류' }}</dd>
          </div>
          <div>
            <dt>현재 카테고리</dt>
            <dd>{{ detail.category?.categoryName ?? '미분류' }}</dd>
          </div>
        </dl>

        <fieldset>
          <legend>카테고리 수정</legend>
          <label v-for="category in categories" :key="category.categoryId">
            <input v-model="selectedCategoryId" type="radio" :value="category.categoryId" />
            <span>{{ category.categoryName }}</span>
          </label>
        </fieldset>

        <p v-if="error" class="save-error" role="alert">{{ error }}</p>

        <button
          type="button"
          class="save"
          :disabled="!canSave"
          @click="selectedCategoryId != null && emit('save', selectedCategoryId)"
        >
          {{ saving ? '저장 중…' : '카테고리 저장' }}
        </button>
      </template>
    </section>
  </BottomSheet>
</template>

<style scoped>
.transaction-sheet {
  padding: 8px 22px 24px;
}
header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
header p {
  color: var(--c-text-3);
  font-size: 12px;
}
h2 {
  margin-top: 3px;
  font-size: 20px;
  font-weight: 700;
}
.close {
  padding: 0 4px;
  color: var(--c-text-2);
  font-size: 28px;
  line-height: 1;
}
.state {
  display: grid;
  min-height: 220px;
  place-items: center;
  color: var(--c-text-3);
}
.state--error {
  align-content: center;
  gap: 12px;
  color: var(--c-danger);
  text-align: center;
}
.state--error button {
  color: var(--c-text);
  font-weight: 600;
}
.amount {
  display: block;
  margin-top: 24px;
  font-family: var(--font-num);
  font-size: 28px;
}
.details {
  display: grid;
  gap: 12px;
  margin-top: 22px;
  padding: 16px;
  border-radius: 12px;
  background: var(--c-surface);
}
.details div {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}
.details dt {
  color: var(--c-text-3);
  font-size: 12px;
}
.details dd {
  min-width: 0;
  text-align: right;
  font-size: 12px;
  overflow-wrap: anywhere;
}
fieldset {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 24px;
  border: 0;
}
legend {
  grid-column: 1 / -1;
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: 700;
}
label {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  padding: 11px;
  border-radius: 10px;
  background: var(--c-surface);
  font-size: 12px;
}
label span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
input {
  accent-color: var(--c-blue);
}
.save {
  width: 100%;
  margin-top: 22px;
  padding: 14px;
  border-radius: 12px;
  background: var(--c-text);
  color: var(--c-bg);
  font-size: 14px;
  font-weight: 700;
}
.save-error {
  margin-top: 14px;
  color: var(--c-danger);
  font-size: 12px;
  text-align: center;
}
.save:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}
@media (max-width: 340px) {
  .transaction-sheet {
    padding-inline: 16px;
  }
}
</style>

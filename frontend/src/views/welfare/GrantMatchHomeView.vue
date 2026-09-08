<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { grantApi, type GrantCard, type GrantHome, type GrantReviewItem } from '@/api/grant'
import checkIcon from '@/assets/icons/check.svg'
import AppIcon from '@/components/AppIcon.vue'
import GrantCardList from '@/components/GrantCardList.vue'
import GrantReasonSheet from '@/components/GrantReasonSheet.vue'
import GrantReviewSheet from '@/components/GrantReviewSheet.vue'

const router = useRouter()
const route = useRoute()
const home = ref<GrantHome | null>(null)
const reviewOpen = ref(false)

const reasonOpen = ref(false)
const reasonCard = ref<GrantCard | null>(null)

/**
 * 카드 목록 섹션이 어떤 벤토 셀을 비추는지. 벤토 셀 클릭으로 바뀐다.
 * 쿼리(`?view=receiving`)에 담아, 상세를 봤다 돌아와도 유지된다.
 */
const section = computed<'missing' | 'receiving'>(() =>
  route.query.view === 'receiving' ? 'receiving' : 'missing',
)
function showSection(view: 'missing' | 'receiving') {
  router.replace({ query: view === 'missing' ? {} : { view } })
}

const sectionCards = computed(() =>
  section.value === 'missing' ? (home.value?.missing ?? []) : (home.value?.receivingList ?? []),
)

onMounted(async () => {
  home.value = await grantApi.fetchHome()
})

function openDetail(id: string) {
  router.push(`/grants/${id}`)
}

function openReview() {
  if (home.value?.reviewQueue.length) reviewOpen.value = true
}

function openReasons(id: string) {
  const card = home.value?.missing.find((c) => c.id === id)
  if (card?.reasons?.length) {
    reasonCard.value = card
    reasonOpen.value = true
  }
}

// 답변마다 상태만 보내고 쌓아둔다. 홈 재조회는 시트가 닫힐 때 한 번만 —
// 답변마다 다시 받으면 reviewQueue가 줄면서 시트가 인덱싱 중인 목록이 흔들려 항목이 건너뛰어진다.
let pendingAnswers: Promise<void>[] = []

function onReviewAnswer(item: GrantReviewItem, receiving: boolean) {
  pendingAnswers.push(grantApi.updateReceivingStatus(item.id, receiving))
}

watch(reviewOpen, async (isOpen, wasOpen) => {
  if (!wasOpen || isOpen) return
  // 마지막 답변까지 반영된 뒤 재조회 (닫힘 트리거와 마지막 PATCH가 경쟁하지 않도록)
  await Promise.allSettled(pendingAnswers)
  pendingAnswers = []
  home.value = await grantApi.fetchHome()
})
</script>

<template>
  <div v-if="home" class="page">
    <!-- 인트로 -->
    <section class="intro">
      <h2 class="intro__title">놓치고 있는 지원을 확인해보세요</h2>
      <p class="intro__updated">
        <AppIcon name="clock" :size="12" />
        마지막 업데이트: {{ home.summary.lastUpdated }}
      </p>
    </section>

    <!-- 요약 (벤토) -->
    <section class="bento">
      <div class="cell cell--wide">
        <span class="cell__label">확인된 지원</span>
        <div class="cell__row">
          <p class="stat">
            <span class="stat__num stat__num--xl">{{ home.summary.confirmed }}</span>
            <span class="stat__unit stat__unit--lg">건</span>
          </p>
          <img :src="checkIcon" alt="" class="cell__badge" />
        </div>
      </div>

      <button
        type="button"
        class="cell cell--button"
        :class="{ 'cell--active': section === 'receiving' }"
        :aria-pressed="section === 'receiving'"
        @click="showSection('receiving')"
      >
        <span class="cell__label">받고 있는 지원</span>
        <p class="stat">
          <span class="stat__num">{{ home.summary.receiving }}</span>
          <span class="stat__unit">건</span>
        </p>
      </button>

      <button
        type="button"
        class="cell cell--button cell--alert"
        :class="{ 'cell--active': section === 'missing' }"
        :aria-pressed="section === 'missing'"
        @click="showSection('missing')"
      >
        <span class="cell__label cell__label--danger">놓치고 있을 수 있는 지원</span>
        <p class="stat">
          <span class="stat__num">{{ home.summary.actionNeeded }}</span>
          <span class="stat__unit">건</span>
        </p>
      </button>

      <div class="cell cell--wide cell--split">
        <div>
          <span class="cell__label">확인 필요한 지원</span>
          <p class="stat">
            <span class="stat__num">{{ home.summary.needsReview }}</span>
            <span class="stat__unit">건</span>
          </p>
        </div>
        <button type="button" class="pill" @click="openReview">확인하기</button>
      </div>
    </section>

    <!-- 카드 목록 — 벤토 셀 클릭에 따라 '놓치고 있을 수 있어요' / '받고 있는 지원' 교체 -->
    <section class="cards">
      <div class="cards__head">
        <h3 class="cards__title">
          {{ section === 'missing' ? '놓치고 있을 수 있어요' : '받고 있는 지원' }}
        </h3>
        <span v-if="section === 'missing'" class="cards__dot" />
        <span v-else class="cards__count">{{ home.receivingList.length }}건</span>
      </div>

      <GrantCardList :cards="sectionCards" @select="openDetail" @reasons="openReasons" />

      <button type="button" class="cta" @click="router.push('/grants/all')">
        전체 지원제도 보러가기
      </button>
    </section>

    <GrantReviewSheet v-model="reviewOpen" :items="home.reviewQueue" @answer="onReviewAnswer" />

    <GrantReasonSheet
      v-if="reasonCard"
      v-model="reasonOpen"
      :program-name="reasonCard.title"
      :reasons="reasonCard.reasons ?? []"
      :footnote="reasonCard.reasonFootnote"
    />
  </div>
</template>

<style scoped>
.page {
  padding-bottom: 40px;
}

/* ── 인트로 ── */
.intro {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 32px 24px 24px;
}
.intro__title {
  font-size: 24px;
  font-weight: 500;
  line-height: 32px;
  letter-spacing: -0.52px;
  color: #000;
}
.intro__updated {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 300;
  line-height: 20px;
  color: var(--c-text-2);
}

/* ── 벤토 요약 ── */
.bento {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin: 0 24px;
}
.cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px;
  border-radius: 12px;
  background: var(--c-surface);
}
.cell--wide {
  grid-column: 1 / -1;
}
.cell--alert {
  position: relative;
  overflow: hidden;
}
.cell--alert::before {
  content: '';
  position: absolute;
  inset: 1px;
  background: linear-gradient(149deg, rgba(255, 218, 214, 0.2) 0%, rgba(255, 218, 214, 0) 100%);
  pointer-events: none;
}
.cell--split {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  min-height: 80px;
}
.cell__label {
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  color: var(--c-text-2);
}
.cell__label--danger {
  color: var(--c-danger);
  position: relative;
}
.cell__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cell__badge {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  object-fit: contain;
}

/* ── 숫자 + 단위 ── */
.stat {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
}
.stat__num {
  font-family: var(--font-num);
  font-weight: 600;
  font-size: 20px;
  line-height: 28px;
  letter-spacing: -0.2px;
  color: #000;
}
.stat__num--xl {
  font-weight: 700;
  font-size: 32px;
  line-height: 40px;
  letter-spacing: -0.64px;
}
.stat__unit {
  font-size: 16px;
  line-height: 24px;
  font-weight: 500;
  color: var(--c-text-2);
}
.stat__unit--lg {
  font-size: 20px;
  line-height: 28px;
}

.pill {
  flex-shrink: 0;
  padding: 9px 17px;
  border: 1px solid var(--c-border);
  border-radius: 9999px;
  background: var(--c-bg);
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  color: #000;
}

/* ── 카드 목록 섹션 ── */
.cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 40px 24px 0;
}
.cards__head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.cards__title {
  font-size: 20px;
  font-weight: 500;
  line-height: 28px;
  letter-spacing: -0.2px;
  color: #000;
}
.cards__dot {
  width: 8px;
  height: 8px;
  border-radius: 9999px;
  background: var(--c-danger);
}
.cards__count {
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text-3);
  font-variant-numeric: tabular-nums;
}

.cell--button {
  align-items: flex-start;
  text-align: left;
  font: inherit;
}
.cell--active {
  outline: 1.5px solid #000;
  outline-offset: -1.5px;
}

.cta {
  align-self: center;
  width: 320px;
  max-width: 100%;
  height: 56px;
  margin-top: 24px;
  border-radius: 8px; /* /grants 주 액션 버튼과 동일 (상세·목록 버튼도 8px) */
  background: #000;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  line-height: 18px;
  letter-spacing: 0.14px;
}
</style>

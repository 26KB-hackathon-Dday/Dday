<script setup lang="ts">
/**
 * 납부 기록 — 통신요금·건강보험료·국민연금을 제때 냈는지 보여준다.
 *
 * 타임라인은 **실제로 낸 달**로 묶는다. 청구월과 납부월은 한 달 차이가 나는데
 * (9월분 통신요금의 기한은 10월 25일), 이 화면은 "성실 납부 기록"이라 언제 냈는지가
 * 기준이다. 청구월로 묶으면 구간 제목과 날짜의 달이 어긋나 보인다.
 *
 * 미납은 낸 날이 없으므로 납부기한이 속한 달로 묶는다.
 */
import { computed, onMounted, ref } from 'vue'
import { creditApi, type PaymentHistory, type PaymentType } from '@/api/credit'
import { ApiError } from '@/api/types'

/** 처음 보여줄 달 수와 "더보기" 한 번에 늘어나는 달 수. */
const MONTH_PAGE = 3

const history = ref<PaymentHistory | null>(null)
const errorMessage = ref('')
const visibleMonths = ref(MONTH_PAGE)

onMounted(async () => {
  try {
    history.value = await creditApi.fetchPaymentHistory()
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '납부 기록을 불러오지 못했습니다.'
  }
})

const types = computed(() => history.value?.types ?? [])

/**
 * 연속 납부가 조회 기간을 다 채웠는지. 채운 종류만 강조한다 —
 * 한 번이라도 끊긴 종류를 같은 무게로 보여주면 "연속"이라는 말이 무의미해진다.
 */
function isPerfect(streak: number): boolean {
  return history.value != null && streak >= history.value.monthsCovered
}

interface TimelineItem {
  key: string
  label: string
  statusLabel: string
  dateText: string
  paymentType: PaymentType
}

interface TimelineMonth {
  key: string
  monthLabel: string
  items: TimelineItem[]
}

/** "2026-09-24" → "26.09.24" */
function toShortDate(date: string): string {
  return date.slice(2).replaceAll('-', '.')
}

const timeline = computed<TimelineMonth[]>(() => {
  const months = new Map<string, TimelineItem[]>()

  // types는 서버가 정한 표시 순서다. 그 순서로 훑어야 한 달 안의 순서도 화면마다 같다.
  types.value.forEach((type) => {
    type.records.forEach((record) => {
      const settledOn = record.paidDate ?? record.dueDate
      const monthKey = settledOn.slice(0, 7)
      const items = months.get(monthKey) ?? []
      items.push({
        key: `${type.paymentType}-${record.billingMonth}`,
        label: type.label,
        statusLabel: record.statusLabel,
        dateText: toShortDate(settledOn),
        paymentType: type.paymentType,
      })
      months.set(monthKey, items)
    })
  })

  return [...months.entries()]
    .sort(([a], [b]) => b.localeCompare(a))
    .map(([key, items]) => ({
      key,
      monthLabel: `${Number(key.slice(5))}월`,
      items,
    }))
})

const visibleTimeline = computed(() => timeline.value.slice(0, visibleMonths.value))
const hasMore = computed(() => timeline.value.length > visibleMonths.value)

function showMore() {
  visibleMonths.value += MONTH_PAGE
}

/** 종류별 배지 색. 전부 코드베이스에 이미 있는 값이다. */
const TYPE_COLORS: Record<PaymentType, string> = {
  HEALTH_INSURANCE: '#22a9ad',
  NATIONAL_PENSION: '#ef8e3d',
  TELECOM: 'var(--c-blue)',
}

/** 원형 배지 안 글리프. 대응하는 아이콘 에셋이 없어 직접 그린다. */
const TYPE_PATHS: Record<PaymentType, string[]> = {
  // 하트 — 건강. 십자는 병원 표시로 읽히고, 20px에서 획이 둘뿐이라 아이콘으로 안 보인다.
  HEALTH_INSURANCE: [
    'M12 17.6 6.6 12.2a3.4 3.4 0 0 1 0-4.8 3.4 3.4 0 0 1 4.8 0l.6.6.6-.6a3.4 3.4 0 0 1 4.8 0 3.4 3.4 0 0 1 0 4.8L12 17.6Z',
  ],
  // 우산 — 노후 보장. 동전·저금통은 이 크기에서 통신요금 아이콘과 구별이 안 된다.
  NATIONAL_PENSION: ['M5.6 12.6a6.4 6.4 0 0 1 12.8 0Z', 'M12 12.6v4a1.7 1.7 0 0 0 3.4 0'],
  // 휴대폰 — 통신. 전파 모양은 와이파이로 읽혀서 "요금"과 이어지지 않는다.
  TELECOM: [
    'M9.6 5.2h4.8a1.4 1.4 0 0 1 1.4 1.4v10.8a1.4 1.4 0 0 1-1.4 1.4H9.6a1.4 1.4 0 0 1-1.4-1.4V6.6a1.4 1.4 0 0 1 1.4-1.4Z',
    'M10.9 16.6h2.2',
  ],
}
</script>

<template>
  <div class="page">
    <p v-if="errorMessage" class="page__error">{{ errorMessage }}</p>

    <template v-else-if="history">
      <p v-if="!types.length" class="page__empty">
        아직 납부 기록이 없어요.<br />
        통신요금·건강보험료·국민연금 납부 이력이 모이면 보여드릴게요.
      </p>

      <template v-else>
        <h2 class="section-title">최근 {{ history.monthsCovered }}개월 납부 기록</h2>

        <!-- 종류별 요약 -->
        <section class="summary">
          <div
            v-for="type in types"
            :key="type.paymentType"
            class="summary__row"
            :class="{ 'is-dim': !isPerfect(type.onTimeStreak) }"
          >
            <span
              class="summary__icon"
              :style="{ background: TYPE_COLORS[type.paymentType] }"
              aria-hidden="true"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.9"
                   stroke-linecap="round" stroke-linejoin="round">
                <path v-for="d in TYPE_PATHS[type.paymentType]" :key="d" :d="d" />
              </svg>
            </span>

            <span class="summary__label">{{ type.label }}</span>

            <span class="summary__badge" :class="{ 'is-perfect': isPerfect(type.onTimeStreak) }">
              <svg
                v-if="isPerfect(type.onTimeStreak)"
                class="summary__check"
                viewBox="0 0 24 24"
                fill="currentColor"
                aria-hidden="true"
              >
                <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm-1.2 14.3-4-4 1.4-1.4 2.6 2.6 5.6-5.6 1.4 1.4-7 7z" />
              </svg>
              {{ type.onTimeStreak }}개월 연속 완납
            </span>
          </div>
        </section>

        <!-- 납부한 달 기준 타임라인 -->
        <ol class="timeline">
          <li v-for="month in visibleTimeline" :key="month.key" class="timeline__month">
            <div class="timeline__marker">
              <span class="timeline__label">{{ month.monthLabel }}</span>
              <span class="timeline__dot" aria-hidden="true" />
            </div>

            <ul class="timeline__records">
              <li v-for="item in month.items" :key="item.key" class="record">
                <div class="record__body">
                  <p class="record__type">{{ item.label }}</p>
                  <p class="record__status">{{ item.statusLabel }}</p>
                </div>
                <span class="record__date">{{ item.dateText }}</span>
              </li>
            </ul>
          </li>
        </ol>

        <button v-if="hasMore" type="button" class="more" @click="showMore">
          이전 기록 더보기
        </button>
      </template>
    </template>
  </div>
</template>

<style scoped>
.page {
  padding: 8px 20px 32px;
}

.page__error,
.page__empty {
  padding: 48px 0;
  text-align: center;
  font-size: 14px;
  line-height: 1.6;
  color: var(--c-text-3);
}

.section-title {
  margin-bottom: 12px;
  font-size: 17px;
  font-weight: 700;
}

/* ── 종류별 요약 ─────────────────────────────────────────────────── */

.summary {
  padding: 4px 16px;
  background: var(--c-surface);
  border-radius: 16px;
}

.summary__row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
}

.summary__row + .summary__row {
  border-top: 1px solid var(--c-border);
}

/* 연속이 끊긴 종류는 눌러서 보여준다. 같은 무게면 "연속"이 무의미해진다. */
.summary__row.is-dim {
  opacity: 0.45;
}

.summary__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 9999px;
}

.summary__icon svg {
  width: 20px;
  height: 20px;
}

.summary__label {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  font-weight: 600;
}

.summary__badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-text-3);
}

.summary__badge.is-perfect {
  color: #15936f;
}

.summary__check {
  width: 14px;
  height: 14px;
}

/* ── 타임라인 ────────────────────────────────────────────────────── */

.timeline {
  position: relative;
  margin-top: 28px;
  list-style: none;
}

/*
 * 세로선은 마커 열(30px) 가운데를 지난다 — 점의 중심과 같은 x다.
 * 첫 점에서 시작해 마지막 달 아래로는 흐르지 않게 잘라둔다.
 */
.timeline::before {
  content: '';
  position: absolute;
  top: 32px;
  bottom: 16px;
  left: 15px;
  width: 1px;
  background: var(--c-border);
}

.timeline__month {
  display: flex;
  gap: 12px;
}

.timeline__month + .timeline__month {
  margin-top: 4px;
}

.timeline__marker {
  position: relative;
  flex-shrink: 0;
  width: 30px;
  padding-top: 6px;
  text-align: right;
}

.timeline__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text-2);
}

.timeline__dot {
  position: absolute;
  top: 26px;
  left: 50%;
  /* 지름을 짝수로 둬야 세로선(1px) 중심과 정확히 맞는다. */
  width: 8px;
  height: 8px;
  margin-left: -4px;
  border-radius: 9999px;
  background: #171717;
}

/* 첫 달만 진하게 — 최근 기록이 어디인지 눈이 먼저 가야 한다. */
.timeline__month:not(:first-child) .timeline__dot {
  background: var(--c-border);
}

.timeline__records {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  list-style: none;
}

.record {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: 12px;
}

.record__body {
  flex: 1;
  min-width: 0;
}

.record__type {
  font-size: 13px;
  color: var(--c-text-3);
}

.record__status {
  margin-top: 2px;
  font-size: 15px;
  font-weight: 700;
}

.record__date {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--c-text-2);
}

/* ── 더보기 ──────────────────────────────────────────────────────── */

.more {
  width: 100%;
  margin-top: 20px;
  padding: 16px;
  border-radius: 12px;
  background: var(--c-surface);
  font-size: 15px;
  font-weight: 500;
  color: var(--c-text-2);
}
</style>

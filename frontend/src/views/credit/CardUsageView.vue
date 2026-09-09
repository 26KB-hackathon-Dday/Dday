<script setup lang="ts">
/**
 * 카드 사용 현황 — 신용카드 한도 대비 이용률.
 *
 * 체크·선불카드는 목록에 없다. 한도라는 개념이 없어 이용률을 낼 수 없고, 그 판단은
 * 서버가 한다(응답에 애초에 담기지 않는다).
 *
 * 하단 수치는 **총 사용액 ÷ 총 한도**다. 카드별 비율의 단순 평균이 아니다 —
 * 평균을 내면 한도가 큰 카드가 과소평가돼 실제보다 낮게 보인다.
 */
import { computed, onMounted, ref } from 'vue'
import { creditApi, type CardUsage } from '@/api/credit'
import { ApiError } from '@/api/types'

/**
 * 이 선을 넘으면 경고색으로 바꾼다. 화면 상단 안내 문구의 "30~50% 이내"와 같은 기준이라
 * 한쪽만 고치면 안내와 색이 어긋난다.
 */
const WARNING_THRESHOLD = 50

const usage = ref<CardUsage | null>(null)
const errorMessage = ref('')

onMounted(async () => {
  try {
    usage.value = await creditApi.fetchCardUsage()
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '카드 사용 현황을 불러오지 못했습니다.'
  }
})

const cards = computed(() => usage.value?.cards ?? [])

/** 원 → 만원. 소수 첫째 자리까지 쓰고 `.0`이면 정수로 둔다. */
function toManwon(won: number): string {
  const manwon = Math.round((won / 10_000) * 10) / 10
  return manwon.toLocaleString('ko-KR', { maximumFractionDigits: 1 })
}

/** 서버는 소수 한 자리로 준다. 시안은 정수라 뒤의 `.0`만 뗀다. */
function toPercent(rate: number): string {
  return String(rate).replace(/\.0$/, '')
}

function isWarning(rate: number): boolean {
  return rate > WARNING_THRESHOLD
}

/** 막대 길이. 한도를 넘겨도 막대가 칸 밖으로 나가지 않게 100%에서 자른다. */
function barWidth(rate: number): string {
  return `${Math.min(rate, 100)}%`
}
</script>

<template>
  <div class="page">
    <p v-if="errorMessage" class="page__error">{{ errorMessage }}</p>

    <template v-else-if="usage">
      <p v-if="!cards.length" class="page__empty">
        연동된 신용카드가 없어요.<br />
        체크카드는 한도가 없어 사용률을 계산할 수 없어요.
      </p>

      <template v-else>
        <aside class="tip">
          <span class="tip__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                 stroke-linecap="round" stroke-linejoin="round">
              <path d="M9.5 17.5h5" />
              <path d="M10 20.5h4" />
              <path d="M12 3.5a5.5 5.5 0 0 0-3.2 9.98c.5.36.8.94.8 1.55v.47h4.8v-.47c0-.61.3-1.19.8-1.55A5.5 5.5 0 0 0 12 3.5z" />
            </svg>
          </span>
          <p class="tip__text">
            신용카드 한도의 {{ WARNING_THRESHOLD - 20 }}~{{ WARNING_THRESHOLD }}% 이내를 유지하는 것이
            신용점수 관리에 좋습니다.
          </p>
        </aside>

        <h2 class="section-title">보유 카드별 현황</h2>

        <ul class="cards">
          <li v-for="card in cards" :key="card.cardId" class="card">
            <div class="card__head">
              <p class="card__name">{{ card.cardName }}</p>
              <strong class="card__rate" :class="{ 'is-warning': isWarning(card.utilization) }">
                {{ toPercent(card.utilization) }}% 사용
              </strong>
            </div>

            <div
              class="card__bar"
              role="img"
              :aria-label="`한도의 ${toPercent(card.utilization)}퍼센트 사용`"
            >
              <span
                class="card__fill"
                :class="{ 'is-warning': isWarning(card.utilization) }"
                :style="{ width: barWidth(card.utilization) }"
              />
            </div>

            <div class="card__foot">
              <p class="card__usage">사용 <strong>{{ toManwon(card.usage) }}만원</strong></p>
              <p class="card__limit">한도 {{ toManwon(card.creditLimit) }}만원</p>
            </div>
          </li>
        </ul>

        <!--
          카드별 비율의 평균이 아니라 총 사용액 ÷ 총 한도다. 평균을 내면 한도가 큰 카드가
          과소평가돼 실제보다 낮게 보인다. 그래서 "평균"이 아니라 "전체 사용률"이라 적는다.
        -->
        <section class="total">
          <span class="total__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                 stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 16.5l5-5 3.5 3.5L20 8" />
              <path d="M15 8h5v5" />
            </svg>
          </span>
          <p class="total__label">전체 사용률</p>
          <strong class="total__value" :class="{ 'is-warning': isWarning(usage.currentUtilization ?? 0) }">
            {{ toPercent(usage.currentUtilization ?? 0) }}%
          </strong>
        </section>
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

/* ── 안내 ────────────────────────────────────────────────────────── */

.tip {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 16px;
  border-radius: 12px;
  background: var(--c-surface);
}

.tip__icon {
  flex-shrink: 0;
  display: flex;
  color: var(--c-blue);
}

.tip__icon svg {
  width: 20px;
  height: 20px;
}

.tip__text {
  font-size: 14px;
  line-height: 1.5;
  color: var(--c-blue);
}

.section-title {
  margin: 24px 0 12px;
  font-size: 17px;
  font-weight: 700;
}

/* ── 카드별 현황 ─────────────────────────────────────────────────── */

.cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
  list-style: none;
}

.card {
  padding: 16px;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: 14px;
}

.card__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.card__name {
  font-size: 15px;
  font-weight: 600;
}

.card__rate {
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 700;
  color: #15936f;
}

.card__rate.is-warning {
  color: #d64545;
}

.card__bar {
  height: 8px;
  margin: 12px 0 10px;
  border-radius: 9999px;
  background: var(--c-surface);
  overflow: hidden;
}

.card__fill {
  display: block;
  height: 100%;
  border-radius: 9999px;
  background: #27b87e;
}

.card__fill.is-warning {
  background: #d64545;
}

.card__foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.card__usage {
  color: var(--c-text-2);
}

.card__usage strong {
  font-weight: 700;
  color: var(--c-text);
}

.card__limit {
  color: var(--c-text-3);
}

/* ── 전체 ────────────────────────────────────────────────────────── */

.total {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
  padding: 18px 20px;
  border-radius: 14px;
  background: var(--c-surface);
}

.total__icon {
  display: flex;
  color: var(--c-text-2);
}

.total__icon svg {
  width: 20px;
  height: 20px;
}

.total__label {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  font-weight: 500;
  color: var(--c-text-2);
}

.total__value {
  font-family: var(--font-num);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.3px;
}

.total__value.is-warning {
  color: #d64545;
}
</style>

<script setup lang="ts">
/**
 * 신용 관리 홈.
 *
 * 필요한 값이 전부 `/api/credit/scores/recent` 하나에서 온다 — 게이지(최신 점수),
 * 상위 %, 직전 대비 증감, 최근 추이 표(최대 5건)까지.
 *
 * 상단바·하단 탭은 공용 크롬을 쓴다(`meta.hideChrome`를 켜지 않는다).
 */
import { computed, onMounted, ref } from 'vue'
import { creditApi, type CreditScoreHistory } from '@/api/credit'
import { ApiError } from '@/api/types'
import CreditScoreGauge from '@/components/credit/CreditScoreGauge.vue'

const history = ref<CreditScoreHistory | null>(null)
const errorMessage = ref('')

/** 기록이 없는 회원도 200 + 빈 응답을 받는다. 에러가 아니라 빈 상태로 그린다. */
const hasScore = computed(() => history.value?.latestScore != null)

onMounted(async () => {
  try {
    history.value = await creditApi.fetchScoreHistory()
  } catch (e) {
    errorMessage.value =
      e instanceof ApiError ? e.message : '신용점수를 불러오지 못했습니다.'
  }
})

/**
 * 상위 %는 서버가 소수 한 자리로 준다. 디자인은 정수라 뒤의 `.0`만 뗀다 —
 * 반올림해서 값을 바꾸지는 않는다 (4.5는 그대로 "4.5%").
 */
function formatPercentile(percentile: number): string {
  return String(percentile).replace(/\.0$/, '')
}

/** "2026-09-01T10:00:00" → "2026년 9월" */
function formatMonth(updatedAt: string): string {
  // 인덱스 접근 대신 잘라 쓴다 — noUncheckedIndexedAccess에서 split 결과는 undefined일 수 있다.
  const [year, month] = updatedAt.slice(0, 10).split('-')
  return `${year}년 ${Number(month)}월`
}

/**
 * 게이지 아래 증감 문구. 0과 null은 다르다 — null은 비교할 직전 기록이 아예 없다는
 * 뜻이라 문구를 숨기고, 0은 "변동 없어요"로 채운다 (빈 자리가 생기지 않게).
 */
const diffText = computed(() => {
  const diff = history.value?.diffFromPrevious
  if (diff == null) return ''
  if (diff > 0) return `↑ ${diff}점 올랐어요`
  if (diff < 0) return `↓ ${Math.abs(diff)}점 내렸어요`
  return '지난달과 같아요'
})

/** 표의 변동 열. */
function formatDiff(diff: number): string {
  if (diff > 0) return `+${diff}`
  if (diff < 0) return String(diff)
  return '0'
}
</script>

<template>
  <div class="page">
    <p v-if="errorMessage" class="page__error">{{ errorMessage }}</p>

    <template v-else-if="history">
      <!-- 점수 게이지 -->
      <section class="score">
        <template v-if="hasScore">
          <CreditScoreGauge :score="history.latestScore!">
            <span class="score__label">나의 신용점수</span>
            <p class="score__value">{{ history.latestScore }}점</p>
            <p v-if="diffText" class="score__diff">{{ diffText }}</p>
          </CreditScoreGauge>

          <p v-if="history.latestPercentile != null" class="score__pill">
            상위 {{ formatPercentile(history.latestPercentile) }}%
          </p>
        </template>

        <p v-else class="score__empty">
          아직 신용점수 기록이 없어요.<br />
          점수가 들어오면 여기에서 변화를 보여드릴게요.
        </p>
      </section>

      <!-- 최근 추이 -->
      <template v-if="history.items.length">
        <h2 class="section-title">최근 추이</h2>

        <div class="trend">
          <table class="trend__table">
            <thead>
              <tr>
                <th scope="col">조회 날짜</th>
                <th scope="col">신용 점수</th>
                <th scope="col">변동</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in history.items" :key="item.creditScoreId">
                <td>{{ formatMonth(item.updatedAt) }}</td>
                <td class="trend__score">{{ item.score }}</td>
                <!-- 비교할 직전 기록이 없는 행은 0이 아니라 '-'다. -->
                <td
                  v-if="item.diff != null"
                  class="trend__diff"
                  :class="item.diff >= 0 ? 'is-up' : 'is-down'"
                >
                  {{ formatDiff(item.diff) }}
                </td>
                <td v-else class="trend__diff trend__diff--none">-</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>

      <RouterLink to="/credit-manage/rates" class="cta">
        예상 금리 보러 가기 <span aria-hidden="true">→</span>
      </RouterLink>
    </template>
  </div>
</template>

<style scoped>
.page {
  padding: 8px 20px 32px;
}

.page__error {
  padding: 48px 0;
  text-align: center;
  color: var(--c-text-3);
  font-size: 14px;
}

/* ── 점수 게이지 카드 ─────────────────────────────────────────────── */

.score {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 28px 20px 20px;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: 20px;
}

.score__label {
  font-size: 13px;
  color: var(--c-text-3);
}

.score__value {
  font-family: var(--font-num);
  font-size: 40px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.5px;
}

.score__diff {
  font-size: 13px;
  color: var(--c-text-2);
}

.score__pill {
  padding: 6px 14px;
  border-radius: 9999px;
  background: var(--c-surface);
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text-2);
}

.score__empty {
  padding: 40px 0;
  text-align: center;
  font-size: 14px;
  line-height: 1.6;
  color: var(--c-text-3);
}

/* ── 최근 추이 ───────────────────────────────────────────────────── */

.section-title {
  margin: 28px 0 12px;
  font-size: 17px;
  font-weight: 600;
}

.trend {
  overflow: hidden;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: 12px;
}

.trend__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.trend__table th,
.trend__table td {
  padding: 14px 16px;
  text-align: left;
}

.trend__table th {
  background: var(--c-surface);
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text-2);
}

.trend__table tbody tr {
  border-top: 1px solid var(--c-border);
}

.trend__score {
  font-weight: 600;
}

/* 증감 색은 포켓 화면에서 쓰는 값과 같다. */
.trend__diff.is-up {
  color: #15936f;
}
.trend__diff.is-down {
  color: #d64545;
}
.trend__diff--none {
  color: var(--c-text-3);
}

/* ── CTA ─────────────────────────────────────────────────────────── */

.cta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  margin-top: 28px;
  padding: 18px;
  border-radius: 12px;
  background: #171717;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}
</style>

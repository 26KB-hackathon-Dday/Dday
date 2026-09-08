<script setup lang="ts">
/**
 * 예상 금리.
 *
 * 최신 신용점수로 업권별(은행·캐피탈·카드사) 예상 금리를 보여주고, 점수를 더 올렸을 때의
 * 금리와 1,000만원 기준 연 절약액을 비교한다. 값은 `/api/credit/rates/expected`가 다 준다 —
 * 이름만 `/api/users/me`에서 온다.
 *
 * 업권 이름은 서버가 주는 `label`을 쓴다. `BANK → 은행` 매핑을 화면에서 다시 만들면
 * 양쪽에서 관리하게 된다 (AGENTS.md).
 */
import { computed, onMounted, ref } from 'vue'
import { creditApi, type ExpectedRates, type LenderType } from '@/api/credit'
import { userApi } from '@/api/user'
import { ApiError } from '@/api/types'

const rates = ref<ExpectedRates | null>(null)
const name = ref('')
const errorMessage = ref('')

const hasScore = computed(() => rates.value?.score != null)
/** 만점이면 목표가 없다. 올릴 여지가 없는데 "0원 절약"을 보여줄 이유가 없어 두 카드를 감춘다. */
const hasTarget = computed(() => rates.value?.targetScore != null)
const lenders = computed(() => rates.value?.lenders ?? [])

onMounted(async () => {
  try {
    const [expected, me] = await Promise.all([
      creditApi.fetchExpectedRates(),
      userApi.fetchMe(),
    ])
    rates.value = expected
    name.value = me.name
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '예상 금리를 불러오지 못했습니다.'
  }
})

/**
 * 원 → 만원. 소수 첫째 자리까지만 쓰고 `.0`이면 정수로 둔다.
 * 537,000 → "53.7", 1,240,000 → "124", 10,000,000 → "1,000". 디자인 표기 규칙이다.
 */
function toManwon(won: number): string {
  const manwon = Math.round((won / 10_000) * 10) / 10
  return manwon.toLocaleString('ko-KR', { maximumFractionDigits: 1 })
}

/** 금액 칸. 공시가 없어 `null`인 값은 0이 아니므로 '-'로 둔다. */
function amountText(won: number | null): string {
  return won == null ? '-' : `${toManwon(won)}만원`
}

/**
 * 금리 칸. 소수 두 자리로 고정한다 — 백엔드는 `6.20`을 주지만 JSON 숫자로 오면서
 * 끝자리 0이 떨어져 `6.2`가 된다. 그대로 찍으면 한 열에 "6.42%"와 "6.2%"가 섞인다.
 */
function rateText(rate: number | null): string {
  return rate == null ? '-' : `${rate.toFixed(2)}%`
}

/** 업권별 강조색. 세 값을 한눈에 구분하려는 것이고, 전부 코드베이스에 이미 있는 색이다. */
const LENDER_COLORS: Record<LenderType, string> = {
  BANK: 'var(--c-blue)',
  CAPITAL: '#ef8e3d',
  CARD: '#bd6bea',
}
</script>

<template>
  <div class="page">
    <p v-if="errorMessage" class="page__error">{{ errorMessage }}</p>

    <template v-else-if="rates">
      <p v-if="!hasScore" class="page__empty">
        아직 신용점수 기록이 없어요.<br />
        점수가 들어오면 예상 금리를 알려드릴게요.
      </p>

      <template v-else>
        <!-- 현재 예상 금리 -->
        <section class="card">
          <div class="card__head">
            <div>
              <h2 class="card__title">{{ name }}님의 예상 금리</h2>
              <p class="card__sub">신용점수 {{ rates.score }}점으로 분석했어요.</p>
            </div>
            <span class="card__badge" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.8"
                   stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="6" width="18" height="13" rx="3" />
                <path d="M3 10h18" />
                <path d="M16.5 14.5h1.5" />
              </svg>
            </span>
          </div>

          <ul class="rates">
            <li v-for="lender in lenders" :key="lender.lenderType" class="rates__item">
              <span class="rates__label">{{ lender.label }} 예상 금리</span>
              <strong class="rates__value" :style="{ color: LENDER_COLORS[lender.lenderType] }">
                {{ rateText(lender.currentRate) }}
              </strong>
            </li>
          </ul>
        </section>

        <!-- 점수를 올렸을 때 -->
        <section v-if="hasTarget" class="card">
          <h2 class="card__title">신용점수 {{ rates.scoreGap }}점 더 올리면?</h2>

          <ul class="rates">
            <li v-for="lender in lenders" :key="lender.lenderType" class="rates__item">
              <span class="rates__label">{{ lender.label }} 예상 금리</span>
              <strong class="rates__value" :style="{ color: LENDER_COLORS[lender.lenderType] }">
                {{ rateText(lender.targetRate) }}
              </strong>
            </li>
          </ul>
        </section>

        <!-- 절약액 -->
        <section v-if="hasTarget" class="card">
          <h2 class="card__title card__title--multiline">
            {{ toManwon(rates.principal ?? 0) }}만원 기준,<br />
            점수 {{ rates.scoreGap }}점 더 올리면 1년에 이 만큼 절약돼요
          </h2>

          <div class="saving">
            <table class="saving__table">
              <thead>
                <tr>
                  <th scope="col"><span class="sr-only">구분</span></th>
                  <th v-for="lender in lenders" :key="lender.lenderType" scope="col">
                    {{ lender.label }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <th scope="row">Before</th>
                  <td v-for="lender in lenders" :key="lender.lenderType">
                    {{ amountText(lender.currentAnnualInterest) }}
                  </td>
                </tr>
                <tr>
                  <th scope="row">After</th>
                  <td v-for="lender in lenders" :key="lender.lenderType">
                    {{ amountText(lender.targetAnnualInterest) }}
                  </td>
                </tr>
                <tr>
                  <th scope="row" class="saving__rowhead--accent">연 절약</th>
                  <td v-for="lender in lenders" :key="lender.lenderType">
                    <span v-if="lender.annualSaving != null" class="saving__pill">
                      {{ toManwon(lender.annualSaving) }}만원
                    </span>
                    <span v-else>-</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!--
          "신용 점수 올리는 방법" 화면은 아직 없다. 죽은 링크를 만들지 않으려고 비활성으로 둔다.
        -->
        <button type="button" class="cta" disabled>
          신용 점수 올리는 방법 <span aria-hidden="true">→</span>
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

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

/* ── 카드 공통 ───────────────────────────────────────────────────── */

.card {
  margin-bottom: 16px;
  padding: 20px;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: 20px;
}

.card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--c-border);
}

.card__title {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.2px;
}

.card__title--multiline {
  line-height: 1.5;
}

.card__sub {
  margin-top: 4px;
  font-size: 13px;
  color: var(--c-text-3);
}

.card__badge {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 9999px;
  background: #171717;
}

.card__badge svg {
  width: 22px;
  height: 22px;
}

/* ── 금리 3열 ────────────────────────────────────────────────────── */

.rates {
  display: flex;
  margin-top: 16px;
  padding: 16px 8px;
  border-radius: 12px;
  background: var(--c-surface);
  list-style: none;
}

.rates__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  text-align: center;
}

.rates__item + .rates__item {
  border-left: 1px solid var(--c-border);
}

.rates__label {
  font-size: 11px;
  color: var(--c-text-3);
}

.rates__value {
  font-family: var(--font-num);
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.3px;
}

/* ── 절약 표 ─────────────────────────────────────────────────────── */

.saving {
  margin-top: 16px;
  overflow: hidden;
  border: 1px solid var(--c-border);
  border-radius: 12px;
}

.saving__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  text-align: center;
}

.saving__table th,
.saving__table td {
  padding: 12px 6px;
}

.saving__table thead th {
  font-weight: 600;
}

.saving__table tbody tr {
  border-top: 1px solid var(--c-border);
}

.saving__table tbody th {
  font-weight: 500;
  color: var(--c-text-2);
  text-align: left;
  padding-left: 14px;
}

.saving__rowhead--accent {
  color: #15936f !important;
  font-weight: 600 !important;
}

/* 절약액은 알약으로 강조한다. 이 화면에서 사용자가 실제로 얻는 값이다. */
.saving__pill {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 9999px;
  background: #eaf7f1;
  color: #15936f;
  font-weight: 600;
}

/* ── CTA ─────────────────────────────────────────────────────────── */

.cta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  margin-top: 12px;
  padding: 18px;
  border-radius: 12px;
  background: #171717;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}

.cta:disabled {
  cursor: default;
}
</style>

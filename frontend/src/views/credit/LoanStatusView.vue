<script setup lang="ts">
/**
 * 대출 현황 — 몇 건을 어디서 빌렸는지.
 *
 * 금융권 문구는 서버가 주는 `label`·`sectorLabel`을 쓴다. 시안은 "은행 (1금융권)",
 * "캐피탈 (2금융권)"이지만 제2금융권에는 저축은행·카드사도 들어가므로 "캐피탈"로 못 박으면
 * 틀린 경우가 생긴다.
 *
 * 비율은 서버 값(잔액)으로 화면에서 계산한다. 라벨과 달리 단순 산술이라 정본을 나눌 일이 없다.
 */
import { computed, onMounted, ref } from 'vue'
import { creditApi, type FinancialSector, type LoanComposition } from '@/api/credit'
import { ApiError } from '@/api/types'

const composition = ref<LoanComposition | null>(null)
const errorMessage = ref('')

onMounted(async () => {
  try {
    composition.value = await creditApi.fetchLoans()
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '대출 현황을 불러오지 못했습니다.'
  }
})

const loans = computed(() => composition.value?.loans ?? [])
const sectors = computed(() => composition.value?.sectors ?? [])

/** 제1금융권이 아닌 대출 건수. 하단 안내를 띄울지, 몇 건이라 쓸지를 정한다. */
const nonBankCount = computed(() =>
  sectors.value
    .filter((sector) => sector.sector !== 'BANK')
    .reduce((sum, sector) => sum + sector.loanCount, 0),
)

/** 원 → 만원. 소수 첫째 자리까지 쓰고 `.0`이면 정수로 둔다. */
function toManwon(won: number): string {
  const manwon = Math.round((won / 10_000) * 10) / 10
  return manwon.toLocaleString('ko-KR', { maximumFractionDigits: 1 })
}

/** 전체 잔액 대비 비중(%). 잔액이 0이면 나눌 수 없다. */
function shareOf(balance: number): number {
  const total = composition.value?.totalBalance ?? 0
  return total > 0 ? Math.round((balance / total) * 100) : 0
}

/** 제1금융권만 안전색이다. 대부업까지 한 색으로 묶지 않도록 권역별로 나눠 둔다. */
const SECTOR_COLORS: Record<FinancialSector, string> = {
  BANK: '#27b87e',
  NON_BANK: '#d64545',
  LOAN_COMPANY: '#a52a2a',
}

function colorOf(sector: FinancialSector | null): string {
  return sector ? SECTOR_COLORS[sector] : 'var(--c-text-3)'
}

/** 점수에 불리한 대출인지. 권역을 모르면 함부로 위험하다고 하지 않는다. */
function isRisky(sector: FinancialSector | null): boolean {
  return sector != null && sector !== 'BANK'
}
</script>

<template>
  <div class="page">
    <p v-if="errorMessage" class="page__error">{{ errorMessage }}</p>

    <template v-else-if="composition">
      <p v-if="!loans.length" class="page__empty">
        연동된 대출이 없어요.<br />
        대출이 없는 것도 신용점수에는 좋은 신호예요.
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
            같은 금액이라도 제2금융권 대출이 있으면 신용점수가 더 깎여요.
          </p>
        </aside>

        <!-- 금융권별 비율 -->
        <section class="ratio">
          <h2 class="ratio__title">금융권별 대출 비율</h2>
          <p class="ratio__total">
            총 대출 잔액 <strong>{{ toManwon(composition.totalBalance) }}만원</strong>
          </p>

          <div class="ratio__bar" role="img" aria-label="금융권별 대출 잔액 비율">
            <span
              v-for="sector in sectors"
              :key="sector.sector"
              class="ratio__segment"
              :style="{
                width: `${shareOf(sector.totalBalance)}%`,
                background: colorOf(sector.sector),
              }"
            />
          </div>

          <ul class="ratio__legend">
            <li v-for="sector in sectors" :key="sector.sector" class="legend">
              <span class="legend__dot" :style="{ background: colorOf(sector.sector) }" />
              <span class="legend__label">{{ sector.label }}</span>
              <span class="legend__value">
                {{ toManwon(sector.totalBalance) }}만원 ({{ shareOf(sector.totalBalance) }}%)
              </span>
            </li>
          </ul>
        </section>

        <!-- 대출 목록 -->
        <h2 class="section-title">나의 대출 목록</h2>

        <ul class="loans">
          <li v-for="loan in loans" :key="loan.accountId" class="loan">
            <div class="loan__head">
              <span
                class="loan__badge"
                :class="{ 'is-risky': isRisky(loan.sector) }"
              >
                {{ loan.sectorLabel ?? '기관 미확인' }}
              </span>

              <span v-if="isRisky(loan.sector)" class="loan__warning">
                <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                  <path d="M12 3.5 1.5 21h21L12 3.5zm0 5.2 6.8 11.3H5.2L12 8.7zm-.9 3.3v4h1.8v-4h-1.8zm0 5.2v1.8h1.8v-1.8h-1.8z" />
                </svg>
                점수 하락 위험
              </span>
            </div>

            <p class="loan__name">{{ loan.productName }}</p>
            <p class="loan__meta">
              잔액 <strong>{{ toManwon(loan.balance) }}만원</strong>
              <template v-if="loan.interestRate != null">
                <span class="loan__dot" aria-hidden="true">·</span>
                금리
                <strong :class="{ 'is-risky': isRisky(loan.sector) }">{{ loan.interestRate }}%</strong>
              </template>
            </p>
          </li>
        </ul>

        <aside v-if="nonBankCount" class="advice">
          <span class="advice__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm-1.2 14.3-4-4 1.4-1.4 2.6 2.6 5.6-5.6 1.4 1.4-7 7z" />
            </svg>
          </span>
          <p class="advice__text">
            제2금융권 {{ nonBankCount }}건을 은행 대출로 옮기면 점수 관리에 큰 도움이 돼요!
          </p>
        </aside>

        <!-- 소비를 줄여야 대출을 갚는다. 포켓이 그 자리다. -->
        <RouterLink to="/pockets" class="cta">
          내 소비 관리하러 가기 <span aria-hidden="true">→</span>
        </RouterLink>
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
  border: 1px solid var(--c-border);
  border-radius: 12px;
  background: var(--c-bg);
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

/* ── 금융권별 비율 ───────────────────────────────────────────────── */

.ratio {
  margin-top: 16px;
  padding: 20px;
  border: 1px solid var(--c-border);
  border-radius: 16px;
  background: var(--c-bg);
}

.ratio__title {
  font-size: 16px;
  font-weight: 700;
}

.ratio__total {
  margin-top: 6px;
  font-size: 13px;
  color: var(--c-text-3);
}

.ratio__total strong {
  margin-left: 4px;
  font-family: var(--font-num);
  font-size: 22px;
  font-weight: 700;
  color: var(--c-text);
}

/* 세그먼트를 이어 붙인 하나의 막대. 사이 간격을 두지 않아야 비율이 눈에 그대로 읽힌다. */
.ratio__bar {
  display: flex;
  height: 12px;
  margin: 14px 0 16px;
  border-radius: 9999px;
  background: var(--c-surface);
  overflow: hidden;
}

.ratio__segment {
  display: block;
  height: 100%;
}

.ratio__legend {
  display: flex;
  flex-direction: column;
  gap: 10px;
  list-style: none;
}

.legend {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.legend__dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 9999px;
}

.legend__label {
  flex: 1;
  min-width: 0;
  color: var(--c-text-2);
}

.legend__value {
  font-weight: 700;
}

/* ── 대출 목록 ───────────────────────────────────────────────────── */

.section-title {
  margin: 24px 0 12px;
  font-size: 17px;
  font-weight: 700;
}

.loans {
  display: flex;
  flex-direction: column;
  gap: 12px;
  list-style: none;
}

.loan {
  padding: 16px;
  border: 1px solid var(--c-border);
  border-radius: 14px;
  background: var(--c-bg);
}

.loan__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.loan__badge {
  padding: 4px 10px;
  border-radius: 8px;
  background: #eaf7f1;
  color: #15936f;
  font-size: 12px;
  font-weight: 600;
}

.loan__badge.is-risky {
  background: #fdecec;
  color: #d64545;
}

.loan__warning {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #d64545;
  font-size: 12px;
  font-weight: 600;
}

.loan__warning svg {
  width: 14px;
  height: 14px;
}

.loan__name {
  margin-top: 12px;
  font-size: 16px;
  font-weight: 700;
}

.loan__meta {
  margin-top: 4px;
  font-size: 13px;
  color: var(--c-text-3);
}

.loan__meta strong {
  font-weight: 700;
  color: var(--c-text);
}

.loan__meta strong.is-risky {
  color: #d64545;
}

.loan__dot {
  margin: 0 6px;
}

/* ── 권유 ────────────────────────────────────────────────────────── */

.advice {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 16px;
  padding: 16px;
  border-radius: 12px;
  background: #eaf7f1;
}

.advice__icon {
  flex-shrink: 0;
  display: flex;
  color: #15936f;
}

.advice__icon svg {
  width: 20px;
  height: 20px;
}

.advice__text {
  font-size: 14px;
  line-height: 1.5;
  font-weight: 500;
  color: #15936f;
}

/* ── CTA ─────────────────────────────────────────────────────────── */

.cta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  margin-top: 20px;
  padding: 18px;
  border-radius: 12px;
  background: #171717;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}
</style>

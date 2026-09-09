<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import type { AssetForecastResponse } from '@/api/assetForecast'
import type { UserAccount } from '@/api/mydata'
import type { PocketMonthlySummary } from '@/api/pocket'
import { formatWon } from '@/utils/format'

const props = defineProps<{
  summary: PocketMonthlySummary
  forecast: AssetForecastResponse | null
  accounts: UserAccount[]
}>()

const futureAccounts = computed(() =>
  props.accounts.filter(
    (account) =>
      account.active && (account.accountType === 'SAVINGS' || account.accountType === 'INVESTMENT'),
  ),
)
const accountLabel = (account: UserAccount) =>
  account.accountType === 'INVESTMENT' ? '투자' : '적금'
const accountIcon = (account: UserAccount) =>
  account.accountType === 'INVESTMENT' ? 'account-investment' : 'account-savings'
const currentAsset = computed(
  () =>
    props.forecast?.currentAsset ??
    futureAccounts.value.reduce((total, account) => total + account.balance, 0),
)
const achievementRate = computed(() => props.forecast?.achievementRate ?? 0)
const progressWidth = computed(() => `${Math.min(Math.max(achievementRate.value, 0), 100)}%`)
</script>

<template>
  <section class="future-summary" aria-labelledby="current-future-asset-title">
    <p id="current-future-asset-title">현재 미래자산</p>
    <strong>{{ formatWon(currentAsset) }}</strong>
  </section>

  <section class="future-section">
    <header class="future-section__header">
      <h2>이번 달 미래 준비</h2>
      <span
        >계획 달성률 <strong>{{ achievementRate.toFixed(1) }}%</strong></span
      >
    </header>
    <div class="plan-card">
      <div class="plan-card__amount">
        <span>이번달 미래자산 계획</span>
        <strong>{{ formatWon(summary.targetAmount) }}</strong>
      </div>
      <div
        class="plan-card__progress"
        role="progressbar"
        aria-label="미래자산 계획 달성률"
        :aria-valuenow="achievementRate"
        aria-valuemin="0"
        aria-valuemax="100"
      >
        <span :style="{ width: progressWidth }" />
      </div>
      <dl>
        <div>
          <dt>실제로 모은 돈</dt>
          <dd>{{ formatWon(forecast?.achievedAmount ?? 0) }}</dd>
        </div>
        <div>
          <dt>남은 금액</dt>
          <dd>{{ formatWon(forecast?.remainingAmount ?? summary.targetAmount) }}</dd>
        </div>
      </dl>
    </div>
  </section>

  <section class="future-section">
    <header class="future-section__header">
      <h2>내 미래자산</h2>
      <slot name="assets-action" />
    </header>
    <ul v-if="futureAccounts.length" class="asset-list">
      <li
        v-for="account in futureAccounts"
        :key="account.accountId"
        class="asset-card"
        :class="`asset-card--${account.accountType.toLowerCase()}`"
      >
        <span class="asset-card__icon" aria-hidden="true">
          <AppIcon :name="accountIcon(account)" :size="20" />
        </span>
        <div class="asset-card__copy">
          <span>{{ accountLabel(account) }}</span>
          <strong>{{ account.productName || account.accountName || accountLabel(account) }}</strong>
          <small>이번 달 납입 +{{ formatWon(account.monthlyContribution) }}</small>
        </div>
        <strong class="asset-card__amount">{{ formatWon(account.balance) }}</strong>
      </li>
    </ul>
    <p v-else class="future-empty">등록된 미래자산이 없습니다.</p>
  </section>
</template>

<style scoped>
.future-summary {
  padding: 18px 0 28px;
  text-align: center;
}
.future-summary p {
  display: inline-flex;
  padding: 5px 12px;
  border-radius: 999px;
  background: #f2ddff;
  color: #a66ee5;
  font-size: 12px;
  font-weight: 600;
}
.future-summary > strong {
  display: block;
  margin-top: 14px;
  font-family: var(--font-num);
  font-size: 30px;
  line-height: 1.2;
}
.future-section {
  margin-top: 28px;
}
.future-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.future-section__header h2 {
  font-size: 17px;
  font-weight: 700;
}
.future-section__header span {
  color: var(--c-text-3);
  font-size: 10px;
}
.future-section__header span strong {
  color: #a66ee5;
}
.plan-card {
  padding: 18px;
  border: 1px solid #eadcff;
  border-radius: 14px;
  background: #fbf8ff;
}
.plan-card__amount,
.plan-card dl,
.plan-card dl div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.plan-card__amount span,
.plan-card dt,
.plan-card dd {
  color: var(--c-text-2);
  font-size: 12px;
}
.plan-card__amount strong,
.plan-card dd {
  flex: none;
  font-family: var(--font-num);
}
.plan-card__progress {
  height: 9px;
  margin: 14px 0;
  overflow: hidden;
  border-radius: 999px;
  background: #e8e1f0;
}
.plan-card__progress span {
  display: block;
  width: 0;
  height: 100%;
  background: #a66ee5;
}
.plan-card dl {
  align-items: flex-start;
}
.plan-card dl div:last-child {
  text-align: right;
}
.asset-list {
  display: grid;
  gap: 10px;
  list-style: none;
}
.asset-card {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--c-border);
  border-radius: 14px;
  background: var(--c-surface);
}
.asset-card--savings {
  border-color: #eadcff;
  background: #f8f2ff;
}
.asset-card--investment {
  background: #f5f6f7;
}
.asset-card__icon {
  display: grid;
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 50%;
  background: var(--c-bg);
  color: #a66ee5;
}
.asset-card__copy {
  display: grid;
  flex: 1;
  min-width: 0;
}
.asset-card__copy span,
.asset-card__copy small {
  color: var(--c-text-3);
  font-size: 10px;
}
.asset-card__copy strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.asset-card__amount {
  flex: none;
  font-family: var(--font-num);
  font-size: 14px;
  white-space: nowrap;
}
.future-empty {
  padding: 34px 16px;
  border-radius: 14px;
  background: var(--c-surface);
  color: var(--c-text-3);
  text-align: center;
  font-size: 12px;
}
@media (max-width: 340px) {
  .asset-card {
    padding-inline: 12px;
  }
  .asset-card__amount {
    font-size: 12px;
  }
}
</style>

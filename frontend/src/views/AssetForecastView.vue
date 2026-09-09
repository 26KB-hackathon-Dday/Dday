<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { assetForecastApi, type AssetForecastResponse } from '@/api/assetForecast'
import { formatPercent, formatWon } from '@/utils/format'

const forecast = ref<AssetForecastResponse | null>(null)
const loading = ref(true)
const errorMessage = ref('')

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    forecast.value = await assetForecastApi.find()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '예상 자산을 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

const assetChangeLabel = computed(() => {
  const value = forecast.value?.totalAssetChange ?? 0
  return value > 0 ? '총자산 순증가' : value < 0 ? '총자산 감소' : '총자산 변화 없음'
})

const signedWon = (value: number) => `${value > 0 ? '+' : ''}${formatWon(value)}`

const formatAssetStatus = (amount: number, depletionMonth: number | null) => {
  if (amount > 0) return formatWon(amount)
  return depletionMonth == null ? '소진됨' : `소진까지 ${depletionMonth}개월`
}

const showDepletionTimeline = computed(() => {
  const value = forecast.value
  return value != null && value.forecastTotalAsset === 0 && value.forecastShortageAmount > 0
})

const timelineWidth = (from: number, to: number | null) => {
  const total = Math.max(forecast.value?.remainingMonths ?? 0, 1)
  const end = Math.min(Math.max(to ?? from, from), total)
  return `${((end - from) / total) * 100}%`
}

const monthsWithoutAssets = computed(() => {
  const value = forecast.value
  if (!value?.housingDepositDepletionMonth) return 0
  return Math.max(value.remainingMonths - value.housingDepositDepletionMonth, 0)
})

onMounted(load)
</script>

<template>
  <main class="forecast-page">
    <div v-if="loading" class="state" role="status">예상 자산을 계산하고 있어요.</div>
    <div v-else-if="errorMessage" class="state state--error">
      <p>{{ errorMessage }}</p>
      <button type="button" @click="load">다시 시도</button>
    </div>

    <template v-else-if="forecast?.dataSufficient">
      <section class="hero">
        <div class="hero__meta">
          <span>지원 종료 시 예상 자산</span>
          <span class="chip">지원 종료까지 {{ forecast.remainingMonths }}개월</span>
        </div>
        <strong :class="{ negative: forecast.forecastNetAsset < 0, zero: forecast.forecastNetAsset === 0 }">{{ formatWon(forecast.forecastNetAsset) }}</strong>
        <p>현재 지원과 자산 흐름을 유지했을 때의 예상 금액이에요.</p>
      </section>

      <section>
        <h2>예상 자산 구성</h2>
        <div v-if="showDepletionTimeline" class="depletion-timeline">
          <div class="composition-bar" aria-label="자산 소진 예상 시점">
            <span class="cash" :style="{ width: timelineWidth(0, forecast.cashDepletionMonth) }" title="현금성 자산 사용 기간" />
            <span class="saving" :style="{ width: timelineWidth(forecast.cashDepletionMonth ?? 0, forecast.liquidAssetDepletionMonth) }" title="적금·투자 사용 기간" />
            <span class="housing" :style="{ width: timelineWidth(forecast.liquidAssetDepletionMonth ?? 0, forecast.housingDepositDepletionMonth) }" title="주거 보증금 사용 기간" />
            <span class="deficit" :style="{ width: timelineWidth(forecast.housingDepositDepletionMonth ?? 0, forecast.remainingMonths) }" title="모든 자산 소진 이후" />
          </div>
          <div class="timeline-key" aria-hidden="true">
            <span>현재</span>
            <span>지원 종료</span>
          </div>
          <p>지원 종료 {{ monthsWithoutAssets }}개월 전부터 모든 자산이 소진될 것으로 예상돼요.</p>
        </div>
        <div v-else class="composition-bar" aria-label="예상 자산 구성 비율">
          <span class="cash" :style="{ width: `${forecast.cashRatio}%` }" />
          <span class="saving" :style="{ width: `${forecast.savingInvestmentRatio}%` }" />
          <span class="housing" :style="{ width: `${forecast.housingDepositRatio}%` }" />
        </div>
        <div class="card legend">
          <div><i class="cash" :class="{ 'depleted-tone': forecast.forecastCashAsset === 0 }" /><span>현금성 자산 <small>{{ formatPercent(forecast.cashRatio) }}%</small></span><strong :class="{ depleted: forecast.forecastCashAsset === 0 }">{{ formatAssetStatus(forecast.forecastCashAsset, forecast.cashDepletionMonth) }}</strong></div>
          <div><i class="saving" :class="{ 'depleted-tone': forecast.forecastSavingInvestmentAsset === 0 }" /><span>적금·투자 <small>{{ formatPercent(forecast.savingInvestmentRatio) }}%</small></span><strong :class="{ depleted: forecast.forecastSavingInvestmentAsset === 0 }">{{ formatAssetStatus(forecast.forecastSavingInvestmentAsset, forecast.liquidAssetDepletionMonth) }}</strong></div>
          <div><i class="housing" :class="{ 'depleted-tone': forecast.forecastHousingDeposit === 0 }" /><span>주거 보증금 <small>{{ formatPercent(forecast.housingDepositRatio) }}%</small></span><strong :class="{ depleted: forecast.forecastHousingDeposit === 0 }">{{ formatAssetStatus(forecast.forecastHousingDeposit, forecast.housingDepositDepletionMonth) }}</strong></div>
        </div>
      </section>

      <section>
        <h2>현재와 비교하면</h2>
        <div class="card compare">
          <div><small>현재 총자산</small><strong :class="{ zero: forecast.currentTotalAsset === 0 }">{{ formatWon(forecast.currentTotalAsset) }}</strong></div>
          <span aria-hidden="true">→</span>
          <div class="compare__end"><small>종료 시 예상</small><strong :class="{ negative: forecast.forecastNetAsset < 0, zero: forecast.forecastNetAsset === 0 }">{{ formatWon(forecast.forecastNetAsset) }}</strong></div>
          <footer><span>{{ assetChangeLabel }}</span><b :class="{ negative: forecast.totalAssetChange < 0, zero: forecast.totalAssetChange === 0 }">{{ signedWon(forecast.totalAssetChange) }}</b></footer>
        </div>
      </section>

      <section>
        <h2>앞으로 이렇게 달라져요</h2>
        <div class="card compare">
          <div><small>현재 적금·투자원금</small><strong :class="{ zero: forecast.currentSavingInvestmentAsset === 0 }">{{ formatWon(forecast.currentSavingInvestmentAsset) }}</strong></div>
          <span aria-hidden="true">→</span>
          <div class="compare__end"><small>종료 시 예상</small><strong :class="{ zero: forecast.forecastSavingInvestmentAsset === 0 }">{{ formatWon(forecast.forecastSavingInvestmentAsset) }}</strong></div>
          <footer><span>적금·투자원금 증감</span><b :class="{ negative: forecast.savingInvestmentChange < 0, zero: forecast.savingInvestmentChange === 0 }">{{ signedWon(forecast.savingInvestmentChange) }}</b></footer>
          <p class="explain">최근 3개월 적금·투자 평균 적립액을 기준으로 계산했어요.<br />최근 3개월 월평균 {{ formatWon(forecast.averageMonthlySavingInvestment) }}을 모으고 있어요.</p>
        </div>
      </section>

      <section>
        <h2>자산의 성격도 확인하세요</h2>
        <div class="card nature">
          <div><span><strong>바로 사용 가능한 현금성 자산</strong><small>필요할 때 바로 사용할 수 있는 자산</small></span><b :class="{ zero: forecast.forecastCashAsset === 0 }">{{ formatWon(forecast.forecastCashAsset) }}</b></div>
          <div><span><strong>현금화하기 쉬운 적금·투자 자산</strong><small>해지 또는 매도를 통해 현금화할 수 있는 자산</small></span><b :class="{ zero: forecast.forecastSavingInvestmentAsset === 0 }">{{ formatWon(forecast.forecastSavingInvestmentAsset) }}</b></div>
          <div><span><strong>현금화하기 어려운 주거 보증금</strong><small>주거 계약과 연결된 자산</small></span><b :class="{ zero: forecast.forecastHousingDeposit === 0 }">{{ formatWon(forecast.forecastHousingDeposit) }}</b></div>
        </div>
      </section>

      <section class="method">
        <h2>어떻게 계산했나요?</h2>
        <p>현재 연결된 마이데이터의 최근 3개월 수입·지출·적금·투자 흐름과 지원 종료까지 남은 기간을 기준으로 계산했어요.</p>
        <p>월 적자가 계속되면 현금성 자산, 적금·투자, 주거 보증금 순으로 차감했으며 투자 수익률은 반영하지 않았어요. 실제 자산은 달라질 수 있어요.</p>
      </section>
    </template>
    <div v-else class="state">
      <p>예상 자산을 계산하기 위한 데이터가 아직 부족해요.</p>
      <small>마이데이터 거래내역과 고정수입을 확인해 주세요.</small>
    </div>
  </main>
</template>

<style scoped>
.forecast-page{display:flex;flex-direction:column;gap:30px;width:100%;padding:24px 12px 40px;background:var(--c-bg)}
.forecast-page section{display:flex;flex-direction:column;gap:12px}.forecast-page h2{font-size:15px;font-weight:650}.hero__meta{display:flex;align-items:center;justify-content:space-between;color:var(--c-text-3);font-size:12px}.chip{padding:4px 9px;border-radius:999px;background:var(--c-surface)}.hero>strong{font:700 30px/1.2 var(--font-num)}.hero>strong.negative,.hero>strong.zero,.compare strong.negative,.compare strong.zero,.compare footer b.zero,.nature b.zero{color:var(--c-danger)}.hero>p,.method p{font-size:11px;color:var(--c-text-3)}.composition-bar{display:flex;height:24px;overflow:hidden;border-radius:999px;background:var(--c-surface)}.cash{background:#3483f5}.saving{background:#10b89f}.housing{background:#337f38}.deficit{background:var(--c-danger)}.depletion-timeline{display:grid;gap:5px}.depletion-timeline .cash,.legend i.cash.depleted-tone{background:#f2b84b}.depletion-timeline .saving,.legend i.saving.depleted-tone{background:#ea8a3a}.depletion-timeline .housing,.legend i.housing.depleted-tone{background:#df645c}.depletion-timeline .deficit{background:#a92f2f}.timeline-key{display:flex;justify-content:space-between;font-size:9px;color:var(--c-text-3)}.depletion-timeline p{font-size:10px;color:var(--c-danger)}.card{padding:16px;border-radius:15px;background:var(--c-surface)}.legend{display:grid;gap:13px}.legend>div{display:grid;grid-template-columns:8px minmax(0,1fr) auto;align-items:center;gap:8px;font-size:12px}.legend i{width:8px;height:8px;border-radius:50%}.legend small{margin-left:5px;color:var(--c-text-3)}.legend strong{color:#2674eb;white-space:nowrap}.legend strong.depleted{color:var(--c-danger)}.compare{display:grid;grid-template-columns:minmax(0,1fr) auto minmax(0,1fr);align-items:center;gap:8px}.compare>div{display:flex;flex-direction:column}.compare small{font-size:10px;color:var(--c-text-3)}.compare strong{font-size:13px;white-space:nowrap}.compare__end{text-align:right;color:#397eea}.compare footer,.compare .explain{grid-column:1/-1}.compare footer{display:flex;justify-content:space-between;margin-top:5px;padding-top:13px;border-top:1px solid var(--c-border);font-size:11px}.compare footer b{color:#21853d}.compare footer b.negative{color:var(--c-danger)}.explain{font-size:10px;line-height:1.7;color:var(--c-text-3)}.nature{display:grid;gap:15px}.nature>div{display:flex;align-items:flex-start;justify-content:space-between;gap:10px}.nature span{display:flex;min-width:0;flex-direction:column}.nature strong{font-size:11px}.nature small{font-size:9px;color:var(--c-text-3)}.nature b{font-size:11px;color:#2e8b43;white-space:nowrap}.method{padding-top:4px;border-top:1px solid var(--c-border)}.state{display:grid;min-height:55vh;place-content:center;gap:12px;text-align:center;color:var(--c-text-3)}.state button{padding:10px 18px;border-radius:10px;background:var(--c-primary);color:white}
@media(max-width:350px){.forecast-page{padding-inline:10px}.legend>div{font-size:11px}.compare strong{font-size:11px}.nature b{font-size:10px}}
</style>

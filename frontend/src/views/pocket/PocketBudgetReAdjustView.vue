<template>
  <div class="budget-page">
    <main class="budget-content">
      <section class="intro-section">
        <h2 class="intro-title">포켓 예산을 조정해 보세요</h2>

        <p class="intro-description">
          이미 사용한 금액보다 낮게는 줄일 수 없어요.<br />
          남아 있는 예산 안에서 자유롭게 조정해 보세요.
        </p>
      </section>

      <section class="usage-guide">
        <strong class="usage-guide__title"> 이미 사용한 금액은 변경할 수 없어요. </strong>

        <p class="usage-guide__description">각 포켓은 현재 사용액 이상으로만 조정할 수 있어요.</p>
      </section>

      <p v-if="loadError" class="load-error">
        {{ loadError }}
      </p>

      <template v-if="!isLoading">
        <section class="total-card">
          <div class="total-card__text">
            <span class="total-label"> 이번 달 총 예산 </span>

            <span class="total-description">
              이번 달 남은 기간에 사용할 총 예산을 수정할 수 있어요.
            </span>
          </div>

          <div class="total-input-wrap">
            <input
              v-model="totalBudgetInput"
              class="total-input"
              type="text"
              inputmode="numeric"
              @input="handleTotalInput"
              @keyup.enter="applyTotalBudget"
            />

            <span class="total-unit"> 원 </span>
          </div>

          <button class="total-register-button" type="button" @click="applyTotalBudget">
            총 예산 등록
          </button>

          <p v-if="totalBudgetError" class="total-error">
            {{ totalBudgetError }}
          </p>
        </section>

        <section class="pocket-section">
          <div class="section-heading">
            <h3 class="section-title">포켓별 예산</h3>

            <p class="section-description">전체 합계가 총 예산과 같아야 저장할 수 있어요.</p>
          </div>

          <div class="pocket-list">
            <section class="pocket-card pocket-card--essential">
              <div class="pocket-card__top">
                <div>
                  <span class="pocket-badge pocket-badge--essential"> 필수 포켓 </span>

                  <p class="pocket-min pocket-min--essential">
                    최소
                    {{ formatCurrency(usedAmounts.essential) }}
                    이상으로 조정 가능
                  </p>
                </div>

                <div class="pocket-value">
                  <strong>
                    {{ formatCurrency(budgets.essential) }}
                  </strong>

                  <span> {{ getPercentage(budgets.essential) }}% </span>
                </div>
              </div>

              <input
                class="pocket-slider"
                type="range"
                :min="usedAmounts.essential"
                :max="totalBudget"
                :step="STEP"
                :value="budgets.essential"
                :style="getSliderStyle(budgets.essential, usedAmounts.essential)"
                @input="handlePocketInput('essential', $event)"
              />

              <p class="used-amount">
                🔒 현재 사용액
                {{ formatCurrency(usedAmounts.essential) }}
              </p>
            </section>

            <section class="pocket-card pocket-card--free">
              <div class="pocket-card__top">
                <div>
                  <span class="pocket-badge pocket-badge--free"> 자유 포켓 </span>

                  <p class="pocket-min pocket-min--free">
                    최소
                    {{ formatCurrency(usedAmounts.free) }}
                    이상으로 조정 가능
                  </p>
                </div>

                <div class="pocket-value">
                  <strong>
                    {{ formatCurrency(budgets.free) }}
                  </strong>

                  <span> {{ getPercentage(budgets.free) }}% </span>
                </div>
              </div>

              <input
                class="pocket-slider"
                type="range"
                :min="usedAmounts.free"
                :max="totalBudget"
                :step="STEP"
                :value="budgets.free"
                :style="getSliderStyle(budgets.free, usedAmounts.free)"
                @input="handlePocketInput('free', $event)"
              />

              <p class="used-amount">
                🔒 현재 사용액
                {{ formatCurrency(usedAmounts.free) }}
              </p>
            </section>

            <section class="pocket-card pocket-card--future">
              <div class="pocket-card__top">
                <div>
                  <span class="pocket-badge pocket-badge--future"> 미래자산 포켓 </span>

                  <p class="pocket-min pocket-min--future">
                    최소
                    {{ formatCurrency(usedAmounts.future) }}
                    이상으로 조정 가능
                  </p>
                </div>

                <div class="pocket-value">
                  <strong>
                    {{ formatCurrency(budgets.future) }}
                  </strong>

                  <span> {{ getPercentage(budgets.future) }}% </span>
                </div>
              </div>

              <input
                class="pocket-slider"
                type="range"
                :min="usedAmounts.future"
                :max="totalBudget"
                :step="STEP"
                :value="budgets.future"
                :style="getSliderStyle(budgets.future, usedAmounts.future)"
                @input="handlePocketInput('future', $event)"
              />

              <p class="used-amount">
                🔒 현재 사용액
                {{ formatCurrency(usedAmounts.future) }}
              </p>
            </section>

            <section class="pocket-card pocket-card--emergency">
              <div class="pocket-card__top">
                <div>
                  <span class="pocket-badge pocket-badge--emergency"> 비상금 포켓 </span>

                  <p class="pocket-min pocket-min--emergency">
                    최소
                    {{ formatCurrency(usedAmounts.emergency) }}
                    이상으로 조정 가능
                  </p>
                </div>

                <div class="pocket-value">
                  <strong>
                    {{ formatCurrency(budgets.emergency) }}
                  </strong>

                  <span> {{ getPercentage(budgets.emergency) }}% </span>
                </div>
              </div>

              <input
                class="pocket-slider"
                type="range"
                :min="usedAmounts.emergency"
                :max="totalBudget"
                :step="STEP"
                :value="budgets.emergency"
                :style="getSliderStyle(budgets.emergency, usedAmounts.emergency)"
                @input="handlePocketInput('emergency', $event)"
              />

              <p class="used-amount">
                🔒 현재 사용액
                {{ formatCurrency(usedAmounts.emergency) }}
              </p>
            </section>
          </div>
        </section>

        <section class="budget-status" :class="statusClass">
          <div class="budget-status__row">
            <span>현재 배분</span>

            <strong>
              {{ formatCurrency(allocatedTotal) }}
            </strong>
          </div>

          <div class="budget-status__row">
            <span>총 예산</span>

            <strong>
              {{ formatCurrency(totalBudget) }}
            </strong>
          </div>

          <div class="budget-status__divider" />

          <div v-if="budgetGap > 0" class="status-message status-message--error">
            <strong>
              총 예산보다
              {{ formatCurrency(budgetGap) }}
              많아요.
            </strong>

            <span> 다른 포켓의 예산을 줄여주세요. </span>
          </div>

          <div v-else-if="budgetGap < 0" class="status-message">
            <strong>
              아직
              {{ formatCurrency(Math.abs(budgetGap)) }}
              남았어요.
            </strong>

            <span> 원하는 포켓에 더 배분해 주세요. </span>
          </div>

          <div v-else class="status-message status-message--success">
            <strong> 총 예산에 맞게 배분됐어요. </strong>
          </div>

          <button
            v-if="budgetGap !== 0 && lastChangedPocket"
            class="auto-button"
            type="button"
            @click="autoBalance"
          >
            자동으로 맞추기
          </button>

          <p v-if="autoBalanceError" class="auto-error">
            {{ autoBalanceError }}
          </p>
        </section>

        <BudgetForecastCard
          v-if="forecastLoaded"
          :current-asset="currentAsset"
          :expected-asset="expectedAsset"
          :future-budget="budgets.future"
          :remaining-months="remainingMonths"
        />

        <section v-else-if="forecastError" class="forecast-error">
          예상 자산 정보를 불러오지 못했어요.
        </section>

        <button
          class="save-button"
          :class="{
            'save-button--disabled': !isBalanced,
          }"
          type="button"
          :disabled="!isBalanced"
          @click="handleSave"
        >
          이 비율로 저장하기
        </button>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { useRouter } from 'vue-router'

import BudgetForecastCard from '@/components/pocket/BudgetForecastCard.vue'

import { budgetAdjustmentApi, type BudgetAdjustmentPocketType } from '@/api/budgetAdjustment'

import { assetForecastApi } from '@/api/assetForecast'

type PocketType = 'essential' | 'free' | 'future' | 'emergency'

const router = useRouter()

const STEP = 50_000

const totalBudget = ref(0)

const budgets = reactive<Record<PocketType, number>>({
  essential: 0,
  free: 0,
  future: 0,
  emergency: 0,
})

const usedAmounts = reactive<Record<PocketType, number>>({
  essential: 0,
  free: 0,
  future: 0,
  emergency: 0,
})

const totalBudgetInput = ref('')

const totalBudgetError = ref('')
const autoBalanceError = ref('')

const loadError = ref('')
const forecastError = ref('')

const isLoading = ref(true)

const lastChangedPocket = ref<PocketType | null>(null)

/*
 * 예상 자산 API 데이터
 */
const currentAsset = ref(0)

const remainingMonths = ref(0)

const forecastLoaded = ref(false)

/*
 * 변경 중인 미래자산 포켓 금액을 이용해
 * 예상 자산을 실시간 미리보기 한다.
 *
 * 실제 현재 자산과 남은 개월 수는
 * 백엔드 계산값을 사용한다.
 */
const expectedAsset = computed(() => {
  return currentAsset.value + budgets.future * remainingMonths.value
})

const totalUsedAmount = computed(() => {
  return usedAmounts.essential + usedAmounts.free + usedAmounts.future + usedAmounts.emergency
})

const allocatedTotal = computed(() => {
  return budgets.essential + budgets.free + budgets.future + budgets.emergency
})

const budgetGap = computed(() => {
  return allocatedTotal.value - totalBudget.value
})

const isBalanced = computed(() => {
  return budgetGap.value === 0 && totalBudget.value > 0 && !isLoading.value
})

const statusClass = computed(() => {
  if (budgetGap.value > 0) {
    return 'budget-status--over'
  }

  if (budgetGap.value < 0) {
    return 'budget-status--under'
  }

  return 'budget-status--balanced'
})

const toLocalPocketType = (type: BudgetAdjustmentPocketType): PocketType => {
  switch (type) {
    case 'ESSENTIAL':
      return 'essential'

    case 'FREE':
      return 'free'

    case 'EMERGENCY':
      return 'emergency'

    case 'FUTURE_ASSET':
      return 'future'
  }
}

const loadCurrentBudget = async () => {
  const response = await budgetAdjustmentApi.findCurrent()

  totalBudget.value = response.totalBudgetAmount

  totalBudgetInput.value = response.totalBudgetAmount.toLocaleString('ko-KR')

  response.pockets.forEach((pocket) => {
    const localType = toLocalPocketType(pocket.pocketType)

    budgets[localType] = Number(pocket.targetAmount ?? 0)

    usedAmounts[localType] = Number(pocket.usedAmount ?? 0)
  })
}

const loadForecast = async () => {
  forecastError.value = ''

  try {
    const response = await assetForecastApi.find()

    currentAsset.value = Number(response.currentAsset ?? 0)

    remainingMonths.value = Number(response.remainingMonths ?? 0)

    forecastLoaded.value = true
  } catch (error) {
    forecastLoaded.value = false

    forecastError.value =
      error instanceof Error ? error.message : '예상 자산 정보를 불러오지 못했어요.'
  }
}

const load = async () => {
  isLoading.value = true
  loadError.value = ''

  try {
    await Promise.all([loadCurrentBudget(), loadForecast()])
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '예산 정보를 불러오지 못했어요.'
  } finally {
    isLoading.value = false
  }
}

const handlePocketInput = (type: PocketType, event: Event) => {
  const target = event.target as HTMLInputElement

  const requestedValue = Number(target.value)

  budgets[type] = Math.max(requestedValue, usedAmounts[type])

  lastChangedPocket.value = type

  autoBalanceError.value = ''
}

const handleTotalInput = (event: Event) => {
  const target = event.target as HTMLInputElement

  const onlyNumbers = target.value.replace(/[^0-9]/g, '')

  if (!onlyNumbers) {
    totalBudgetInput.value = ''
    return
  }

  totalBudgetInput.value = Number(onlyNumbers).toLocaleString('ko-KR')
}

const applyTotalBudget = () => {
  totalBudgetError.value = ''

  const rawValue = totalBudgetInput.value.replace(/,/g, '')

  const value = Number(rawValue)

  if (!value || value <= 0) {
    totalBudgetError.value = '총 예산을 입력해 주세요.'

    return
  }

  const normalized = Math.round(value / STEP) * STEP

  if (normalized < totalUsedAmount.value) {
    totalBudgetError.value = `이미 ${formatCurrency(
      totalUsedAmount.value,
    )}을 사용해서 그보다 낮게 설정할 수 없어요.`

    return
  }

  totalBudget.value = normalized

  totalBudgetInput.value = normalized.toLocaleString('ko-KR')

  lastChangedPocket.value = null
}

const autoBalance = () => {
  autoBalanceError.value = ''

  const fixedType = lastChangedPocket.value

  if (!fixedType || budgetGap.value === 0) {
    return
  }

  const otherTypes = (['essential', 'free', 'future', 'emergency'] as PocketType[]).filter(
    (type) => type !== fixedType,
  )

  if (budgetGap.value > 0) {
    const amountToReduce = budgetGap.value

    const reducibleTotal = otherTypes.reduce(
      (sum, type) => sum + Math.max(budgets[type] - usedAmounts[type], 0),
      0,
    )

    if (reducibleTotal < amountToReduce) {
      autoBalanceError.value =
        '이미 사용한 금액 때문에 자동으로 맞출 수 없어요. 총 예산을 늘려주세요.'

      return
    }

    let remaining = amountToReduce

    for (let index = 0; index < otherTypes.length; index += 1) {
      const type = otherTypes[index]

      if (!type) {
        continue
      }

      const reducible = Math.max(budgets[type] - usedAmounts[type], 0)

      if (reducible <= 0) {
        continue
      }

      let decrease = 0

      if (index === otherTypes.length - 1) {
        decrease = Math.min(reducible, remaining)
      } else {
        const ratio = reducible / reducibleTotal

        decrease = Math.round((amountToReduce * ratio) / STEP) * STEP

        decrease = Math.min(decrease, reducible, remaining)
      }

      budgets[type] -= decrease

      remaining -= decrease

      if (remaining === 0) {
        break
      }
    }

    if (remaining > 0) {
      for (const type of otherTypes) {
        const reducible = budgets[type] - usedAmounts[type]

        if (reducible <= 0) {
          continue
        }

        const decrease = Math.min(reducible, remaining)

        budgets[type] -= decrease

        remaining -= decrease

        if (remaining === 0) {
          break
        }
      }
    }

    return
  }

  const amountToAdd = Math.abs(budgetGap.value)

  const otherTotal = otherTypes.reduce((sum, type) => sum + budgets[type], 0)

  if (otherTotal === 0) {
    const fallback = otherTypes.includes('emergency') ? 'emergency' : otherTypes[0]

    if (fallback) {
      budgets[fallback] += amountToAdd
    }

    return
  }

  let assigned = 0

  otherTypes.forEach((type, index) => {
    let increase = 0

    if (index === otherTypes.length - 1) {
      increase = amountToAdd - assigned
    } else {
      const ratio = budgets[type] / otherTotal

      increase = Math.round((amountToAdd * ratio) / STEP) * STEP
    }

    budgets[type] += increase

    assigned += increase
  })

  const finalGap = totalBudget.value - allocatedTotal.value

  if (finalGap !== 0 && otherTypes[0]) {
    budgets[otherTypes[0]] += finalGap
  }
}

const getPercentage = (amount: number) => {
  if (totalBudget.value <= 0) {
    return 0
  }

  return Math.round((amount / totalBudget.value) * 100)
}

const getSliderStyle = (value: number, min: number) => {
  const range = totalBudget.value - min

  if (range <= 0) {
    return {
      '--slider-progress': '100%',
    }
  }

  const percentage = ((value - min) / range) * 100

  return {
    '--slider-progress': `${Math.min(100, Math.max(0, percentage))}%`,
  }
}

const formatCurrency = (value: number) => {
  return `${Math.round(Number(value ?? 0)).toLocaleString('ko-KR')}원`
}

const handleSave = () => {
  if (!isBalanced.value) {
    return
  }

  router.push({
    name: 'pocket-budget-readjust-confirm',

    query: {
      total: totalBudget.value.toString(),

      essential: budgets.essential.toString(),

      free: budgets.free.toString(),

      future: budgets.future.toString(),

      emergency: budgets.emergency.toString(),

      expectedAsset: expectedAsset.value.toString(),
    },
  })
}

onMounted(load)
</script>

<style scoped>
.budget-page {
  width: 100%;
  min-height: 100vh;

  background: #ffffff;
  color: #171717;
}

.budget-content {
  display: flex;
  flex-direction: column;

  width: 100%;
  max-width: 430px;

  margin: 0 auto;

  padding: 28px 28px 42px;
}

.intro-section {
  margin-bottom: 18px;
}

.intro-title {
  margin: 0;

  font-size: 27px;
  font-weight: 700;
  line-height: 1.35;

  letter-spacing: -0.8px;
}

.intro-description {
  margin: 7px 0 0;

  color: #777777;

  font-size: 13px;
  line-height: 1.6;
}

.usage-guide {
  margin-bottom: 26px;

  padding: 14px 16px;

  border-radius: 10px;

  background: #f5f5f6;
}

.usage-guide__title {
  display: block;

  color: #333333;

  font-size: 13px;
  font-weight: 700;
}

.usage-guide__description {
  margin: 5px 0 0;

  color: #888888;

  font-size: 11px;
  line-height: 1.5;
}

.load-error {
  margin: 20px 0;

  color: #d95050;

  font-size: 13px;
}

.total-card {
  display: flex;
  flex-direction: column;

  gap: 15px;

  padding: 20px 18px;

  border: 1px solid #e4e4e4;
  border-radius: 16px;
}

.total-card__text {
  display: flex;
  flex-direction: column;

  gap: 5px;
}

.total-label {
  color: #171717;

  font-size: 14px;
  font-weight: 700;
}

.total-description {
  color: #888888;

  font-size: 11px;
  line-height: 1.5;
}

.total-input-wrap {
  display: flex;
  align-items: center;
  justify-content: flex-end;

  padding: 0 2px 9px;

  border-bottom: 1px solid #dddddd;
}

.total-input {
  flex: 1;

  min-width: 0;

  border: 0;
  outline: none;

  color: #111111;
  background: transparent;

  text-align: right;

  font-size: 25px;
  font-weight: 800;

  letter-spacing: -0.5px;
}

.total-unit {
  margin-left: 5px;

  color: #171717;

  font-size: 15px;
  font-weight: 700;
}

.total-register-button {
  width: 100%;
  height: 42px;

  border: 0;
  border-radius: 8px;

  color: #ffffff;
  background: #111111;

  font-size: 12px;
  font-weight: 700;

  cursor: pointer;
}

.total-error {
  margin: -5px 0 0;

  color: #e05252;

  font-size: 11px;
  line-height: 1.5;
}

.pocket-section {
  margin-top: 36px;
}

.section-heading {
  margin-bottom: 16px;
}

.section-title {
  margin: 0;

  font-size: 17px;
  font-weight: 700;
}

.section-description {
  margin: 5px 0 0;

  color: #888888;

  font-size: 11px;
}

.pocket-list {
  display: flex;
  flex-direction: column;

  gap: 14px;
}

.pocket-card {
  padding: 18px 16px 16px;

  border-radius: 14px;
}

.pocket-card--essential {
  background: #f0f6ff;
}

.pocket-card--free {
  background: #fff7f1;
}

.pocket-card--future {
  background: #faf2ff;
}

.pocket-card--emergency {
  background: #ebfafa;
}

.pocket-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;

  gap: 12px;
}

.pocket-badge {
  display: inline-flex;
  align-items: center;

  padding: 6px 10px;

  border-radius: 999px;

  font-size: 12px;
  font-weight: 700;
}

.pocket-badge--essential {
  color: #397bc7;
  background: #d5e9ff;
}

.pocket-badge--free {
  color: #ef8e3d;
  background: #ffe1c7;
}

.pocket-badge--future {
  color: #bd6bea;
  background: #eed6ff;
}

.pocket-badge--emergency {
  color: #22a9ad;
  background: #c2eeee;
}

.pocket-min {
  margin: 7px 0 0;

  font-size: 10px;
}

.pocket-min--essential {
  color: #6994c2;
}

.pocket-min--free {
  color: #c98e5d;
}

.pocket-min--future {
  color: #ad82c4;
}

.pocket-min--emergency {
  color: #69a6a8;
}

.pocket-value {
  display: flex;
  flex-direction: column;
  align-items: flex-end;

  gap: 3px;
}

.pocket-value strong {
  color: #171717;

  font-size: 15px;
  font-weight: 800;
}

.pocket-value span {
  color: #777777;

  font-size: 10px;
}

.pocket-slider {
  width: 100%;

  margin-top: 22px;

  appearance: none;

  height: 4px;

  border-radius: 999px;

  outline: none;

  background: linear-gradient(
    to right,
    #171717 0%,
    #171717 var(--slider-progress),
    #dddddd var(--slider-progress),
    #dddddd 100%
  );
}

.pocket-slider::-webkit-slider-thumb {
  width: 18px;
  height: 18px;

  appearance: none;

  border: 4px solid #ffffff;
  border-radius: 50%;

  background: #171717;

  box-shadow: 0 0 0 1px #d0d0d0;

  cursor: pointer;
}

.pocket-slider::-moz-range-thumb {
  width: 12px;
  height: 12px;

  border: 4px solid #ffffff;
  border-radius: 50%;

  background: #171717;

  box-shadow: 0 0 0 1px #d0d0d0;

  cursor: pointer;
}

.used-amount {
  margin: 10px 0 0;

  color: #888888;

  font-size: 10px;
}

.budget-status {
  display: flex;
  flex-direction: column;

  gap: 9px;

  margin-top: 26px;
  margin-bottom: 22px;

  padding: 17px 16px;

  border: 1px solid #e7e7e7;
  border-radius: 12px;

  background: #ffffff;
}

.budget-status--over {
  border-color: #f0c7c7;
}

.budget-status--under {
  border-color: #e5e5e5;
}

.budget-status--balanced {
  border-color: #cde9de;
}

.budget-status__row {
  display: flex;
  align-items: center;
  justify-content: space-between;

  color: #777777;

  font-size: 12px;
}

.budget-status__row strong {
  color: #171717;

  font-size: 15px;
  font-weight: 700;
}

.budget-status__divider {
  height: 1px;

  background: #eeeeee;
}

.status-message {
  display: flex;
  flex-direction: column;

  gap: 3px;

  color: #666666;

  font-size: 11px;
}

.status-message--error {
  color: #d95050;
}

.status-message--success {
  color: #15936f;
}

.auto-button {
  width: 100%;
  height: 45px;

  margin-top: 3px;

  border: 0;
  border-radius: 9px;

  background: #f0f0f0;

  color: #171717;

  font-size: 13px;
  font-weight: 700;

  cursor: pointer;
}

.auto-error {
  margin: 0;

  color: #e05252;

  font-size: 10px;
  line-height: 1.5;
}

.forecast-error {
  width: 100%;

  padding: 18px;

  border-radius: 12px;

  color: #777777;
  background: #f5f5f6;

  font-size: 12px;
  text-align: center;
}

.save-button {
  width: 100%;
  height: 60px;

  margin-top: 26px;

  border: 0;
  border-radius: 10px;

  color: #ffffff;
  background: #111111;

  font-size: 15px;
  font-weight: 700;

  cursor: pointer;
}

.save-button--disabled {
  color: #999999;
  background: #e5e5e5;

  cursor: not-allowed;
}
</style>

<template>
  <div class="budget-page">
    <main class="budget-content">
      <!-- 페이지 설명 -->
      <section class="intro-section">
        <h2 class="intro-title">포켓 예산을 조정해 보세요</h2>

        <p class="intro-description">
          이미 사용한 금액보다 낮게는 줄일 수 없어요.<br />
          남아 있는 예산 안에서 자유롭게 조정해 보세요.
        </p>
      </section>

      <!-- 안내 -->
      <section class="usage-guide">
        <strong class="usage-guide__title"> 이미 사용한 금액은 변경할 수 없어요. </strong>

        <p class="usage-guide__description">각 포켓은 현재 사용액 이상으로만 조정할 수 있어요.</p>
      </section>

      <!-- 총 예산 -->
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

      <!-- 포켓 -->
      <section class="pocket-section">
        <div class="section-heading">
          <h3 class="section-title">포켓별 예산</h3>

          <p class="section-description">전체 합계가 총 예산과 같아야 저장할 수 있어요.</p>
        </div>

        <div class="pocket-list">
          <!-- 필수 -->
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

          <!-- 자유 -->
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

          <!-- 미래자산 -->
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

          <!-- 비상금 -->
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

      <!-- 현재 배분 상태 -->
      <section class="budget-status" :class="statusClass">
        <div class="budget-status__row">
          <span> 현재 배분 </span>

          <strong>
            {{ formatCurrency(allocatedTotal) }}
          </strong>
        </div>

        <div class="budget-status__row">
          <span> 총 예산 </span>

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

      <!-- 예상 자산 -->
      <BudgetForecastCard
        :expected-asset="expectedAsset"
        :previous-asset="previousAsset"
        :difference="assetDifference"
        :future-budget="budgets.future"
      />

      <!-- 저장 -->
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
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import BudgetForecastCard from '@/components/pocket/BudgetForecastCard.vue'

type PocketType = 'essential' | 'free' | 'future' | 'emergency'

const router = useRouter()

const STEP = 50_000

/*
 * TODO:
 * 추후 백엔드에서 현재 진행 중인
 * 이번 달 포켓 예산 조회 API로 교체
 */
const totalBudget = ref(3_000_000)

const budgets = reactive<Record<PocketType, number>>({
  essential: 1_000_000,
  free: 700_000,
  future: 500_000,
  emergency: 800_000,
})

/*
 * 이미 이번 달에 사용한 금액
 *
 * 이 값이 각 슬라이더의 최소값이 된다.
 *
 * TODO:
 * 백엔드 조회값으로 교체
 */
const usedAmounts = reactive<Record<PocketType, number>>({
  essential: 450_000,
  free: 300_000,
  future: 300_000,
  emergency: 50_000,
})

const totalBudgetInput = ref(totalBudget.value.toLocaleString('ko-KR'))

const totalBudgetError = ref('')
const autoBalanceError = ref('')

const lastChangedPocket = ref<PocketType | null>(null)

/*
 * 예상 자산 임시 데이터
 */
const previousAsset = ref(24_000_000)

const expectedAsset = computed(() => {
  return Math.round(previousAsset.value + budgets.future * 14.4)
})

const assetDifference = computed(() => {
  return expectedAsset.value - previousAsset.value
})

/*
 * 지금까지 실제 사용한 총 금액.
 *
 * 총예산은 절대로 이 값보다
 * 낮아질 수 없다.
 */
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
  return budgetGap.value === 0
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

const handlePocketInput = (type: PocketType, event: Event) => {
  const target = event.target as HTMLInputElement

  const requestedValue = Number(target.value)

  /*
   * 핵심:
   * 이미 사용한 금액보다 내려갈 수 없음
   */
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

  /*
   * 이미 쓴 총액보다
   * 총예산을 낮출 수 없음
   */
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

/*
 * 자동으로 맞추기
 *
 * 마지막으로 사용자가 만진 포켓은 유지.
 *
 * 나머지 세 포켓에서
 * 조정 가능한 범위만 사용한다.
 *
 * 어떤 경우에도
 * usedAmounts 아래로 내려가지 않는다.
 */
const autoBalance = () => {
  autoBalanceError.value = ''

  const fixedType = lastChangedPocket.value

  if (!fixedType) {
    return
  }

  if (budgetGap.value === 0) {
    return
  }

  const otherTypes: PocketType[] = ['essential', 'free', 'future', 'emergency'].filter(
    (type) => type !== fixedType,
  ) as PocketType[]

  /*
   * 현재 총예산 초과
   * → 나머지 포켓에서 줄여야 함
   */
  if (budgetGap.value > 0) {
    const amountToReduce = budgetGap.value

    /*
     * 각 포켓에서 줄일 수 있는 금액
     * = 현재 예산 - 이미 사용한 금액
     */
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

    otherTypes.forEach((type, index) => {
      const reducible = Math.max(budgets[type] - usedAmounts[type], 0)

      if (reducible === 0) {
        return
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
    })

    /*
     * 반올림 때문에 남은 금액 보정
     */
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

  /*
   * 총예산보다 덜 배분
   * → 남는 돈을 다른 세 포켓에 비율대로 증가
   */
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

  /*
   * 반올림 보정
   */
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
  return `${Math.round(value).toLocaleString('ko-KR')}원`
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

/* =========================
   INTRO
========================= */

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

/* =========================
   사용액 안내
========================= */

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

/* =========================
   총예산
========================= */

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
  font-size: 17px;
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

  padding: 0 14px;

  border: 1px solid #e1e1e1;
  border-radius: 10px;

  background: #f7f7f7;
}

.total-input {
  flex: 1;

  min-width: 0;
  height: 54px;

  padding: 0;

  border: 0;
  outline: 0;

  background: transparent;

  text-align: right;

  font-size: 21px;
  font-weight: 800;
}

.total-unit {
  margin-left: 5px;

  font-size: 17px;
  font-weight: 700;
}

.total-register-button {
  width: 100%;
  height: 48px;

  border: 0;
  border-radius: 9px;

  color: #ffffff;
  background: #111111;

  font-size: 14px;
  font-weight: 700;

  cursor: pointer;
}

.total-error {
  margin: -5px 0 0;

  color: #e05252;

  font-size: 11px;
  line-height: 1.5;
}

/* =========================
   POCKET
========================= */

.pocket-section {
  margin-top: 28px;
}

.section-heading {
  margin-bottom: 15px;
}

.section-title {
  margin: 0;

  font-size: 18px;
  font-weight: 700;
}

.section-description {
  margin: 5px 0 0;

  color: #888888;

  font-size: 11px;
  line-height: 1.5;
}

.pocket-list {
  display: flex;
  flex-direction: column;

  gap: 12px;
}

.pocket-card {
  padding: 17px 16px 15px;

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

  margin-bottom: 14px;
}

.pocket-badge {
  display: inline-flex;

  padding: 5px 9px;

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
  color: #6692c4;
}

.pocket-min--free {
  color: #ca8958;
}

.pocket-min--future {
  color: #b281c7;
}

.pocket-min--emergency {
  color: #58a4a4;
}

.pocket-value {
  display: flex;
  flex-direction: column;
  align-items: flex-end;

  gap: 5px;
}

.pocket-value strong {
  font-size: 16px;
  font-weight: 800;
}

.pocket-value span {
  padding: 2px 6px;

  border-radius: 999px;

  background: rgba(255, 255, 255, 0.7);

  color: #888888;

  font-size: 10px;
}

.pocket-slider {
  width: 100%;
  height: 4px;

  margin: 0;

  appearance: none;
  -webkit-appearance: none;

  border-radius: 999px;

  background: linear-gradient(
    to right,
    #111111 0%,
    #111111 var(--slider-progress),
    #d5d5d5 var(--slider-progress),
    #d5d5d5 100%
  );

  cursor: pointer;
}

.pocket-slider::-webkit-slider-thumb {
  width: 18px;
  height: 18px;

  appearance: none;
  -webkit-appearance: none;

  border: 0;
  border-radius: 50%;

  background: #111111;
}

.pocket-slider::-moz-range-thumb {
  width: 18px;
  height: 18px;

  border: 0;
  border-radius: 50%;

  background: #111111;
}

.used-amount {
  margin: 9px 0 0;

  color: #999999;

  font-size: 9px;
}

/* =========================
   STATUS
========================= */

.budget-status {
  display: flex;
  flex-direction: column;

  gap: 11px;

  margin-top: 24px;
  margin-bottom: 18px;

  padding: 18px 16px;

  border: 1px solid #e1e1e1;
  border-radius: 14px;
}

.budget-status--over {
  border-color: #efd0d0;
  background: #fffafa;
}

.budget-status--balanced {
  border-color: #c7e8da;
  background: #f8fcfa;
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

/* =========================
   SAVE
========================= */

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

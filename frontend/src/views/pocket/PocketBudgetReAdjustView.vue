<template>
  <div class="budget-page">
    <main class="budget-content">
      <section v-if="loading" class="state-section">
        <p>이번 달 예산을 불러오고 있어요.</p>
      </section>

      <section v-else-if="loadError" class="state-section">
        <p class="state-error">{{ loadError }}</p>

        <button class="retry-button" type="button" @click="loadCurrentBudget">다시 불러오기</button>
      </section>

      <template v-else>
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
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import BudgetForecastCard from '@/components/pocket/BudgetForecastCard.vue'

import { budgetAdjustmentApi, type BudgetAdjustmentPocketType } from '@/api/budgetAdjustment'

import { ApiError } from '@/api/types'

type PocketType = 'essential' | 'free' | 'future' | 'emergency'

const router = useRouter()

const STEP = 50_000

const loading = ref(true)
const loadError = ref('')

const totalBudget = ref(0)
const minimumTotalBudget = ref(0)

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

const lastChangedPocket = ref<PocketType | null>(null)

/*
 * 아직 자산 예측 API는 별도 연결 전이라
 * 기존 화면 계산을 유지한다.
 */
const previousAsset = ref(24_000_000)

const expectedAsset = computed(() => {
  return Math.round(previousAsset.value + budgets.future * 14.4)
})

const assetDifference = computed(() => {
  return expectedAsset.value - previousAsset.value
})

const backendToFrontendPocketType = (type: BudgetAdjustmentPocketType): PocketType => {
  switch (type) {
    case 'ESSENTIAL':
      return 'essential'

    case 'FREE':
      return 'free'

    case 'FUTURE_ASSET':
      return 'future'

    case 'EMERGENCY':
      return 'emergency'
  }
}

/**
 * DB에서 이번 달 확정 예산 + 실제 사용액을 가져온다.
 */
const loadCurrentBudget = async () => {
  loading.value = true
  loadError.value = ''

  try {
    const result = await budgetAdjustmentApi.getCurrent()

    totalBudget.value = result.totalBudgetAmount
    minimumTotalBudget.value = result.minimumTotalBudget

    totalBudgetInput.value = result.totalBudgetAmount.toLocaleString('ko-KR')

    for (const pocket of result.pockets) {
      const type = backendToFrontendPocketType(pocket.pocketType)

      budgets[type] = pocket.targetAmount
      usedAmounts[type] = pocket.minimumAmount
    }

    lastChangedPocket.value = null
  } catch (error) {
    loadError.value =
      error instanceof ApiError ? error.message : '이번 달 예산을 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCurrentBudget()
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
   * 서버 GET이 내려주는 minimumTotalBudget과
   * 화면에서 계산한 사용액 합계 중 더 큰 값을
   * 최소 총예산으로 사용한다.
   */
  const minimum = Math.max(minimumTotalBudget.value, totalUsedAmount.value)

  if (normalized < minimum) {
    totalBudgetError.value =
      `이미 ${formatCurrency(minimum)}을 사용해서 ` + '그보다 낮게 설정할 수 없어요.'

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

  const allTypes: PocketType[] = ['essential', 'free', 'future', 'emergency']

  const otherTypes = allTypes.filter((type) => type !== fixedType)

  /*
   * 총예산을 초과한 경우:
   * 나머지 포켓에서 사용액 밑으로 내려가지 않는
   * 범위만큼 줄인다.
   */
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

    for (const type of otherTypes) {
      if (remaining <= 0) {
        break
      }

      const reducible = Math.max(budgets[type] - usedAmounts[type], 0)

      const decrease = Math.min(reducible, remaining)

      budgets[type] -= decrease
      remaining -= decrease
    }

    return
  }

  /*
   * 총예산보다 적게 배분한 경우:
   * 남는 금액을 다른 포켓들에 배분한다.
   */
  let amountToAdd = Math.abs(budgetGap.value)

  for (const type of otherTypes) {
    if (amountToAdd <= 0) {
      break
    }

    budgets[type] += amountToAdd
    amountToAdd = 0
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

  /*
   * 여기서는 아직 DB를 수정하지 않는다.
   * 확인 화면에서 최종 버튼을 눌렀을 때
   * PATCH API를 호출한다.
   */
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
  min-height: calc(100vh - 120px);
  margin: 0 auto;
  padding: 34px 28px 40px;
}

.state-section {
  padding: 80px 0;
  text-align: center;
  color: #666666;
  font-size: 14px;
}

.state-error {
  margin: 0 0 16px;
  color: #d64545;
}

.retry-button {
  height: 42px;
  padding: 0 18px;
  border: 0;
  border-radius: 9px;
  background: #111111;
  color: #ffffff;
  font-weight: 700;
}

.intro-section {
  margin-bottom: 28px;
}

.intro-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -1px;
}

.intro-description {
  margin: 8px 0 0;
  color: #666666;
  font-size: 14px;
  line-height: 1.55;
}

.usage-guide {
  padding: 18px;
  margin-bottom: 20px;
  border-radius: 12px;
  background: #f5f6f8;
}

.usage-guide__title {
  display: block;
  font-size: 14px;
  font-weight: 700;
}

.usage-guide__description {
  margin: 6px 0 0;
  color: #777777;
  font-size: 12px;
}

.total-card {
  padding: 20px;
  border: 1px solid #e7e7e7;
  border-radius: 14px;
  background: #ffffff;
}

.total-card__text {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.total-label {
  font-size: 15px;
  font-weight: 700;
}

.total-description {
  color: #777777;
  font-size: 12px;
  line-height: 1.45;
}

.total-input-wrap {
  display: flex;
  align-items: center;
  margin-top: 18px;
  border-bottom: 1px solid #d9d9d9;
}

.total-input {
  flex: 1;
  min-width: 0;
  padding: 8px 0;
  border: 0;
  outline: none;
  color: #111111;
  background: transparent;
  font-size: 26px;
  font-weight: 800;
  text-align: right;
}

.total-unit {
  margin-left: 7px;
  font-size: 16px;
  font-weight: 700;
}

.total-register-button {
  width: 100%;
  height: 44px;
  margin-top: 16px;
  border: 0;
  border-radius: 9px;
  color: #ffffff;
  background: #171717;
  font-size: 13px;
  font-weight: 700;
}

.total-error {
  margin: 10px 0 0;
  color: #d64545;
  font-size: 12px;
}

.pocket-section {
  margin-top: 34px;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.section-description {
  margin: 6px 0 0;
  color: #777777;
  font-size: 12px;
}

.pocket-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 18px;
}

.pocket-card {
  padding: 18px;
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
  justify-content: space-between;
  gap: 12px;
}

.pocket-badge {
  display: inline-flex;
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
  font-size: 11px;
}

.pocket-min--essential {
  color: #397bc7;
}

.pocket-min--free {
  color: #ef8e3d;
}

.pocket-min--future {
  color: #bd6bea;
}

.pocket-min--emergency {
  color: #22a9ad;
}

.pocket-value {
  flex-shrink: 0;
  text-align: right;
}

.pocket-value strong {
  display: block;
  font-size: 16px;
  font-weight: 800;
}

.pocket-value span {
  color: #777777;
  font-size: 11px;
}

.pocket-slider {
  --slider-progress: 0%;

  width: 100%;
  height: 4px;
  margin-top: 22px;
  appearance: none;
  border-radius: 999px;
  outline: none;

  background: linear-gradient(
    to right,
    #171717 0%,
    #171717 var(--slider-progress),
    rgba(0, 0, 0, 0.13) var(--slider-progress),
    rgba(0, 0, 0, 0.13) 100%
  );
}

.pocket-slider::-webkit-slider-thumb {
  width: 20px;
  height: 20px;
  appearance: none;
  border: 4px solid #ffffff;
  border-radius: 50%;
  background: #171717;
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.18);
  cursor: pointer;
}

.pocket-slider::-moz-range-thumb {
  width: 13px;
  height: 13px;
  border: 4px solid #ffffff;
  border-radius: 50%;
  background: #171717;
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.18);
  cursor: pointer;
}

.used-amount {
  margin: 11px 0 0;
  color: #777777;
  font-size: 11px;
}

.budget-status {
  margin-top: 22px;
  padding: 18px;
  border: 1px solid #e7e7e7;
  border-radius: 14px;
  background: #ffffff;
}

.budget-status__row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #666666;
  font-size: 13px;
}

.budget-status__row strong {
  color: #171717;
}

.budget-status__divider {
  height: 1px;
  margin: 14px 0;
  background: #eeeeee;
}

.status-message {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #555555;
  font-size: 12px;
}

.status-message strong {
  color: #171717;
  font-size: 13px;
}

.status-message--error strong {
  color: #d64545;
}

.status-message--success strong {
  color: #248c6b;
}

.auto-button {
  width: 100%;
  height: 40px;
  margin-top: 14px;
  border: 0;
  border-radius: 9px;
  background: #f2f2f2;
  color: #171717;
  font-size: 12px;
  font-weight: 700;
}

.auto-error {
  margin: 10px 0 0;
  color: #d64545;
  font-size: 11px;
  line-height: 1.5;
}

.save-button {
  width: 100%;
  height: 58px;
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
  background: #d7d7d7;
  cursor: default;
}
</style>

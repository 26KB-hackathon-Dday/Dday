<template>
  <div class="budget-page">
    <main class="budget-content">
      <section class="intro-section">
        <h2 class="intro-title">포켓 예산을 조정해 보세요</h2>

        <p class="intro-description">
          이미 사용하거나 달성한 금액은 줄일 수 없어요.<br />
          남아 있는 예산은 자유롭게 다시 배분할 수 있어요.
        </p>
      </section>

      <section class="usage-guide">
        <strong class="usage-guide__title"> 사용한 금액 아래로는 줄일 수 없어요. </strong>

        <p class="usage-guide__description">빨간색 구간은 조정할 수 없는 영역이에요.</p>
      </section>

      <p v-if="loadError" class="load-error">
        {{ loadError }}
      </p>

      <div v-if="isLoading" class="loading-state">예산 정보를 불러오는 중이에요.</div>

      <template v-else>
        <!-- 총 예산 -->
        <section class="total-card">
          <div class="total-card__text">
            <span class="total-label"> 이번 달 총 예산 </span>

            <span class="total-description"> 변경한 총 예산은 바로 저장돼요. </span>
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

          <button
            class="total-register-button"
            type="button"
            :disabled="updatingTotalBudget"
            @click="applyTotalBudget"
          >
            {{ updatingTotalBudget ? '저장 중…' : '총 예산 등록 완료' }}
          </button>

          <p v-if="totalBudgetError" class="total-error">
            {{ totalBudgetError }}
          </p>

          <p v-if="totalBudgetSuccess" class="total-success">총 예산이 저장됐어요.</p>
        </section>

        <section class="adjust-guide">
          <strong class="adjust-guide__title"> 포켓별 예산 </strong>

          <p class="adjust-guide__description">
            각 포켓의 금액을 조정해 주세요.<br />
            전체 합계가 총 예산과 같아야 저장할 수 있어요.
          </p>
        </section>

        <!-- 실시간 상태 -->
        <section class="budget-live-status" :class="liveStatusClass">
          <template v-if="hasLockedAmountViolation">
            <strong class="budget-live-status__title"> 조정이 필요한 포켓이 있어요 </strong>

            <span class="budget-live-status__description">
              이미 사용하거나 달성한 금액보다 낮은 포켓을 조정해 주세요.
            </span>
          </template>

          <template v-else-if="budgetGap > 0">
            <strong class="budget-live-status__title">
              {{ formatCurrency(budgetGap) }}
              초과했어요
            </strong>

            <span class="budget-live-status__description">
              조정 가능한 포켓에서
              {{ formatCurrency(budgetGap) }}
              줄여주세요.
            </span>
          </template>

          <template v-else-if="budgetGap < 0">
            <strong class="budget-live-status__title">
              아직
              {{ formatCurrency(Math.abs(budgetGap)) }}
              남았어요
            </strong>

            <span class="budget-live-status__description"> 원하는 포켓에 더 배분해 주세요. </span>
          </template>

          <template v-else>
            <strong class="budget-live-status__title"> 예산 배분이 완료됐어요 </strong>

            <span class="budget-live-status__description"> 총 예산에 맞게 모두 배분했어요. </span>
          </template>
        </section>

        <!-- 포켓 슬라이더 -->
        <section class="slider-list">
          <PocketBudgetSlider
            label="필수 포켓"
            variant="essential"
            :value="budgets.essential"
            :min="lockedAmounts.essential"
            :max="totalBudget"
            :allowed-max="totalBudget"
            :step="500"
            @change="handlePocketChange('essential', $event)"
          />

          <PocketBudgetSlider
            label="자유 포켓"
            variant="free"
            :value="budgets.free"
            :min="lockedAmounts.free"
            :max="totalBudget"
            :allowed-max="totalBudget"
            :step="500"
            @change="handlePocketChange('free', $event)"
          />

          <PocketBudgetSlider
            label="미래자산 포켓"
            variant="future"
            :value="budgets.future"
            :min="lockedAmounts.future"
            :max="totalBudget"
            :allowed-max="totalBudget"
            :step="500"
            @change="handlePocketChange('future', $event)"
          />

          <PocketBudgetSlider
            label="비상금 포켓"
            variant="emergency"
            :value="budgets.emergency"
            :min="0"
            :max="totalBudget"
            :allowed-max="emergencyMax"
            :step="500"
            @change="handlePocketChange('emergency', $event)"
          />
        </section>

        <button
          class="save-button"
          :class="{
            'save-button--disabled': !canSave,
          }"
          type="button"
          :disabled="!canSave"
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

import PocketBudgetSlider from '@/components/pocket/PocketBudgetSlider.vue'

import { budgetAdjustmentApi, type BudgetAdjustmentPocketType } from '@/api/budgetAdjustment'

import { assetForecastApi } from '@/api/assetForecast'

import { ApiError } from '@/api/types'

type PocketType = 'essential' | 'free' | 'future' | 'emergency'

const router = useRouter()

const totalBudget = ref(0)

const budgets = reactive<Record<PocketType, number>>({
  essential: 0,
  free: 0,
  future: 0,
  emergency: 0,
})

const lockedAmounts = reactive<Record<PocketType, number>>({
  essential: 0,
  free: 0,
  future: 0,
  emergency: 0,
})

const totalBudgetInput = ref('')

const totalBudgetError = ref('')

const totalBudgetSuccess = ref(false)

const loadError = ref('')

const isLoading = ref(true)

const updatingTotalBudget = ref(false)

const currentAsset = ref(0)

const remainingMonths = ref(0)

const toSafeNumber = (value: unknown): number => {
  const numberValue = Number(value)

  return Number.isFinite(numberValue) ? numberValue : 0
}

/**
 * 이미 사용/달성한 금액을 기준으로
 * 총예산이 내려갈 수 있는 최소값.
 */
const minimumTotalBudget = computed(() => {
  return lockedAmounts.essential + lockedAmounts.free + lockedAmounts.future
})

/**
 * 현재 필수 / 자유 / 미래자산 금액을 기준으로
 * 비상금이 가질 수 있는 최대 금액.
 */
const emergencyMax = computed(() => {
  return Math.max(0, totalBudget.value - budgets.essential - budgets.free - budgets.future)
})

/**
 * 비상금이 허용 최대값을 넘었다면
 * 최대값으로 자동 조정한다.
 *
 * 중요한 점:
 * 비상금이 최대값보다 작은 경우에는 건드리지 않는다.
 *
 * 즉 사용자가 회색 영역 안에서
 * 직접 비상금을 줄이는 것은 가능하다.
 */
const clampEmergencyToMax = () => {
  if (budgets.emergency > emergencyMax.value) {
    budgets.emergency = emergencyMax.value
  }
}

const allocatedTotal = computed(() => {
  return budgets.essential + budgets.free + budgets.future + budgets.emergency
})

const budgetGap = computed(() => {
  return allocatedTotal.value - totalBudget.value
})

const isBalanced = computed(() => {
  return totalBudget.value > 0 && budgetGap.value === 0
})

const hasLockedAmountViolation = computed(() => {
  return (
    budgets.essential < lockedAmounts.essential ||
    budgets.free < lockedAmounts.free ||
    budgets.future < lockedAmounts.future
  )
})

const canSave = computed(() => {
  return isBalanced.value && !hasLockedAmountViolation.value
})

const expectedAsset = computed(() => {
  return currentAsset.value + budgets.future * remainingMonths.value
})

const liveStatusClass = computed(() => {
  if (hasLockedAmountViolation.value || budgetGap.value > 0) {
    return 'budget-live-status--over'
  }

  if (budgetGap.value < 0) {
    return 'budget-live-status--under'
  }

  return 'budget-live-status--balanced'
})

const toLocalPocketType = (type: BudgetAdjustmentPocketType): PocketType => {
  const map: Record<BudgetAdjustmentPocketType, PocketType> = {
    ESSENTIAL: 'essential',

    FREE: 'free',

    FUTURE_ASSET: 'future',

    EMERGENCY: 'emergency',
  }

  return map[type]
}

/**
 * 현재 예산 조회.
 */
const loadCurrentBudget = async () => {
  const response = await budgetAdjustmentApi.findCurrent()

  totalBudget.value = Math.max(0, toSafeNumber(response.totalBudgetAmount))

  totalBudgetInput.value = totalBudget.value.toLocaleString('ko-KR')

  budgets.essential = 0
  budgets.free = 0
  budgets.future = 0
  budgets.emergency = 0

  lockedAmounts.essential = 0
  lockedAmounts.free = 0
  lockedAmounts.future = 0
  lockedAmounts.emergency = 0

  const pockets = response.pockets ?? []

  pockets.forEach((pocket) => {
    const type = toLocalPocketType(pocket.pocketType)

    const targetAmount = Math.max(0, toSafeNumber(pocket.targetAmount))

    const usedAmount = Math.max(0, toSafeNumber(pocket.usedAmount))

    /**
     * 현재 DB 값을 먼저 그대로 표시.
     */
    budgets[type] = targetAmount

    if (type === 'emergency') {
      lockedAmounts.emergency = 0

      return
    }

    if (type === 'future') {
      lockedAmounts.future = Math.max(usedAmount, targetAmount)

      return
    }

    lockedAmounts[type] = usedAmount
  })

  /**
   * 단, 비상금은 현재 다른 세 포켓을 기준으로
   * 허용 최대값을 넘어갈 수 없으므로
   * 화면 진입 시 최대값까지만 자동으로 내린다.
   *
   * 이렇게 하면 thumb가 빨간 영역에
   * 위치하는 일이 없다.
   */
  clampEmergencyToMax()
}

const loadForecast = async () => {
  try {
    const response = await assetForecastApi.find()

    currentAsset.value = Math.max(0, toSafeNumber(response.currentAsset))

    remainingMonths.value = Math.max(0, toSafeNumber(response.remainingMonths))
  } catch {
    currentAsset.value = 0

    remainingMonths.value = 0
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

const handleTotalInput = (event: Event) => {
  totalBudgetSuccess.value = false

  totalBudgetError.value = ''

  const target = event.target as HTMLInputElement

  const onlyNumbers = target.value.replace(/[^0-9]/g, '')

  if (!onlyNumbers) {
    totalBudgetInput.value = ''

    return
  }

  totalBudgetInput.value = Number(onlyNumbers).toLocaleString('ko-KR')
}

const applyTotalBudget = async () => {
  if (updatingTotalBudget.value) {
    return
  }

  totalBudgetError.value = ''

  totalBudgetSuccess.value = false

  const rawValue = totalBudgetInput.value.replace(/,/g, '')

  const value = Number(rawValue)

  if (!Number.isFinite(value) || value <= 0) {
    totalBudgetError.value = '총 예산을 입력해 주세요.'

    return
  }

  const newTotal = Math.round(value)

  if (newTotal < minimumTotalBudget.value) {
    totalBudgetError.value = `최소 총 예산은 ${formatCurrency(
      minimumTotalBudget.value,
    )} 이상부터 설정할 수 있어요.`

    return
  }

  updatingTotalBudget.value = true

  try {
    const response = await budgetAdjustmentApi.updateTotalBudget(newTotal)

    totalBudget.value = Math.max(0, toSafeNumber(response.totalBudgetAmount))

    totalBudgetInput.value = totalBudget.value.toLocaleString('ko-KR')

    /**
     * 총예산이 줄어서
     * 기존 비상금이 최대 허용값을 넘게 되면
     * 비상금만 자동으로 경계까지 이동.
     */
    clampEmergencyToMax()

    totalBudgetSuccess.value = true
  } catch (error) {
    totalBudgetError.value =
      error instanceof ApiError
        ? error.message
        : error instanceof Error
          ? error.message
          : '총 예산 수정에 실패했어요.'
  } finally {
    updatingTotalBudget.value = false
  }
}

/**
 * 포켓 조정.
 *
 * 필수 / 자유 / 미래자산은
 * 사용자가 직접 변경한다.
 *
 * 대신 이 세 포켓 때문에
 * 비상금 최대 가능 금액이 내려가면
 * 비상금만 자동으로 최대값까지 줄인다.
 */
const handlePocketChange = (type: PocketType, value: number) => {
  const safeValue = Math.max(0, toSafeNumber(value))

  /**
   * 비상금 자체를 움직이는 경우.
   *
   * 회색 허용 영역 안에서만 움직인다.
   */
  if (type === 'emergency') {
    budgets.emergency = Math.min(emergencyMax.value, safeValue)

    return
  }

  /**
   * 필수 / 자유 / 미래자산.
   */
  budgets[type] = Math.max(lockedAmounts[type], safeValue)

  /**
   * 다른 포켓이 증가해서
   * 비상금 최대 가능액이 줄었다면
   * 기존 비상금을 최대 경계까지 자동으로 이동.
   */
  clampEmergencyToMax()
}

const handleSave = () => {
  if (!canSave.value) {
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

const formatCurrency = (value: unknown): string => {
  return `${Math.round(toSafeNumber(value)).toLocaleString('ko-KR')}원`
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

  padding: 34px 28px 40px;

  box-sizing: border-box;
}

.intro-section {
  margin-bottom: 20px;
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
  margin-bottom: 26px;

  padding: 16px 18px;

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
  line-height: 1.5;
}

.loading-state {
  padding: 60px 0;

  color: #777777;

  font-size: 13px;

  text-align: center;
}

.load-error {
  margin: 0 0 20px;

  color: #d95050;

  font-size: 13px;
}

.total-card {
  display: flex;
  flex-direction: column;

  gap: 16px;

  width: 100%;

  padding: 22px 18px;

  border: 1px solid #e5e5e5;

  border-radius: 16px;

  box-sizing: border-box;
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

  font-size: 12px;
}

.total-input-wrap {
  display: flex;

  align-items: center;

  padding: 0 16px;

  border: 1px solid #dedede;

  border-radius: 12px;

  background: #f7f7f7;
}

.total-input {
  flex: 1;

  min-width: 0;

  height: 56px;

  border: 0;

  outline: none;

  background: transparent;

  text-align: right;

  font-size: 22px;

  font-weight: 800;
}

.total-unit {
  margin-left: 5px;

  font-size: 18px;

  font-weight: 700;
}

.total-register-button {
  width: 100%;

  height: 50px;

  border: 0;

  border-radius: 10px;

  color: #ffffff;

  background: #111111;

  font-size: 15px;

  font-weight: 700;

  cursor: pointer;
}

.total-register-button:disabled {
  opacity: 0.55;

  cursor: not-allowed;
}

.total-error {
  margin: 0;

  color: #e05252;

  font-size: 12px;

  line-height: 1.5;
}

.total-success {
  margin: 0;

  color: #168867;

  font-size: 12px;

  font-weight: 600;
}

.adjust-guide {
  margin-top: 32px;

  margin-bottom: 14px;
}

.adjust-guide__title {
  display: block;

  font-size: 18px;

  font-weight: 700;
}

.adjust-guide__description {
  margin: 6px 0 0;

  color: #777777;

  font-size: 13px;

  line-height: 1.55;
}

.budget-live-status {
  position: sticky;

  top: 78px;

  z-index: 20;

  display: flex;

  flex-direction: column;

  gap: 4px;

  margin-bottom: 14px;

  padding: 13px 16px;

  border: 1px solid transparent;

  border-radius: 12px;

  backdrop-filter: blur(8px);

  -webkit-backdrop-filter: blur(8px);

  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
}

.budget-live-status--over {
  color: #df4e4e;

  border-color: #f2cccc;

  background: rgba(255, 247, 247, 0.96);
}

.budget-live-status--under {
  color: #666666;

  border-color: #e4e4e4;

  background: rgba(248, 248, 248, 0.96);
}

.budget-live-status--balanced {
  color: #168867;

  border-color: #cbe7dc;

  background: rgba(246, 252, 249, 0.96);
}

.budget-live-status__title {
  font-size: 14px;

  font-weight: 800;
}

.budget-live-status__description {
  font-size: 12px;

  line-height: 1.5;
}

.slider-list {
  display: flex;

  flex-direction: column;

  gap: 14px;
}

.save-button {
  width: 100%;

  height: 64px;

  margin-top: 28px;

  border: 0;

  border-radius: 10px;

  color: #ffffff;

  background: #111111;

  font-size: 17px;

  font-weight: 700;

  cursor: pointer;
}

.save-button--disabled {
  color: #999999;

  background: #e7e7e7;

  cursor: not-allowed;
}
</style>

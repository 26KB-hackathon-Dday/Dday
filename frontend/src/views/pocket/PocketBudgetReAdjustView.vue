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
        <strong class="usage-guide__title"> 잠금선 아래로는 줄일 수 없어요. </strong>

        <p class="usage-guide__description">
          필수·자유 포켓은 사용액, 미래자산은 달성액이 최소 기준이에요. 비상금은 0원까지 조정할 수
          있어요.
        </p>
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

        <!-- 포켓별 예산 -->
        <section class="adjust-guide">
          <strong class="adjust-guide__title"> 포켓별 예산 </strong>

          <p class="adjust-guide__description">
            잠금선보다 왼쪽으로는 조정할 수 없어요.<br />
            전체 합계가 총 예산과 같아야 저장할 수 있어요.
          </p>
        </section>

        <!-- 실시간 상태 -->
        <section class="budget-live-status" :class="liveStatusClass">
          <template v-if="budgetGap > 0">
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
              {{ formatCurrency(Math.abs(budgetGap)) }}
              남았어요
            </strong>

            <span class="budget-live-status__description">
              원하는 포켓에
              {{ formatCurrency(Math.abs(budgetGap)) }}
              더 배분해 주세요.
            </span>
          </template>

          <template v-else>
            <strong class="budget-live-status__title"> 예산 배분이 완료됐어요 </strong>

            <span class="budget-live-status__description"> 총 예산에 맞게 모두 배분했어요. </span>
          </template>
        </section>

        <!-- 슬라이더 -->
        <section class="slider-list">
          <PocketBudgetSlider
            label="필수 포켓"
            variant="essential"
            :value="budgets.essential"
            :min="lockedAmounts.essential"
            :max="totalBudget"
            :used-amount="lockedAmounts.essential"
            :show-used-marker="true"
            :step="500"
            @change="handlePocketChange('essential', $event)"
          />

          <PocketBudgetSlider
            label="자유 포켓"
            variant="free"
            :value="budgets.free"
            :min="lockedAmounts.free"
            :max="totalBudget"
            :used-amount="lockedAmounts.free"
            :show-used-marker="true"
            :step="500"
            @change="handlePocketChange('free', $event)"
          />

          <PocketBudgetSlider
            label="미래자산 포켓"
            variant="future"
            :value="budgets.future"
            :min="lockedAmounts.future"
            :max="totalBudget"
            :used-amount="lockedAmounts.future"
            :show-used-marker="true"
            :step="500"
            @change="handlePocketChange('future', $event)"
          />

          <PocketBudgetSlider
            label="비상금 포켓"
            variant="emergency"
            :value="budgets.emergency"
            :min="0"
            :max="emergencyMax"
            :show-used-marker="false"
            :step="500"
            @change="handlePocketChange('emergency', $event)"
          />
        </section>

        <BudgetForecastCard
          v-if="forecastLoaded"
          :current-asset="currentAsset"
          :expected-asset="expectedAsset"
          :future-budget="budgets.future"
          :remaining-months="remainingMonths"
        />

        <p v-else-if="forecastError" class="forecast-error">예상 자산 정보를 불러오지 못했어요.</p>

        <button
          class="save-button"
          :class="{
            'save-button--disabled': !isBalanced || saving,
          }"
          type="button"
          :disabled="!isBalanced || saving"
          @click="handleSave"
        >
          {{ saving ? '저장 중…' : '이 비율로 저장하기' }}
        </button>

        <p v-if="saveError" class="save-error">
          {{ saveError }}
        </p>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { useRouter } from 'vue-router'

import PocketBudgetSlider from '@/components/pocket/PocketBudgetSlider.vue'
import BudgetForecastCard from '@/components/pocket/BudgetForecastCard.vue'

import { budgetAdjustmentApi, type BudgetAdjustmentPocketType } from '@/api/budgetAdjustment'

import { assetForecastApi } from '@/api/assetForecast'

import { ApiError } from '@/api/types'

type PocketType = 'essential' | 'free' | 'future' | 'emergency'

const router = useRouter()

const totalBudget = ref(0)

const serverMinimumTotalBudget = ref(0)

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

const forecastError = ref('')

const saveError = ref('')

const isLoading = ref(true)

const updatingTotalBudget = ref(false)

const saving = ref(false)

const currentAsset = ref(0)

const remainingMonths = ref(0)

const forecastLoaded = ref(false)

const toSafeNumber = (value: unknown): number => {
  const numberValue = Number(value)

  return Number.isFinite(numberValue) ? numberValue : 0
}

const minimumTotalBudget = computed(() => {
  const calculated = lockedAmounts.essential + lockedAmounts.free + lockedAmounts.future

  return Math.max(calculated, serverMinimumTotalBudget.value)
})

const emergencyMax = computed(() => {
  return Math.max(0, totalBudget.value - budgets.essential - budgets.free - budgets.future)
})

const allocatedTotal = computed(() => {
  return budgets.essential + budgets.free + budgets.future + budgets.emergency
})

const budgetGap = computed(() => {
  return allocatedTotal.value - totalBudget.value
})

const isBalanced = computed(() => {
  return totalBudget.value > 0 && budgetGap.value === 0
})

const expectedAsset = computed(() => {
  return currentAsset.value + budgets.future * remainingMonths.value
})

const liveStatusClass = computed(() => {
  if (budgetGap.value > 0) {
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

const clampEmergency = () => {
  budgets.emergency = Math.min(Math.max(0, budgets.emergency), emergencyMax.value)
}

const loadCurrentBudget = async () => {
  const response = await budgetAdjustmentApi.findCurrent()

  totalBudget.value = Math.max(0, toSafeNumber(response.totalBudgetAmount))

  serverMinimumTotalBudget.value = Math.max(0, toSafeNumber(response.minimumTotalBudget))

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

    const minimumAmount = Math.max(0, toSafeNumber(pocket.minimumAmount))

    budgets[type] = targetAmount

    if (type === 'emergency') {
      lockedAmounts.emergency = 0
    } else {
      lockedAmounts[type] = minimumAmount

      budgets[type] = Math.max(targetAmount, minimumAmount)
    }
  })

  clampEmergency()
}

const loadForecast = async () => {
  forecastError.value = ''

  try {
    const response = await assetForecastApi.find()

    currentAsset.value = Math.max(0, toSafeNumber(response.currentAsset))

    remainingMonths.value = Math.max(0, toSafeNumber(response.remainingMonths))

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

  /*
   * 비상금이나 현재 할당액은
   * 최소 총예산에 포함하지 않는다.
   */
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

    serverMinimumTotalBudget.value = Math.max(0, toSafeNumber(response.minimumTotalBudget))

    totalBudgetInput.value = totalBudget.value.toLocaleString('ko-KR')

    /*
     * 총예산 감소 시
     * 먼저 재배분 가능한 비상금을 줄인다.
     */
    clampEmergency()

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

const handlePocketChange = (type: PocketType, value: number) => {
  const safeValue = Math.max(0, toSafeNumber(value))

  if (type === 'emergency') {
    budgets.emergency = Math.min(emergencyMax.value, safeValue)

    return
  }

  budgets[type] = Math.max(lockedAmounts[type], safeValue)

  /*
   * 필수/자유/미래자산을 늘리면
   * 비상금에서 먼저 줄어든다.
   */
  clampEmergency()
}

const handleSave = async () => {
  if (!isBalanced.value || saving.value) {
    return
  }

  saving.value = true

  saveError.value = ''

  try {
    await budgetAdjustmentApi.adjustCurrent({
      totalBudgetAmount: totalBudget.value,

      /*
       * 중요:
       * allocations가 아니라 pockets.
       */
      pockets: [
        {
          pocketType: 'ESSENTIAL',

          amount: budgets.essential,
        },

        {
          pocketType: 'FREE',

          amount: budgets.free,
        },

        {
          pocketType: 'FUTURE_ASSET',

          amount: budgets.future,
        },

        {
          pocketType: 'EMERGENCY',

          amount: budgets.emergency,
        },
      ],

      changeReason: '사용자 포켓 예산 재조정',
    })

    await router.replace({
      name: 'pockets',
    })
  } catch (error) {
    saveError.value =
      error instanceof ApiError
        ? error.message
        : error instanceof Error
          ? error.message
          : '예산 수정에 실패했어요.'
  } finally {
    saving.value = false
  }
}

const formatCurrency = (value: unknown) => {
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

.load-error {
  margin: 20px 0;

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

.total-error,
.save-error {
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
  color: #555555;

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
}

.slider-list {
  display: flex;
  flex-direction: column;

  gap: 14px;
}

.forecast-error {
  margin-top: 4px;

  padding: 16px;

  border-radius: 12px;

  color: #777777;
  background: #f5f5f6;

  font-size: 12px;
  text-align: center;
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

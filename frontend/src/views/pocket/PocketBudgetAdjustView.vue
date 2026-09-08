<template>
  <div class="budget-page">
    <main class="budget-content">
      <section class="intro-section">
        <h2 class="intro-title">포켓 예산을 조정해 보세요</h2>

        <p class="intro-description">
          총 예산 안에서 원하는 비율로<br />
          포켓별 금액을 자유롭게 조정해 보세요.
        </p>
      </section>

      <!-- 총 예산 -->
      <section class="total-card">
        <div class="total-card__text">
          <span class="total-label"> 이번 달 총 예산 </span>

          <span class="total-description"> 이번 달 실제 사용할 예산을 직접 입력할 수 있어요. </span>
        </div>

        <div class="total-input-wrap">
          <input
            v-model="totalBudgetInput"
            class="total-input"
            type="text"
            inputmode="numeric"
            placeholder="0"
            @input="handleTotalInput"
            @keyup.enter="applyTotalBudget"
          />

          <span class="total-unit"> 원 </span>
        </div>

        <button class="total-register-button" type="button" @click="applyTotalBudget">
          총 예산 등록 완료
        </button>

        <p v-if="totalBudgetError" class="total-error">
          {{ totalBudgetError }}
        </p>
      </section>

      <!-- 포켓 조정 안내 -->
      <section class="adjust-guide">
        <strong class="adjust-guide__title"> 포켓별 예산 </strong>

        <p class="adjust-guide__description">
          각 포켓을 원하는 금액으로 조정해 주세요.<br />
          전체 합계가 총 예산과 같아야 저장할 수 있어요.
        </p>
      </section>

      <!-- 슬라이더 -->
      <section class="slider-list">
        <PocketBudgetSlider
          label="필수 포켓"
          variant="essential"
          :value="budgetStore.essentialBudget"
          :max="budgetStore.totalBudget"
          @change="handlePocketChange('essential', $event)"
        />

        <PocketBudgetSlider
          label="자유 포켓"
          variant="free"
          :value="budgetStore.freeBudget"
          :max="budgetStore.totalBudget"
          @change="handlePocketChange('free', $event)"
        />

        <PocketBudgetSlider
          label="미래자산 포켓"
          variant="future"
          :value="budgetStore.futureBudget"
          :max="budgetStore.totalBudget"
          @change="handlePocketChange('future', $event)"
        />

        <PocketBudgetSlider
          label="비상금 포켓"
          variant="emergency"
          :value="budgetStore.emergencyBudget"
          :max="budgetStore.totalBudget"
          @change="handlePocketChange('emergency', $event)"
        />
      </section>

      <!-- 배분 상태 -->
      <section class="budget-status" :class="statusClass">
        <div class="budget-status__row">
          <span class="budget-status__label"> 현재 배분 </span>

          <strong class="budget-status__amount">
            {{ formatCurrency(budgetStore.allocatedTotal) }}
          </strong>
        </div>

        <div class="budget-status__row">
          <span class="budget-status__label"> 총 예산 </span>

          <strong class="budget-status__amount">
            {{ formatCurrency(budgetStore.totalBudget) }}
          </strong>
        </div>

        <div class="budget-status__divider" />

        <div v-if="budgetStore.budgetGap > 0" class="budget-status__message">
          <strong>
            총 예산보다
            {{ formatCurrency(budgetStore.budgetGap) }}
            많아요.
          </strong>

          <span> 다른 포켓의 예산을 줄여주세요. </span>
        </div>

        <div v-else-if="budgetStore.budgetGap < 0" class="budget-status__message">
          <strong>
            아직
            {{ formatCurrency(Math.abs(budgetStore.budgetGap)) }}
            남았어요.
          </strong>

          <span> 원하는 포켓에 더 배분해 주세요. </span>
        </div>

        <div v-else class="budget-status__message budget-status__message--success">
          <strong> 총 예산에 맞게 배분됐어요. </strong>
        </div>

        <button
          v-if="!budgetStore.isBudgetBalanced && lastChangedPocket"
          class="auto-balance-button"
          type="button"
          @click="handleAutoBalance"
        >
          자동으로 맞추기
        </button>
      </section>

      <BudgetForecastCard
        :expected-asset="budgetStore.expectedAsset"
        :previous-asset="budgetStore.previousAsset"
        :difference="budgetStore.difference"
        :future-budget="budgetStore.futureBudget"
      />

      <button
        class="save-button"
        :class="{
          'save-button--disabled': !budgetStore.isBudgetBalanced,
        }"
        type="button"
        :disabled="!budgetStore.isBudgetBalanced"
        @click="handleSave"
      >
        조정 내용 저장하기
      </button>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import PocketBudgetSlider from '@/components/pocket/PocketBudgetSlider.vue'
import BudgetForecastCard from '@/components/pocket/BudgetForecastCard.vue'

import { usePocketBudgetStore, type PocketType } from '@/stores/pocketBudget'

const router = useRouter()

const budgetStore = usePocketBudgetStore()

const totalBudgetInput = ref(budgetStore.totalBudget.toLocaleString('ko-KR'))

const totalBudgetError = ref('')

const lastChangedPocket = ref<PocketType | null>(null)

const statusClass = computed(() => {
  if (budgetStore.budgetGap > 0) {
    return 'budget-status--over'
  }

  if (budgetStore.budgetGap < 0) {
    return 'budget-status--under'
  }

  return 'budget-status--balanced'
})

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

  const numberValue = Number(rawValue)

  if (!numberValue || numberValue <= 0) {
    totalBudgetError.value = '총 예산을 입력해 주세요.'

    return
  }

  const normalized = Math.round(numberValue / 50_000) * 50_000

  budgetStore.setTotalBudget(normalized)

  totalBudgetInput.value = normalized.toLocaleString('ko-KR')

  /*
   * 총예산 자체를 바꿨으므로
   * 특정 포켓을 마지막 변경 포켓으로
   * 간주하지 않는다.
   */
  lastChangedPocket.value = null
}

const handlePocketChange = (type: PocketType, value: number) => {
  budgetStore.updatePocket(type, value)

  lastChangedPocket.value = type
}

const handleAutoBalance = () => {
  if (!lastChangedPocket.value) {
    return
  }

  budgetStore.autoBalance(lastChangedPocket.value)
}

const formatCurrency = (value: number) => {
  return `${value.toLocaleString('ko-KR')}원`
}

const handleSave = () => {
  if (!budgetStore.isBudgetBalanced) {
    return
  }

  console.log('포켓 예산 조정 저장', {
    totalBudget: budgetStore.totalBudget,

    essentialBudget: budgetStore.essentialBudget,

    freeBudget: budgetStore.freeBudget,

    futureBudget: budgetStore.futureBudget,

    emergencyBudget: budgetStore.emergencyBudget,
  })

  router.back()
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

  padding: 34px 28px 40px;
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

/* =========================
   총 예산
========================= */

.total-card {
  display: flex;
  flex-direction: column;

  gap: 16px;

  width: 100%;

  padding: 22px 18px;

  border: 1px solid #e5e5e5;
  border-radius: 16px;

  background: #ffffff;
}

.total-card__text {
  display: flex;
  flex-direction: column;

  gap: 5px;
}

.total-label {
  color: #171717;

  font-size: 17px;
  font-weight: 700;
}

.total-description {
  color: #888888;

  font-size: 12px;
  line-height: 1.5;
}

.total-input-wrap {
  display: flex;
  align-items: center;

  width: 100%;

  padding: 0 16px;

  border: 1px solid #dedede;
  border-radius: 12px;

  background: #f7f7f7;
}

.total-input {
  flex: 1;

  min-width: 0;
  height: 56px;

  padding: 0;

  border: 0;
  outline: none;

  color: #111111;
  background: transparent;

  text-align: right;

  font-size: 22px;
  font-weight: 800;
}

.total-unit {
  margin-left: 5px;

  color: #111111;

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

.total-error {
  margin: -6px 0 0;

  color: #e05252;

  font-size: 12px;
  font-weight: 500;
}

/* =========================
   포켓 안내
========================= */

.adjust-guide {
  margin-top: 32px;
  margin-bottom: 16px;
}

.adjust-guide__title {
  display: block;

  color: #171717;

  font-size: 18px;
  font-weight: 700;
}

.adjust-guide__description {
  margin: 6px 0 0;

  color: #777777;

  font-size: 13px;
  line-height: 1.55;
}

/* =========================
   포켓 슬라이더
========================= */

.slider-list {
  display: flex;
  flex-direction: column;

  gap: 14px;
}

/* =========================
   배분 상태
========================= */

.budget-status {
  display: flex;
  flex-direction: column;

  gap: 12px;

  margin: 28px 0 18px;

  padding: 20px 18px;

  border: 1px solid #e5e5e5;
  border-radius: 16px;

  background: #ffffff;
}

.budget-status--over {
  border-color: #f0cccc;
  background: #fffafa;
}

.budget-status--under {
  border-color: #dedede;
}

.budget-status--balanced {
  border-color: #cce8dc;
  background: #f8fcfa;
}

.budget-status__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.budget-status__label {
  color: #777777;

  font-size: 14px;
}

.budget-status__amount {
  color: #171717;

  font-size: 17px;
  font-weight: 700;
}

.budget-status__divider {
  width: 100%;
  height: 1px;

  background: #eeeeee;
}

.budget-status__message {
  display: flex;
  flex-direction: column;

  gap: 4px;

  color: #d95050;

  font-size: 13px;
  line-height: 1.5;
}

.budget-status--under .budget-status__message {
  color: #666666;
}

.budget-status__message--success {
  color: #15936f;
}

.auto-balance-button {
  width: 100%;
  height: 48px;

  margin-top: 4px;

  border: 0;
  border-radius: 10px;

  color: #171717;
  background: #f1f1f1;

  font-size: 14px;
  font-weight: 700;

  cursor: pointer;
}

/* =========================
   저장
========================= */

.save-button {
  width: 100%;
  height: 64px;

  margin-top: 28px;

  border: none;
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

.save-button:active:not(.save-button--disabled) {
  opacity: 0.85;
}
</style>

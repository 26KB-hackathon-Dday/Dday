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
          총 예산 업데이트하기
        </button>

        <p v-if="totalBudgetError" class="total-error">
          {{ totalBudgetError }}
        </p>
      </section>

      <!-- 포켓 안내 -->
      <section class="adjust-guide">
        <strong class="adjust-guide__title"> 포켓별 예산 </strong>

        <p class="adjust-guide__description">
          슬라이더를 움직이거나 금액을 직접 입력해 주세요.<br />
          전체 합계가 총 예산과 같아야 저장할 수 있어요.
        </p>
      </section>

      <!--
        실시간 예산 상태

        진행 중 예산 조정 화면과 동일하게
        슬라이더 위에 두고 sticky 처리.
      -->
      <section class="budget-live-status" :class="liveStatusClass">
        <template v-if="budgetStore.budgetGap > 0">
          <strong class="budget-live-status__title">
            {{ formatCurrency(budgetStore.budgetGap) }}
            초과했어요
          </strong>

          <span class="budget-live-status__description">
            다른 포켓에서
            {{ formatCurrency(budgetStore.budgetGap) }}
            줄여주세요.
          </span>
        </template>

        <template v-else-if="budgetStore.budgetGap < 0">
          <strong class="budget-live-status__title">
            아직
            {{ formatCurrency(Math.abs(budgetStore.budgetGap)) }}
            남았어요
          </strong>

          <span class="budget-live-status__description"> 원하는 포켓에 더 배분해 주세요. </span>
        </template>

        <template v-else>
          <strong class="budget-live-status__title"> 예산 배분이 완료됐어요 </strong>

          <span class="budget-live-status__description"> 총 예산에 맞게 모두 배분했어요. </span>
        </template>
      </section>

      <!-- 자동 맞추기 기능은 유지 -->
      <button
        v-if="!budgetStore.isBudgetBalanced && lastChangedPocket"
        class="auto-balance-button"
        type="button"
        @click="handleAutoBalance"
      >
        자동으로 맞추기
      </button>

      <!-- 슬라이더 -->
      <section class="slider-list">
        <PocketBudgetSlider
          label="필수 포켓"
          variant="essential"
          :value="budgetStore.essentialBudget"
          :min="0"
          :max="budgetStore.totalBudget"
          :allowed-max="budgetStore.totalBudget"
          :step="500"
          @change="handlePocketChange('essential', $event)"
        />

        <PocketBudgetSlider
          label="자유 포켓"
          variant="free"
          :value="budgetStore.freeBudget"
          :min="0"
          :max="budgetStore.totalBudget"
          :allowed-max="budgetStore.totalBudget"
          :step="500"
          @change="handlePocketChange('free', $event)"
        />

        <PocketBudgetSlider
          label="미래자산 포켓"
          variant="future"
          :value="budgetStore.futureBudget"
          :min="0"
          :max="budgetStore.totalBudget"
          :allowed-max="budgetStore.totalBudget"
          :step="500"
          @change="handlePocketChange('future', $event)"
        />

        <PocketBudgetSlider
          label="비상금 포켓"
          variant="emergency"
          :value="budgetStore.emergencyBudget"
          :min="0"
          :max="budgetStore.totalBudget"
          :allowed-max="budgetStore.totalBudget"
          :step="500"
          @change="handlePocketChange('emergency', $event)"
        />
      </section>

      <!--
        기존 BudgetForecastCard
        "이 비율로 조정하면" 영역 삭제
      -->

      <button
        class="save-button"
        :class="{
          'save-button--disabled': !budgetStore.isBudgetBalanced || saving,
        }"
        type="button"
        :disabled="!budgetStore.isBudgetBalanced || saving"
        @click="handleSave"
      >
        {{ saving ? '확정 중…' : '조정 내용 저장하기' }}
      </button>

      <p v-if="saveError" class="save-error" role="alert">
        {{ saveError }}
      </p>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

import { useRouter } from 'vue-router'

import PocketBudgetSlider from '@/components/pocket/PocketBudgetSlider.vue'

import { budgetApi } from '@/api/budget'

import { ApiError } from '@/api/types'

import { usePocketBudgetStore, type PocketType } from '@/stores/pocketBudget'

const router = useRouter()

const budgetStore = usePocketBudgetStore()

const totalBudgetInput = ref(budgetStore.totalBudget.toLocaleString('ko-KR'))

const totalBudgetError = ref('')

const lastChangedPocket = ref<PocketType | null>(null)

const saving = ref(false)

const saveError = ref('')

const toSafeNumber = (value: unknown): number => {
  const numberValue = Number(value)

  return Number.isFinite(numberValue) ? numberValue : 0
}

/**
 * 진행 중 화면과 동일한
 * 실시간 상태 색상.
 */
const liveStatusClass = computed(() => {
  if (budgetStore.budgetGap > 0) {
    return 'budget-live-status--over'
  }

  if (budgetStore.budgetGap < 0) {
    return 'budget-live-status--under'
  }

  return 'budget-live-status--balanced'
})

const handleTotalInput = (event: Event) => {
  totalBudgetError.value = ''

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

  if (!Number.isFinite(numberValue) || numberValue <= 0) {
    totalBudgetError.value = '총 예산을 입력해 주세요.'

    return
  }

  const nextTotal = Math.round(numberValue)

  budgetStore.setTotalBudget(nextTotal)

  totalBudgetInput.value = nextTotal.toLocaleString('ko-KR')

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

const formatCurrency = (value: unknown): string => {
  return `${Math.round(toSafeNumber(value)).toLocaleString('ko-KR')}원`
}

const handleSave = async () => {
  if (!budgetStore.isBudgetBalanced || saving.value) {
    return
  }

  saving.value = true

  saveError.value = ''

  try {
    await budgetApi.confirmCurrent({
      totalBudgetAmount: budgetStore.totalBudget,

      pockets: [
        {
          pocketType: 'ESSENTIAL',

          amount: budgetStore.essentialBudget,
        },

        {
          pocketType: 'FREE',

          amount: budgetStore.freeBudget,
        },

        {
          pocketType: 'FUTURE_ASSET',

          amount: budgetStore.futureBudget,
        },

        {
          pocketType: 'EMERGENCY',

          amount: budgetStore.emergencyBudget,
        },
      ],
    })

    await router.replace({
      name: 'pockets',
    })
  } catch (error) {
    saveError.value = error instanceof ApiError ? error.message : '예산 확정에 실패했습니다.'
  } finally {
    saving.value = false
  }
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

  box-sizing: border-box;
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

.total-card {
  display: flex;
  flex-direction: column;

  gap: 16px;

  width: 100%;

  padding: 22px 18px;

  border: 1px solid #e5e5e5;
  border-radius: 16px;

  background: #ffffff;

  box-sizing: border-box;
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

  box-sizing: border-box;
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

.total-error,
.save-error {
  margin: 0;

  color: #e05252;

  font-size: 12px;
  line-height: 1.5;
}

.adjust-guide {
  margin-top: 32px;
  margin-bottom: 14px;
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

/*
 * 진행 중 예산 조정 화면과
 * 동일한 sticky 상태 카드.
 */
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

  border-color: #e2e2e2;

  background: rgba(250, 250, 250, 0.96);
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
  line-height: 1.45;
}

.auto-balance-button {
  width: 100%;
  height: 46px;

  margin-bottom: 14px;

  border: 0;
  border-radius: 10px;

  color: #171717;
  background: #f1f1f1;

  font-size: 14px;
  font-weight: 700;

  cursor: pointer;
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

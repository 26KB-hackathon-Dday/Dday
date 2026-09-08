<template>
  <div class="confirm-page">
    <main class="confirm-content">
      <section class="intro-section">
        <h2 class="intro-title">
          9월 계획을<br />
          이대로 바꿀까요?
        </h2>

        <p class="intro-description">
          저장하면 변경한 포켓 예산이 바로 반영돼요.<br />
          사용 중에도 다시 수정할 수 있어요.
        </p>
      </section>

      <section class="total-card">
        <span class="total-label"> 이번 달 총 예산 </span>

        <strong class="total-amount">
          {{ formatCurrency(totalBudget) }}
        </strong>
      </section>

      <section class="pocket-list">
        <div class="pocket-row pocket-row--essential">
          <span class="pocket-badge pocket-badge--essential"> 필수 포켓 </span>

          <div class="pocket-value">
            <strong>
              {{ formatCurrency(essentialBudget) }}
            </strong>

            <span class="pocket-percent pocket-percent--essential">
              {{ getPercentage(essentialBudget) }}%
            </span>
          </div>
        </div>

        <div class="pocket-row pocket-row--free">
          <span class="pocket-badge pocket-badge--free"> 자유 포켓 </span>

          <div class="pocket-value">
            <strong>
              {{ formatCurrency(freeBudget) }}
            </strong>

            <span class="pocket-percent pocket-percent--free">
              {{ getPercentage(freeBudget) }}%
            </span>
          </div>
        </div>

        <div class="pocket-row pocket-row--emergency">
          <span class="pocket-badge pocket-badge--emergency"> 비상금 포켓 </span>

          <div class="pocket-value">
            <strong>
              {{ formatCurrency(emergencyBudget) }}
            </strong>

            <span class="pocket-percent pocket-percent--emergency">
              {{ getPercentage(emergencyBudget) }}%
            </span>
          </div>
        </div>

        <div class="pocket-row pocket-row--future">
          <span class="pocket-badge pocket-badge--future"> 미래자산 포켓 </span>

          <div class="pocket-value">
            <strong>
              {{ formatCurrency(futureBudget) }}
            </strong>

            <span class="pocket-percent pocket-percent--future">
              {{ getPercentage(futureBudget) }}%
            </span>
          </div>
        </div>
      </section>

      <section class="forecast-card">
        <div class="forecast-icon">↗</div>

        <span class="forecast-label"> 예상 지원 종료 시 자산 </span>

        <strong class="forecast-amount">
          {{ formatShortCurrency(expectedAsset) }}
        </strong>
      </section>

      <section class="bottom-area">
        <div class="bottom-divider" />

        <button class="confirm-button" type="button" @click="handleConfirm">
          9월 계획 변경하기
        </button>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const getQueryNumber = (value: unknown, fallback: number): number => {
  let rawValue: unknown = value

  if (Array.isArray(rawValue)) {
    rawValue = rawValue[0]
  }

  if (typeof rawValue !== 'string' && typeof rawValue !== 'number') {
    return fallback
  }

  const parsed = Number(rawValue)

  return Number.isFinite(parsed) ? parsed : fallback
}

const totalBudget = computed(() => getQueryNumber(route.query.total, 3_000_000))

const essentialBudget = computed(() => getQueryNumber(route.query.essential, 1_000_000))

const freeBudget = computed(() => getQueryNumber(route.query.free, 700_000))

const futureBudget = computed(() => getQueryNumber(route.query.future, 500_000))

const emergencyBudget = computed(() => getQueryNumber(route.query.emergency, 800_000))

const expectedAsset = computed(() => getQueryNumber(route.query.expectedAsset, 31_200_000))

const getPercentage = (amount: number): number => {
  if (totalBudget.value <= 0) {
    return 0
  }

  return Math.round((amount / totalBudget.value) * 100)
}

const formatCurrency = (value: number): string => {
  return `${Math.round(value).toLocaleString('ko-KR')}원`
}

const formatShortCurrency = (value: number): string => {
  if (value >= 10_000) {
    const manwon = Math.round(value / 10_000)

    return `${manwon.toLocaleString('ko-KR')}만원`
  }

  return formatCurrency(value)
}

const handleConfirm = () => {
  console.log('진행 중 포켓 예산 변경 최종 확정', {
    totalBudget: totalBudget.value,

    essentialBudget: essentialBudget.value,

    freeBudget: freeBudget.value,

    futureBudget: futureBudget.value,

    emergencyBudget: emergencyBudget.value,

    expectedAsset: expectedAsset.value,
  })

  /*
   * TODO:
   * 백엔드 API 호출
   *
   * 저장 성공 후
   * 실제 포켓 진행중 화면으로 이동
   */

  router.back()
}
</script>

<style scoped>
.confirm-page {
  width: 100%;
  min-height: 100vh;

  background: #ffffff;
  color: #171717;
}

.confirm-content {
  display: flex;
  flex-direction: column;

  width: 100%;
  max-width: 430px;

  min-height: calc(100vh - 56px);

  margin: 0 auto;

  padding: 28px 28px 36px;
}

.intro-section {
  margin-bottom: 20px;
}

.intro-title {
  margin: 0;

  font-size: 27px;
  font-weight: 700;
  line-height: 1.32;

  letter-spacing: -0.8px;
}

.intro-description {
  margin: 12px 0 0;

  color: #777777;

  font-size: 12px;
  line-height: 1.6;
}

.total-card {
  display: flex;
  align-items: center;
  justify-content: space-between;

  width: 100%;
  min-height: 68px;

  padding: 0 16px;

  border: 1px solid #e4e4e4;
  border-radius: 12px;

  background: #ffffff;
}

.total-label {
  color: #555555;

  font-size: 14px;
  font-weight: 500;
}

.total-amount {
  color: #111111;

  font-size: 20px;
  font-weight: 800;

  letter-spacing: -0.5px;
}

.pocket-list {
  display: flex;
  flex-direction: column;

  gap: 12px;

  margin-top: 54px;
}

.pocket-row {
  display: flex;
  align-items: center;
  justify-content: space-between;

  min-height: 66px;

  padding: 0 16px;

  border-radius: 12px;
}

.pocket-row--essential {
  background: #f0f6ff;
}

.pocket-row--free {
  background: #fff7f1;
}

.pocket-row--future {
  background: #faf2ff;
}

.pocket-row--emergency {
  background: #ebfafa;
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

.pocket-value {
  display: flex;
  align-items: center;

  gap: 8px;
}

.pocket-value strong {
  color: #171717;

  font-size: 16px;
  font-weight: 800;
}

.pocket-percent {
  display: inline-flex;
  align-items: center;
  justify-content: center;

  min-width: 34px;

  padding: 3px 6px;

  border-radius: 999px;

  font-size: 10px;
  font-weight: 600;
}

.pocket-percent--essential {
  color: #397bc7;
  background: #dcecff;
}

.pocket-percent--free {
  color: #ef8e3d;
  background: #ffead9;
}

.pocket-percent--future {
  color: #bd6bea;
  background: #f0dcfb;
}

.pocket-percent--emergency {
  color: #22a9ad;
  background: #d1f1f1;
}

.forecast-card {
  display: flex;
  flex-direction: column;
  align-items: center;

  margin-top: 34px;

  padding: 26px 16px;

  border-radius: 12px;

  background: #f5f6ff;
}

.forecast-icon {
  margin-bottom: 8px;

  color: #111111;

  font-size: 22px;
  font-weight: 700;
}

.forecast-label {
  color: #777777;

  font-size: 12px;
}

.forecast-amount {
  margin-top: 6px;

  color: #111111;

  font-size: 25px;
  font-weight: 800;

  letter-spacing: -0.8px;
}

.bottom-area {
  margin-top: auto;
  padding-top: 70px;
}

.bottom-divider {
  width: 100%;
  height: 1px;

  margin-bottom: 20px;

  background: #eeeeee;
}

.confirm-button {
  width: 100%;
  height: 60px;

  border: 0;
  border-radius: 10px;

  color: #ffffff;
  background: #111111;

  font-size: 15px;
  font-weight: 700;

  cursor: pointer;
}

.confirm-button:active {
  opacity: 0.85;
}
</style>

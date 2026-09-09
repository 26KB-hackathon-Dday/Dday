<template>
  <section class="forecast-card">
    <h2 class="forecast-card__title">이 비율로 조정하면</h2>

    <p class="forecast-card__label">지원 종료 시 예상 총자산</p>

    <div class="forecast-card__result">
      <strong class="forecast-card__amount">
        {{ formatCurrency(safeExpectedAsset) }}
      </strong>

      <span
        class="forecast-card__difference"
        :class="{
          'forecast-card__difference--minus': difference < 0,
        }"
      >
        {{ differenceText }}
      </span>
    </div>

    <p class="forecast-card__previous">
      현재 미래자산
      {{ formatCurrency(safeCurrentAsset) }}
    </p>

    <div class="forecast-card__divider" />

    <p class="forecast-card__description">
      미래자산 포켓에 월
      {{ formatCurrency(safeFutureBudget) }}씩 {{ safeRemainingMonths }}개월 동안 꾸준히 모았을 때의
      예상이에요.
    </p>

    <p class="forecast-card__notice">투자 수익과 손실은 예상 금액에 반영하지 않았어요.</p>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  currentAsset?: number | null
  expectedAsset?: number | null
  futureBudget?: number | null
  remainingMonths?: number | null
}

const props = withDefaults(defineProps<Props>(), {
  currentAsset: 0,
  expectedAsset: 0,
  futureBudget: 0,
  remainingMonths: 0,
})

const toSafeNumber = (value: unknown) => {
  const numberValue = Number(value)

  return Number.isFinite(numberValue) ? numberValue : 0
}

const safeCurrentAsset = computed(() => {
  return toSafeNumber(props.currentAsset)
})

const safeExpectedAsset = computed(() => {
  return toSafeNumber(props.expectedAsset)
})

const safeFutureBudget = computed(() => {
  return toSafeNumber(props.futureBudget)
})

const safeRemainingMonths = computed(() => {
  return Math.max(0, Math.floor(toSafeNumber(props.remainingMonths)))
})

const difference = computed(() => {
  return safeExpectedAsset.value - safeCurrentAsset.value
})

const differenceText = computed(() => {
  const sign = difference.value >= 0 ? '+' : '-'

  return `${sign}${formatCurrency(Math.abs(difference.value))}`
})

const formatCurrency = (value: unknown) => {
  return `${Math.round(toSafeNumber(value)).toLocaleString('ko-KR')}원`
}
</script>

<style scoped>
.forecast-card {
  width: 100%;

  padding: 26px 22px 22px;
  border: 1px solid #e5e5e5;
  border-radius: 16px;
  background: #ffffff;
}

.forecast-card__title {
  margin: 0 0 28px;
  color: #171717;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;
}

.forecast-card__label {
  margin: 0 0 8px;
  color: #777777;
  font-size: 13px;
}

.forecast-card__result {
  display: flex;
  align-items: center;

  gap: 8px;
  flex-wrap: wrap;
}

.forecast-card__amount {
  color: #111111;
  font-size: 27px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -1px;
}

.forecast-card__difference {
  display: inline-flex;
  align-items: center;
  padding: 4px 7px;
  border-radius: 6px;
  color: #00a56a;
  background: #e9f9f2;
  font-size: 12px;
  font-weight: 700;
}

.forecast-card__difference--minus {
  color: #e05252;

  background: #fff0f0;
}

.forecast-card__previous {
  margin: 8px 0 0;
  color: #aaaaaa;
  font-size: 13px;
}

.forecast-card__divider {
  width: 100%;
  height: 1px;
  margin: 22px 0 14px;
  background: #eeeeee;
}

.forecast-card__description {
  margin: 0;
  color: #666666;
  font-size: 12px;
  line-height: 1.7;
}

.forecast-card__notice {
  margin: 8px 0 0;
  color: #aaaaaa;
  font-size: 10px;
  line-height: 1.5;
}
</style>

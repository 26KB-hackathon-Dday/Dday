<template>
  <section class="forecast-card">
    <h2 class="forecast-card__title">이 비율로 조정하면</h2>

    <p class="forecast-card__label">지원 종료 시 예상 총자산</p>

    <div class="forecast-card__result">
      <strong class="forecast-card__amount">
        {{ formatCurrency(expectedAsset) }}
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

    <p class="forecast-card__previous">기존 계획 {{ formatCurrency(previousAsset) }}</p>

    <div class="forecast-card__divider" />

    <p class="forecast-card__description">
      미래자산 포켓에 월
      {{ formatCurrency(futureBudget) }}씩 꾸준히 모았을 때의 예상이에요.
    </p>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  expectedAsset: number
  previousAsset: number
  difference: number
  futureBudget: number
}

const props = defineProps<Props>()

const differenceText = computed(() => {
  const sign = props.difference >= 0 ? '+' : '-'

  return `${sign}${formatCurrency(Math.abs(props.difference))}`
})

const formatCurrency = (value: number) => {
  return `${value.toLocaleString('ko-KR')}원`
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
  font-weight: 400;
}

.forecast-card__result {
  display: flex;
  align-items: center;
  gap: 8px;
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
  line-height: 1.2;
}

.forecast-card__difference--minus {
  color: #e05252;
  background: #fff0f0;
}

.forecast-card__previous {
  margin: 8px 0 0;

  color: #aaaaaa;

  font-size: 13px;
  font-weight: 400;
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
  font-weight: 400;
  line-height: 1.6;
}
</style>

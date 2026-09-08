<script setup lang="ts">
/**
 * 신용점수 반원 게이지.
 *
 * 반원 하나를 두 번 그린다 — 회색 트랙 위에 검은 진행 호를 겹치고, 진행 길이는
 * `stroke-dasharray`로 자른다. 호의 길이는 반지름 × π이므로 채움 비율만 곱하면 된다.
 *
 * 뷰가 아니라 여기 있는 이유는 이 dash 계산 때문이다. 뷰에 섞으면 화면 구조를 읽을 때마다
 * SVG 좌표를 같이 읽게 된다.
 *
 * 가운데 내용은 슬롯이다 — 점수·증감 문구는 화면이 정한다.
 */
import { computed } from 'vue'

const props = withDefaults(defineProps<{ score: number; max?: number }>(), { max: 1000 })

const RADIUS = 100
const STROKE = 26
/** 획이 잘리지 않도록 반지름 바깥에 획 절반만큼 여백을 둔다. */
const PADDING = STROKE / 2
const BASELINE_Y = RADIUS + PADDING

const WIDTH = (RADIUS + PADDING) * 2
const HEIGHT = RADIUS + PADDING

/** 왼쪽 끝에서 오른쪽 끝까지, 위로 볼록한 반원. */
const ARC = `M ${PADDING} ${BASELINE_Y} A ${RADIUS} ${RADIUS} 0 0 1 ${WIDTH - PADDING} ${BASELINE_Y}`
const ARC_LENGTH = Math.PI * RADIUS

/** 0~1로 자른다. 점수가 범위를 벗어나도 호가 넘치거나 뒤로 감기지 않는다. */
const ratio = computed(() => Math.min(Math.max(props.score / props.max, 0), 1))
const dashOffset = computed(() => ARC_LENGTH * (1 - ratio.value))
</script>

<template>
  <div class="gauge">
    <svg class="gauge__svg" :viewBox="`0 0 ${WIDTH} ${HEIGHT}`" role="img"
         :aria-label="`${max}점 만점에 ${score}점`">
      <path :d="ARC" fill="none" stroke="var(--c-border)" :stroke-width="STROKE" />
      <path
        :d="ARC"
        fill="none"
        stroke="#171717"
        :stroke-width="STROKE"
        :stroke-dasharray="ARC_LENGTH"
        :stroke-dashoffset="dashOffset"
      />
    </svg>

    <!-- 호 안쪽 아래에 붙인다. 반원이라 세로 가운데가 아니라 아래쪽이 시각적 중심이다. -->
    <div class="gauge__center">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.gauge {
  position: relative;
  width: 100%;
}

.gauge__svg {
  display: block;
  width: 100%;
  height: auto;
}

.gauge__center {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
</style>

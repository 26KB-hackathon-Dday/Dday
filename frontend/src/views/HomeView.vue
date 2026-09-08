<script setup lang="ts">
/**
 * 로그인 후 홈. 보호종료일 기준 자립 D-day와 지원 종료 시 예상 자산을 보여준다.
 *
 * D-day 계산식(지원기간 5년)은 백엔드 `OnboardingCalculator`와 같다 — 온보딩 저장 응답에도
 * 같은 값이 실려오지만, 그 스토어는 온보딩 플로우용이라 홈에서는 `/api/users/me`로 새로 받는다.
 */
import { computed, onMounted, ref } from 'vue'
import { userApi, type Me } from '@/api/user'
import AppIcon from '@/components/AppIcon.vue'

/** 자립 지원 기간(년). 백엔드 OnboardingCalculator.SUPPORT_YEARS와 맞춘다. */
const SUPPORT_YEARS = 5

const me = ref<Me | null>(null)

onMounted(async () => {
  me.value = await userApi.fetchMe()
})

/** 오늘 00:00 (로컬). */
function startOfToday(): Date {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  return d
}

function monthsBetween(from: Date, to: Date): number {
  let months = (to.getFullYear() - from.getFullYear()) * 12 + (to.getMonth() - from.getMonth())
  if (to.getDate() < from.getDate()) months -= 1
  return months
}

const plan = computed(() => {
  const ped = me.value?.protectionEndDate
  if (!ped) return null

  const start = new Date(`${ped}T00:00:00`)
  const end = new Date(start)
  end.setFullYear(end.getFullYear() + SUPPORT_YEARS)
  const today = startOfToday()

  const dayMs = 86_400_000
  const daysLeft = Math.round((end.getTime() - today.getTime()) / dayMs)
  const totalDays = (end.getTime() - start.getTime()) / dayMs
  const elapsedDays = (today.getTime() - start.getTime()) / dayMs
  const progress = Math.min(100, Math.max(0, Math.round((elapsedDays / totalDays) * 100)))

  return {
    monthsSince: Math.max(0, monthsBetween(start, today)),
    monthsLeft: Math.max(0, monthsBetween(today, end)),
    daysLeft,
    progress,
    endLabel: `${end.getFullYear()}년 ${String(end.getMonth() + 1).padStart(2, '0')}월 ${String(
      end.getDate(),
    ).padStart(2, '0')}일`,
  }
})

const ddayText = computed(() => {
  const d = plan.value?.daysLeft
  if (d == null) return 'D-day'
  return d >= 0 ? `D-${d}` : `D+${Math.abs(d)}`
})

// TODO: "지원 종료 시 예상 자산"은 자산·저축·투자 흐름을 반영한 추정 API가 필요하다.
//       그 엔드포인트가 생기기 전까지는 온보딩 시점 보유 자산을 그대로 보여준다.
const assetText = computed(() => {
  const a = me.value?.initialAsset
  return a == null ? '—' : `${a.toLocaleString('ko-KR')}원`
})
</script>

<template>
  <div v-if="me" class="page">
    <section class="hero">
      <p class="hero__phase">자립 {{ plan?.monthsSince ?? 0 }}개월 차</p>
      <p class="hero__dday">{{ ddayText }}</p>

      <p class="hero__note">지원 종료까지 {{ plan?.monthsLeft ?? 0 }}개월 남았어요</p>
      <div class="hero__progress">
        <span class="bar"><span class="bar__fill" :style="{ width: `${plan?.progress ?? 0}%` }" /></span>
        <span class="hero__pct">{{ plan?.progress ?? 0 }}%</span>
      </div>
      <p class="hero__end">{{ plan?.endLabel ?? '' }}</p>
    </section>

    <section class="proj">
      <h2 class="proj__title">지원 종료 시 예상 자산</h2>
      <div class="proj__card">
        <p class="proj__amount">{{ assetText }}</p>
        <p class="proj__desc">현재 자산과 저축, 투자 흐름을 기준으로 예상했어요.</p>
        <div class="proj__link">
          <span>예상 자산 자세히 보러가기</span>
          <AppIcon name="arrow-right" :size="10" />
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 32px;
  padding: 24px 20px 32px;
}

/* ── D-day 히어로 ── */
.hero {
  display: flex;
  flex-direction: column;
  padding: 0 16px;
}
.hero__phase {
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  color: #000;
}
.hero__dday {
  margin-top: 4px;
  font-family: var(--font-num);
  font-weight: 700;
  font-size: 45px;
  line-height: 44px;
  letter-spacing: -1.08px;
  color: #000;
}
.hero__note {
  margin-top: 24px;
  text-align: center;
  font-size: 13px;
  font-weight: 300;
  line-height: 20px;
  color: #000;
}
.hero__progress {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.bar {
  flex: 1;
  height: 8px;
  border-radius: 9999px;
  background: #d4cfcf;
  overflow: hidden;
}
.bar__fill {
  display: block;
  height: 100%;
  border-radius: 9999px;
  background: #727272;
}
.hero__pct {
  font-family: var(--font-num);
  font-weight: 700;
  font-size: 11px;
  letter-spacing: 0.22px;
  color: #000;
}
.hero__end {
  margin-top: 8px;
  align-self: flex-end;
  font-size: 11px;
  font-weight: 500;
  line-height: 14px;
  color: var(--c-text-2);
}

/* ── 예상 자산 ── */
.proj {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.proj__title {
  font-size: 16px;
  font-weight: 500;
  line-height: 28px;
  letter-spacing: -0.2px;
  color: #171717;
}
.proj__card {
  display: flex;
  flex-direction: column;
  padding: 24px 25px;
  border: 1px solid #848484;
  border-radius: 16px;
  background: #000;
}
.proj__amount {
  font-family: var(--font-num);
  font-weight: 700;
  font-size: 24px;
  line-height: 32px;
  letter-spacing: -0.48px;
  color: #fff;
}
.proj__desc {
  margin-top: 8px;
  font-size: 11px;
  font-weight: 300;
  line-height: 14px;
  letter-spacing: 0.22px;
  color: #fff;
}
.proj__link {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 24px;
  padding-top: 17px;
  border-top: 1px solid var(--c-border);
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  color: #fff;
}
</style>

<script setup lang="ts">
/**
 * 로그인 후 홈. 보호종료일 기준 자립 D-day와 지원 종료 시 예상 자산을 보여준다.
 *
 * D-day 계산식(지원기간 5년)은 백엔드 `OnboardingCalculator`와 같다 — 온보딩 저장 응답에도
 * 같은 값이 실려오지만, 그 스토어는 온보딩 플로우용이라 홈에서는 `/api/users/me`로 새로 받는다.
 *
 * 보호종료일(`users.protection_end_date`)이 아직 없으면(온보딩 전) D-day만 지원기간
 * 전체(1825일)로 고정해서 보여주고, 진행률 바·경과 개월수 등 나머지는 숨긴다.
 */
import { computed, onMounted, ref } from 'vue'
import { userApi, type Me } from '@/api/user'
import AppIcon from '@/components/AppIcon.vue'
import HomeActionCards from '@/components/home/HomeActionCards.vue'

/** 자립 지원 기간(년). 백엔드 OnboardingCalculator.SUPPORT_YEARS와 맞춘다. */
const SUPPORT_YEARS = 5

/** 보호종료일 미입력 시 쓰는 남은 일수. 지원기간 5년(365×5)을 일 단위 고정값으로 둔다. */
const FALLBACK_DAYS_LEFT = 1825

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

/** `2030년 03월 01일` 꼴. */
function formatDate(d: Date): string {
  return `${d.getFullYear()}년 ${String(d.getMonth() + 1).padStart(2, '0')}월 ${String(
    d.getDate(),
  ).padStart(2, '0')}일`
}

const DAY_MS = 86_400_000

/**
 * 보호종료일이 있을 때만 계산되는 자립 진행 정보.
 *
 * 보호종료일이 없으면(온보딩에서 "모르겠다"로 넘긴 경우) `null`이다 — 시작일을 몰라
 * 진행률·경과 개월수를 낼 수 없어, 화면은 D-day(고정 1825일)만 보여준다.
 */
const plan = computed(() => {
  const ped = me.value?.protectionEndDate
  if (!ped) return null

  const start = new Date(`${ped}T00:00:00`)
  const end = new Date(start)
  end.setFullYear(end.getFullYear() + SUPPORT_YEARS)
  const today = startOfToday()

  const daysLeft = Math.round((end.getTime() - today.getTime()) / DAY_MS)
  const totalDays = (end.getTime() - start.getTime()) / DAY_MS
  const elapsedDays = (today.getTime() - start.getTime()) / DAY_MS
  const progress = Math.min(100, Math.max(0, Math.round((elapsedDays / totalDays) * 100)))

  return {
    monthsSince: Math.max(0, monthsBetween(start, today)),
    monthsLeft: Math.max(0, monthsBetween(today, end)),
    daysLeft,
    progress,
    endLabel: formatDate(end),
  }
})

const ddayText = computed(() => {
  const d = plan.value?.daysLeft ?? FALLBACK_DAYS_LEFT
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
      <p v-if="plan" class="hero__phase">자립 {{ plan.monthsSince }}개월 차</p>
      <p class="hero__dday">{{ ddayText }}</p>

      <template v-if="plan">
        <p class="hero__note">
          지원 종료까지 <strong>{{ plan.monthsLeft }}</strong
          >개월 남았어요
        </p>
        <div class="hero__progress">
          <div class="bar">
            <div class="bar__fill" :style="{ width: `${plan.progress}%` }" />
            <span class="bar__pct">{{ plan.progress }}%</span>
          </div>
        </div>
        <p class="hero__end">{{ plan.endLabel }}</p>
      </template>
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

    <HomeActionCards />
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
  font-size: 13px;
  font-weight: 300;
  line-height: 20px;
  color: #000;
}
.hero__note strong {
  font-weight: 700;
}
.hero__progress {
  margin-top: 16px;
}
/* 알약형 트랙 — 검정 테두리 안에서 검정 막대가 차고, 오른쪽에 % 를 얹는다. */
.bar {
  position: relative;
  isolation: isolate;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 40px;
  padding-right: 18px;
  border: 1.5px solid #000;
  border-radius: 9999px;
  background: var(--c-bg);
  overflow: hidden;
}
.bar__fill {
  position: absolute;
  left: 4px;
  top: 4px;
  bottom: 4px;
  min-width: 32px;
  max-width: calc(100% - 8px);
  border-radius: 9999px;
  background: #000;
  transition: width 0.4s ease;
}
.bar__pct {
  position: relative;
  font-family: var(--font-num);
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 0.26px;
  /* 빈 트랙(흰색) 위에선 검정, 채워진 막대(검정) 위에선 흰색으로 자동 반전. */
  color: #fff;
  mix-blend-mode: difference;
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
  font-size: 13px;
  font-weight: 400;
  line-height: 18px;
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

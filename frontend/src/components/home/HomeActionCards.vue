<script setup lang="ts">
/**
 * 홈 "오늘 확인할 것" — 지금 사용자가 놓치고 있을 만한 것 3종을 카드로 띄운다.
 *
 * - 지원금: 자격이 되는데 아직 안 받는 지원제도가 있는가
 * - 포켓:   자유 포켓이 이번 달 예산보다 빠르게 소진 중인가
 * - 신용:   신용평가에 제출할 수 있는 비금융 납부 기록이 있는가
 *
 * 세 API를 각각 독립적으로 조회하고(하나 실패해도 나머지는 뜬다), 조건에 맞는
 * 카드만 담는다. 담긴 게 없으면 섹션 자체를 그리지 않는다.
 */
import { onMounted, ref } from 'vue'
import { grantApi } from '@/api/grant'
import { pocketApi } from '@/api/pocket'
import { creditApi } from '@/api/credit'
import { formatWon } from '@/utils/format'
import AppIcon from '@/components/AppIcon.vue'

interface ActionCard {
  key: string
  label: string
  title: string
  lines: string[]
  linkText: string
  to: string
}

const cards = ref<ActionCard[]>([])

/** 이번 달 `YYYY-MM`. 포켓 API가 받는 형식. */
function currentMonth(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

onMounted(async () => {
  const month = currentMonth()
  const [grant, pockets, payments] = await Promise.allSettled([
    grantApi.fetchHome(),
    pocketApi.findMonthly(month),
    creditApi.fetchPaymentHistory(),
  ])

  const next: ActionCard[] = []

  // 1. 지원금 — 자격은 되는데 아직 안 받는 제도 중 첫 번째.
  const topGrant = grant.status === 'fulfilled' ? grant.value.missing[0] : undefined
  if (topGrant) {
    const top = topGrant
    next.push({
      key: 'grant',
      label: '지원금',
      title: '받을 수 있는 지원금이 있어요',
      lines: [top.title, top.amountText],
      linkText: '신청 방법 확인하러 가기',
      to: `/grants/${top.id}`,
    })
  }

  // 2. 포켓 — 자유 포켓이 이미 예산을 넘었거나, 소진 속도상 넘길 페이스일 때.
  if (pockets.status === 'fulfilled') {
    const free = pockets.value.pockets.find((p) => p.pocketType === 'FREE')
    if (free?.usageRate != null && free.usedAmount != null && free.targetAmount > 0) {
      const now = new Date()
      const dayOfMonth = now.getDate()
      const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
      const linearPace = (dayOfMonth / daysInMonth) * 100
      const alreadyOver = (free.overAmount ?? 0) > 0

      // 이미 초과 → 넘긴 금액을 그대로. 아직이면 → 현재 속도로 월말 예상 초과액.
      const projectedOver =
        (free.usedAmount / dayOfMonth) * daysInMonth - free.targetAmount

      let lines: string[] | null = null
      if (alreadyOver) {
        lines = [
          `이번 달 자유 포켓 예산 ${formatWon(free.targetAmount)}을`,
          `${formatWon(free.overAmount as number)} 넘겼어요.`,
        ]
      } else if (free.usageRate >= linearPace + 10 && projectedOver >= 10_000) {
        // 선형 페이스보다 10%p 넘게 앞서고, 이대로면 1만원 이상 초과할 때만.
        const over = Math.round(projectedOver / 1000) * 1000
        lines = [
          `이번 달 예산의 ${Math.round(free.usageRate)}%를 사용했어요.`,
          `현재 속도라면 약 ${formatWon(over)} 초과할 수 있어요.`,
        ]
      }

      if (lines) {
        next.push({
          key: 'pocket',
          label: '포켓',
          title: '자유 포켓 예산이 빠르게 줄고 있어요',
          lines,
          linkText: '자유 포켓 확인하러 가기',
          to: `/pockets/FREE?month=${month}`,
        })
      }
    }
  }

  // 3. 신용관리 — 신용평가에 낼 수 있는 비금융 납부 이력이 잡혀 있을 때.
  if (payments.status === 'fulfilled' && payments.value.types.length > 0) {
    next.push({
      key: 'credit',
      label: '신용관리',
      title: '제출 가능한 금융 기록이 있어요',
      lines: ['신용평가에 활용할 수 있는 금융 기록을 확인해 보세요.'],
      linkText: '확인하러 가기',
      to: '/credit-manage/payments',
    })
  }

  cards.value = next
})
</script>

<template>
  <section v-if="cards.length" class="alerts">
    <h2 class="alerts__title">오늘 확인할 것</h2>
    <div class="alerts__list">
      <RouterLink v-for="card in cards" :key="card.key" :to="card.to" class="card">
        <span class="card__label">{{ card.label }}</span>
        <p class="card__heading">{{ card.title }}</p>
        <p class="card__body">
          <span v-for="(line, i) in card.lines" :key="i">{{ line }}</span>
        </p>
        <span class="card__link">
          {{ card.linkText }}
          <span class="card__arrow"><AppIcon name="arrow-right" :size="10" /></span>
        </span>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
/*
 * 뉴모피즘 느낌만: 배경 판/색 없이 카드가 흰 페이지 위에서 그림자로만 떠 보인다.
 * 흰 배경이라 밝은 쪽 그림자는 안 보이지만, 어두운 쪽(방향성) 그림자가 입체감을 만든다.
 */
.alerts {
  --nm-shadow: rgba(163, 177, 198, 0.5);
  --nm-light: rgba(255, 255, 255, 0.9);

  display: flex;
  flex-direction: column;
  gap: 12px;
}
.alerts__title {
  font-size: 16px;
  font-weight: 500;
  line-height: 28px;
  letter-spacing: -0.2px;
  color: #171717;
}
.alerts__list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.card {
  display: flex;
  flex-direction: column;
  padding: 18px 16px;
  border-radius: 18px;
  background: var(--c-bg);
  box-shadow: 7px 7px 18px var(--nm-shadow), -7px -7px 18px var(--nm-light);
  transition:
    box-shadow 0.15s ease,
    transform 0.15s ease;
}
.card:active {
  transform: scale(0.99);
  box-shadow:
    inset 5px 5px 10px var(--nm-shadow),
    inset -5px -5px 10px var(--nm-light);
}

.card__label {
  align-self: flex-start;
  padding: 4px 11px;
  border-radius: 999px;
  box-shadow:
    inset 2px 2px 4px var(--nm-shadow),
    inset -1px -1px 2px var(--nm-light);
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
  letter-spacing: 0.4px;
  color: #7a7f88;
}
.card__heading {
  margin-top: 14px;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
  color: #2b2f36;
}
.card__body {
  margin-top: 6px;
  font-size: 12px;
  line-height: 20px;
  color: #6b7079;
}
.card__body span {
  display: block;
}
.card__link {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
  font-size: 13px;
  font-weight: 500;
  line-height: 20px;
  color: #4a4f57;
}
.card__arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--c-bg);
  box-shadow: 2px 2px 5px var(--nm-shadow), -2px -2px 5px var(--nm-light);
  color: #6b7079;
}
</style>

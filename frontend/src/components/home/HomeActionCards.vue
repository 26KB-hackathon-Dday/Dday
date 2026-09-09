<script setup lang="ts">
/**
 * 홈 "오늘 확인할 것" — 현재는 **목업**. 모든 사용자에게 같은 3장을 보여준다.
 *
 * TODO: 사용자별 실데이터로 교체 (지원금 자격 / 자유 포켓 소진 / 제출 가능한 신용 기록).
 *       이전 데이터 연동 구현은 git 이력 참고 — grantApi·pocketApi·creditApi를
 *       Promise.allSettled로 조회해 조건 맞는 카드만 담았다.
 */
import AppIcon from '@/components/AppIcon.vue'

interface ActionCard {
  key: string
  label: string
  title: string
  lines: string[]
  linkText: string
  to: string
}

const cards: ActionCard[] = [
  {
    key: 'grant',
    label: '지원금',
    title: '받을 수 있는 지원금이 있어요',
    lines: ['청년 주거지원', '월 200,000원'],
    linkText: '신청 방법 확인하러 가기',
    to: '/grants',
  },
  {
    key: 'pocket',
    label: '포켓',
    title: '자유 포켓 예산이 빠르게 줄고 있어요',
    lines: ['이번 달 예산의 82%를 사용했어요.', '현재 속도라면 약 48,000원 초과할 수 있어요.'],
    linkText: '자유 포켓 확인하러 가기',
    to: '/pockets/FREE',
  },
  {
    key: 'credit',
    label: '신용관리',
    title: '제출 가능한 금융 기록이 있어요',
    lines: ['신용평가에 활용할 수 있는 금융 기록을 확인해 보세요.'],
    linkText: '확인하러 가기',
    to: '/credit-manage/payments',
  },
]
</script>

<template>
  <section class="alerts">
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

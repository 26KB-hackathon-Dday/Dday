<script setup lang="ts">
/**
 * 신용관리 하기 — 신용점수에 영향을 주는 요소를 훑어보는 목록.
 *
 * 문구가 화면에 박혀 있는 이유는 사용자 데이터가 아니라 고정 안내이기 때문이다.
 * (AGENTS.md의 "표시용 값은 서버 것을 쓴다"는 `BANK → 은행`처럼 서버 값에서 파생되는
 * 라벨에 대한 규칙이다.) 항목마다 실제 내 데이터를 보여주는 상세 화면이 붙어 있다.
 */

interface Topic {
  icon: 'bars' | 'alert' | 'card'
  title: string
  description: string
  /** 눌렀을 때 열리는 상세 화면. */
  to: string
}

const TOPICS: Topic[] = [
  {
    icon: 'bars',
    title: '매달 내는 요금도 점수가 될 수 있어요.',
    description: '통신요금·건강보험료·국민연금 납부 이력 확인',
    to: '/credit-manage/payments',
  },
  {
    icon: 'alert',
    title: '한도를 많이 쓰면 점수가 내려가요.',
    description: '카드 한도 대비 사용 비율 확인',
    to: '/credit-manage/card-usage',
  },
  {
    icon: 'card',
    title: '어디서 빌렸는지가 점수를 나눠요.',
    description: '대출 건수와 금융권 종류 확인',
    to: '/credit-manage/loans',
  },
]

/**
 * 원형 배지 안에 그릴 글리프. 대응하는 아이콘 에셋이 없어 직접 그린다
 * (`credit.svg` 등은 하단 탭용 손그림이라 비율이 맞지 않는다).
 */
const ICON_PATHS: Record<Topic['icon'], string[]> = {
  bars: ['M8 16.5V12', 'M12 16.5V7.5', 'M16 16.5v-3'],
  alert: ['M12 4.5a7.5 7.5 0 1 0 0 15 7.5 7.5 0 0 0 0-15', 'M12 8v5', 'M12 15.9v.1'],
  card: ['M4.5 7.5h15v9h-15z', 'M4.5 11h15', 'M15.5 14h2'],
}
</script>

<template>
  <div class="page">
    <header class="intro">
      <h2 class="intro__title">내 신용, 지금 괜찮은가요?</h2>
      <p class="intro__desc">연결된 데이터에서 점수에 영향을 줄 수 있는 부분을 확인해드려요.</p>
    </header>

    <ul class="topics">
      <li v-for="topic in TOPICS" :key="topic.title">
        <RouterLink :to="topic.to" class="topic">
          <span class="topic__badge" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.8"
                 stroke-linecap="round" stroke-linejoin="round">
              <path v-for="d in ICON_PATHS[topic.icon]" :key="d" :d="d" />
            </svg>
          </span>

          <div class="topic__body">
            <p class="topic__title">{{ topic.title }}</p>
            <p class="topic__desc">{{ topic.description }}</p>
          </div>

          <span class="topic__chevron" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                 stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 5l7 7-7 7" />
            </svg>
          </span>
        </RouterLink>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.page {
  padding: 24px 20px 32px;
}

.intro {
  margin-bottom: 32px;
}

.intro__title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.3px;
}

.intro__desc {
  margin-top: 10px;
  font-size: 15px;
  line-height: 1.55;
  color: var(--c-text-2);
}

.topics {
  display: flex;
  flex-direction: column;
  gap: 12px;
  list-style: none;
}

.topic {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: var(--c-bg);
  border-radius: 16px;
}

.topic__badge {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 9999px;
  background: #171717;
}

.topic__badge svg {
  width: 26px;
  height: 26px;
}

.topic__body {
  flex: 1;
  min-width: 0;
}

.topic__title {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.2px;
}

.topic__desc {
  margin-top: 4px;
  font-size: 13px;
  color: var(--c-text-3);
}

.topic__chevron {
  flex-shrink: 0;
  display: flex;
  color: var(--c-text-3);
}

.topic__chevron svg {
  width: 16px;
  height: 16px;
}
</style>

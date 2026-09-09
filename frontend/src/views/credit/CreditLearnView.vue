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

/**
 * 세 항목이 보여주는 건 납부 이력·카드 이용률·대출 구성이고, 근거는 연결된
 * 마이데이터다. 그 한계를 밝히지 않으면 사용자가 여기 숫자를 심사 결과로 오해한다.
 *
 * 순서는 데이터 범위 → 반영 조건 → 점수의 의미다. 무엇을 보고 있는지, 그게 저절로
 * 점수가 되지는 않는다는 것, 그 점수도 절대적이지 않다는 것 순으로 읽힌다.
 *
 * 둘째 줄은 첫 항목("매달 내는 요금도 점수가 될 수 있어요")의 "될 수 있어요"가 무슨
 * 조건인지 설명한다. 비금융 정보는 신용평가사에 따로 제출해야 가점이 붙는데, 화면
 * 어디에도 그 말이 없어서 납부 기록만 쌓이면 점수가 오르는 것처럼 읽힌다.
 *
 * ⚠️ 신용조회 관련 문구를 넣지 않는다. 이 화면도 하위 세 화면도 신용점수를 조회하지
 *    않는다 (`/api/credit/payments`, `/card-usage`, `/loans`만 부른다).
 *    점수 조회는 신용관리 홈과 예상 금리 화면의 `scores/recent`다.
 */
const NOTES: string[] = [
  '연결된 마이데이터 기준이라, 연결하지 않은 기관의 거래는 빠져 있을 수 있어요.',
  '통신요금·건강보험료 같은 비금융 정보는 신용평가사에 따로 제출해야 점수에 반영돼요.',
  '신용점수는 KCB 기준이에요. 금융사는 저마다의 기준으로 다시 평가하기 때문에 실제 심사 결과와 다를 수 있어요.',
]
</script>

<template>
  <div class="page">
    <header class="intro">
      <h2 class="intro__title">내 신용점수, 지금 괜찮은가요?</h2>
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

    <section class="notes">
      <h3 class="notes__title">유의사항</h3>
      <ul class="notes__list">
        <li v-for="note in NOTES" :key="note">{{ note }}</li>
      </ul>
    </section>
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

/* ── 유의사항 ────────────────────────────────────────────────────── */

/* 화면 맨 아래에 고정하지 않고 목록 바로 뒤에 잇는다 — 하단에 붙이면 목록과의 사이가
   빈 채로 남아 오히려 끊겨 보인다. 회색 판은 CardUsageView·LoanStatusView의 `.tip`과
   같은 --c-surface 관용구다. 글자는 물러나 있어야 하므로 배경만 주고 색은 올리지 않는다. */
.notes {
  margin-top: 32px;
  padding: 16px;
  border-radius: 16px;
  background: var(--c-surface);
}

.notes__title {
  font-size: 13px;
  font-weight: 700;
  color: var(--c-text-2);
}

.notes__list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 10px;
  list-style: none;
}

/* 가운뎃점을 바깥으로 빼서 줄이 넘어가도 본문 왼쪽이 맞는다. */
.notes__list li {
  position: relative;
  padding-left: 10px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--c-text-3);
}

.notes__list li::before {
  content: '·';
  position: absolute;
  left: 0;
}
</style>

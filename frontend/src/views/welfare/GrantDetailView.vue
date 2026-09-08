<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { grantApi, type GrantDetail } from '@/api/grant'
import AppIcon from '@/components/AppIcon.vue'

const route = useRoute()
const router = useRouter()

const detail = ref<GrantDetail | null>(null)
const loading = ref(true)

async function load(id: string) {
  loading.value = true
  detail.value = await grantApi.fetchDetail(id)
  loading.value = false
}

onMounted(() => load(String(route.params.id)))
watch(
  () => route.params.id,
  (id) => load(String(id)),
)

function goBack() {
  if (window.history.state?.back != null) router.back()
  else router.push('/grants')
}

function won(n: number): string {
  return `${Math.round(n).toLocaleString('ko-KR')}원`
}

/** 월 지원액 × 개월 수 — 제도 사실만으로 계산되는 예상 총액. 개월 수 없으면 null. */
const incomeTotal = computed(() => {
  const ic = detail.value?.incomeChange
  if (!ic || !ic.durationMonths) return null
  return ic.monthlyAmount * ic.durationMonths
})

const channel = computed(() => detail.value?.applicationChannel ?? null)
const hasChannel = computed(
  () => !!(channel.value && (channel.value.name || channel.value.url || channel.value.phone)),
)

function openChannel() {
  const url = channel.value?.url
  if (url) window.open(url, '_blank', 'noopener')
}
</script>

<template>
  <div class="detail">
    <header class="detail__header">
      <button type="button" class="detail__back" aria-label="뒤로" @click="goBack">
        <AppIcon name="back" :size="16" />
      </button>
      <h1 class="detail__heading">지원 상세</h1>
    </header>

    <p v-if="loading" class="detail__state">불러오는 중…</p>

    <div v-else-if="!detail" class="detail__state">
      <p>지원 정보를 찾을 수 없습니다.</p>
      <button type="button" class="detail__linkbtn" @click="goBack">돌아가기</button>
    </div>

    <template v-else>
      <div class="body">
        <!-- 히어로 -->
        <section class="hero">
          <span v-if="detail.category" class="hero__cat">{{ detail.category }}</span>
          <h2 class="hero__title">{{ detail.name }}</h2>
          <p v-if="detail.agency" class="hero__agency">{{ detail.agency }}</p>
        </section>

        <!-- 핵심 정보 -->
        <section class="info">
          <div v-if="detail.description" class="card desc">
            <p class="card__label">
              <AppIcon name="info" :size="15" />
              제도 설명
            </p>
            <p class="desc__text">{{ detail.description }}</p>
          </div>

          <div v-if="detail.benefitText" class="card benefit">
            <p class="card__label">
              <AppIcon name="benefit" :size="15" />
              지원 내용
            </p>
            <p class="benefit__amount">{{ detail.benefitText }}</p>
            <p v-if="detail.benefitNote" class="benefit__note">{{ detail.benefitNote }}</p>
          </div>

          <!-- 예상 수입 변화 — 금액 지원형·월별일 때만 백엔드가 채워준다 -->
          <div v-if="detail.incomeChange" class="card income">
            <h3 class="income__title">{{ detail.incomeChange.title }}</h3>
            <p class="income__monthly">
              매월 <strong>+{{ won(detail.incomeChange.monthlyAmount) }}</strong>
            </p>
            <p v-if="incomeTotal" class="income__total">
              {{ detail.incomeChange.durationMonths }}개월 동안 총 {{ won(incomeTotal) }}
            </p>
          </div>

          <div v-if="detail.targetDescription" class="card factrow">
            <span class="factrow__icon"><AppIcon name="target-person" :size="16" /></span>
            <div class="factrow__text">
              <span class="card__label">지원 대상</span>
              <span class="factrow__value">{{ detail.targetDescription }}</span>
            </div>
          </div>
        </section>

        <!-- 필요 서류 (백엔드 구조화 전 — 값 있을 때만) -->
        <section v-if="detail.requiredDocuments.length" class="details">
          <div class="details__block">
            <h3 class="minihead">
              <AppIcon name="doc" :size="14" />
              필요 서류
            </h3>
            <ul class="doclist">
              <li v-for="doc in detail.requiredDocuments" :key="doc">{{ doc }}</li>
            </ul>
          </div>
        </section>

        <!-- 신청 방법 -->
        <section v-if="hasChannel" class="details">
          <div class="details__block">
            <h3 class="minihead">
              <AppIcon name="method" :size="15" />
              신청 방법
            </h3>
            <div class="method">
              <span class="method__left">
                <span class="method__icon"><AppIcon name="globe" :size="15" /></span>
                {{ channel?.name || '문의처' }}
              </span>
              <span v-if="channel?.phone" class="method__channel">{{ channel.phone }}</span>
            </div>
          </div>
        </section>
      </div>

      <!-- 하단 고정 액션 -->
      <div class="actionbar">
        <button type="button" class="actionbar__save" aria-label="저장">
          <AppIcon name="detail-bookmark" :size="20" />
        </button>
        <button
          type="button"
          class="actionbar__primary"
          :disabled="!channel?.url"
          @click="openChannel"
        >
          신청 페이지 열기
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  background: #f9f9f9;
}

/* ── 헤더 ── */
.detail__header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 24px;
  background: var(--c-bg);
  border-bottom: 1px solid var(--c-border);
}
.detail__back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  margin-left: -8px;
  color: var(--c-text-2);
}
.detail__heading {
  flex: 1;
  text-align: center;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: -0.2px;
  color: #000;
  /* 좌측 back 버튼(32px 유효폭)만큼 우측을 보정해 시각적 중앙을 맞춘다. */
  margin-right: 32px;
}
.detail__state {
  padding: 48px 24px;
  text-align: center;
  color: var(--c-text-3);
}
.detail__linkbtn {
  margin-top: 12px;
  padding: 8px 16px;
  border: 1px solid var(--c-border);
  border-radius: 8px;
}

/* ── 본문 ── */
.body {
  display: flex;
  flex-direction: column;
  gap: 40px;
  padding: 40px 24px;
  background: var(--c-bg);
}

.hero {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.hero__cat {
  align-self: flex-start;
  padding: 4px 12px;
  border-radius: 9999px;
  background: var(--c-surface);
  color: var(--c-text-2);
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
}
.hero__title {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 500;
  line-height: 32px;
  letter-spacing: -0.52px;
  color: #000;
}
.hero__agency {
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  color: var(--c-text-2);
}

/* ── 카드 공통 ── */
.card {
  border: 1px solid var(--c-border);
  border-radius: 12px;
  background: var(--c-bg);
}
.card__label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  color: var(--c-text-2);
}

.info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.benefit {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 25px;
}
.benefit__amount {
  margin-top: 4px;
  font-family: var(--font-num);
  font-weight: 600;
  font-size: 20px;
  line-height: 28px;
  letter-spacing: -0.2px;
  color: #000;
}
.benefit__note {
  margin-top: 9px;
  padding-top: 17px;
  border-top: 1px solid var(--c-border);
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  color: var(--c-text-2);
}

/* ── 예상 수입 변화 카드 ── */
.card.income {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 21px;
  background: #f5f6f8;
}
.income__title {
  font-size: 14px;
  font-weight: 500;
  line-height: 18px;
  letter-spacing: 0.14px;
  color: var(--c-text-2);
}
.income__monthly {
  font-size: 18px;
  font-weight: 400;
  line-height: 26px;
  color: var(--c-text-2);
}
.income__monthly strong {
  font-family: var(--font-num);
  font-weight: 600;
  font-size: 22px;
  letter-spacing: -0.2px;
  color: var(--c-blue);
}
.income__total {
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  color: var(--c-text-3);
}

/* ── 제도 설명 카드 ── */
.desc {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 21px;
}
.desc__text {
  font-size: 15px;
  font-weight: 500;
  line-height: 25px;
  color: var(--c-text-2);
}

.factrow {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 21px;
}
.factrow__icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 9999px;
  background: var(--c-surface);
  color: var(--c-text-2);
}
.factrow__text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.factrow__value {
  font-size: 16px;
  font-weight: 500;
  line-height: 24px;
  color: #000;
}

/* ── 서류 · 신청 방법 ── */
.details {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.details__block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.minihead {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 500;
  line-height: 18px;
  letter-spacing: 0.14px;
  color: var(--c-text-2);
}
.doclist {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.doclist li {
  position: relative;
  padding-left: 27px;
  font-size: 16px;
  font-weight: 500;
  line-height: 24px;
  color: #000;
}
.doclist li::before {
  content: '•';
  position: absolute;
  left: 6px;
  color: var(--c-text-3);
}
.method {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  background: var(--c-surface);
}
.method__left {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 500;
  line-height: 24px;
  color: #000;
}
.method__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--c-border);
  border-radius: 4px;
  background: var(--c-bg);
  color: var(--c-text-2);
}
.method__channel {
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  color: var(--c-text-2);
  text-align: right;
}

/* ── 하단 고정 액션 ── */
.actionbar {
  position: sticky;
  bottom: 0;
  z-index: 10;
  display: flex;
  gap: 12px;
  padding: 25px 24px calc(24px + env(safe-area-inset-bottom));
  background: var(--c-bg);
  border-top: 1px solid var(--c-border);
}
.actionbar__save {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border: 1px solid var(--c-border);
  border-radius: 12px;
  background: var(--c-bg);
  color: #000;
}
.actionbar__primary {
  flex: 1;
  height: 56px;
  border-radius: 12px;
  background: #000;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  line-height: 18px;
  letter-spacing: 0.14px;
}
.actionbar__primary:disabled {
  opacity: 0.4;
}
</style>

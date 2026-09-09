<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOnboardingStore } from '@/stores/onboarding'
import StepProgress from '@/components/signup/StepProgress.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const route = useRoute()
const router = useRouter()
const onboarding = useOnboardingStore()

const displayProtectionDate = computed(() => onboarding.protectionEndDate.replace(/-/g, '.'))
const displaySupportDate = computed(() => onboarding.supportEndDate.replace(/-/g, '.'))

/** 정보 확인 화면에서 수정하러 들어온 경우엔 체크포인트를 건너뛰고 바로 그리로 되돌아간다. */
function next() {
  if (route.query.from === 'review') router.push('/onboarding/review')
  else router.push('/onboarding/protection-date/checkpoint')
}
</script>

<template>
  <div class="page">
    <StepProgress :current-step="1" :total-steps="4" />

    <div class="content">
      <h1 class="title">자립 시작일을 확인해주세요</h1>

      <div class="selected-box">
        <span class="selected-label">SELECTED DATE</span>
        <strong class="selected-date">{{ displayProtectionDate }}</strong>
      </div>

      <div class="card">
        <span class="card-label">지원 종료 예정일</span>
        <strong class="card-value">{{ displaySupportDate }}</strong>
      </div>

      <div class="card">
        <span class="card-label">지원 종료까지</span>
        <strong class="card-value">{{ onboarding.remainingMonths }}개월 남았어요</strong>
      </div>

      <!-- 보호종료일이 아직 오지 않은(=아직 자립하지 않은) 회원이다.
           카운트다운(D-1825)의 기준일이 보호종료일이라, 그날이 오기 전까지는 홈 화면에
           1825일짜리 카운트다운이 아니라 이 안내만 보인다 — 홈 화면 구현은 이번 범위 밖이다. -->
      <p v-if="onboarding.protectionStatus === 'IN_PROTECTION'" class="notice">
        아직 보호 중이라 카운트다운은 시작 전이에요. 보호종료일이 되어 D-1825일이 되는 날부터
        홈 화면에서 카운트다운이 시작돼요.
      </p>
    </div>

    <div class="actions">
      <PrimaryButton @click="next">다음</PrimaryButton>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 0 var(--space-page);
}

.content {
  flex: 1;
  padding-top: var(--space-lg);
}

.title {
  margin-bottom: var(--space-lg);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.selected-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: var(--space-lg);
  background-color: var(--color-bg-soft);
  border-radius: var(--radius-lg);
  text-align: center;
}

.selected-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-secondary);
  letter-spacing: 0.05em;
}

.selected-date {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  margin-top: var(--space-md);
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.card-label {
  font-size: 14px;
  color: var(--color-secondary);
}

.card-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-primary);
}

.notice {
  margin-top: var(--space-md);
  padding: var(--space-md);
  background-color: var(--color-bg-soft);
  border-radius: var(--radius-md);
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-secondary);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

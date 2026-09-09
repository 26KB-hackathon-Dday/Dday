<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'

const router = useRouter()

type Reason = 'RARELY_USED' | 'MISSING_FEATURE' | 'FREQUENT_ERROR' | 'ETC'

const REASONS: { value: Reason; label: string }[] = [
  { value: 'RARELY_USED', label: '자주 사용하지 않아요' },
  { value: 'MISSING_FEATURE', label: '원하는 기능이 부족해요' },
  { value: 'FREQUENT_ERROR', label: '앱 오류가 잦아요' },
  { value: 'ETC', label: '기타' },
]

/** UI 퍼블리싱 단계 — 아직 백엔드로 보내지 않는다. */
const selectedReason = ref<Reason | null>(null)
const isAgreed = ref(false)

const canWithdraw = computed(() => isAgreed.value)

function selectReason(value: Reason) {
  selectedReason.value = value
}

function withdraw() {
  if (!isAgreed.value) {
    showToast('최종 확인에 동의해주세요')
    return
  }
  showToast('준비 중이에요')
}
</script>

<template>
  <div class="page">
    <header class="header">
      <button class="back" type="button" aria-label="뒤로" @click="router.back()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 5-7 7 7 7" />
        </svg>
      </button>
      <h1 class="header-title">회원탈퇴</h1>
      <span class="spacer" aria-hidden="true" />
    </header>

    <div class="content">
      <div class="alert">
        <p class="alert-title">
          <svg class="alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 9v4M12 17h.01" />
            <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L14.71 3.86a2 2 0 0 0-3.42 0Z" />
          </svg>
          정말 탈퇴하시겠어요?
        </p>
        <p class="alert-body">
          탈퇴 시 개인정보, 자립 계획 등 모든 데이터가 즉시 삭제됩니다.<br />
          삭제된 데이터는 복구할 수 없습니다.
        </p>
      </div>

      <section class="card">
        <p class="card-title">탈퇴하시려는 이유가 궁금해요</p>
        <p class="card-subtitle">서비스 개선을 위해 소중한 의견을 들려주세요.</p>

        <ul class="reason-list">
          <li v-for="reason in REASONS" :key="reason.value">
            <button
              type="button"
              class="reason"
              :class="{ 'is-selected': selectedReason === reason.value }"
              @click="selectReason(reason.value)"
            >
              <span class="radio" aria-hidden="true" />
              <span class="reason-label">{{ reason.label }}</span>
            </button>
          </li>
        </ul>
      </section>

      <section class="card">
        <label class="final-check">
          <input v-model="isAgreed" type="checkbox" class="checkbox-input" />
          <span class="checkbox-box" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
              <path d="m5 12.5 4.5 4.5L19 7" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </span>
          <span class="final-check-text">
            <strong class="final-check-title">최종 확인</strong>
            <span class="final-check-body">탈퇴 시 모든 데이터가 복구 불가능하게 삭제되는 것에 동의합니다.</span>
          </span>
        </label>
      </section>
    </div>

    <div class="actions">
      <button type="button" class="btn btn--danger" :disabled="!canWithdraw" @click="withdraw">
        회원 탈퇴
      </button>
      <button type="button" class="btn btn--outline" @click="router.back()">계속 이용하기</button>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
  max-width: 430px;
  margin: 0 auto;
  background-color: #ffffff;
}

.header {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 8px;
}

.back {
  display: flex;
  padding: 8px;
  color: #17191c;
}

.back svg {
  width: 22px;
  height: 22px;
}

.header-title {
  font-size: 16px;
  font-weight: 700;
  color: #17191c;
}

.spacer {
  width: 38px;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
  padding: 0 var(--space-page) var(--space-lg);
}

/* ── 경고 안내 ── */
.alert {
  padding: var(--space-md);
  background-color: #fff0f0;
  border: 1px solid #ffc9c9;
  border-radius: 8px;
}

.alert-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 700;
  color: #e53e3e;
}

.alert-icon {
  width: 18px;
  height: 18px;
  flex: none;
}

.alert-body {
  margin-top: 8px;
  padding-left: 24px;
  font-size: 13px;
  line-height: 1.6;
  color: #5a5f65;
}

/* ── 공용 카드 ── */
.card {
  padding: var(--space-lg) var(--space-md);
  background-color: #f8f9fa;
  border-radius: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #17191c;
}

.card-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #8a8f96;
}

/* ── 탈퇴 사유 ── */
.reason-list {
  margin-top: var(--space-md);
}

.reason {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 4px;
  text-align: left;
}

.reason-list li + li .reason {
  border-top: 1px solid #e4e6e8;
}

.radio {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: 1.5px solid #c5c6ca;
  border-radius: 50%;
  transition:
    border-color 0.15s,
    background-color 0.15s;
}

.reason.is-selected .radio {
  border-color: #17191c;
  border-width: 6px;
}

.reason-label {
  font-size: 15px;
  font-weight: 500;
  color: #17191c;
}

/* ── 최종 확인 ── */
.final-check {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  cursor: pointer;
}

.checkbox-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  appearance: none;
}

.checkbox-box {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  margin-top: 1px;
  border: 1.5px solid #c5c6ca;
  border-radius: 6px;
  color: transparent;
  transition:
    background-color 0.15s,
    border-color 0.15s,
    color 0.15s;
}

.checkbox-box svg {
  width: 13px;
  height: 13px;
}

.checkbox-input:checked + .checkbox-box {
  background-color: #17191c;
  border-color: #17191c;
  color: #ffffff;
}

.final-check-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.final-check-title {
  font-size: 15px;
  font-weight: 700;
  color: #17191c;
}

.final-check-body {
  font-size: 13px;
  line-height: 1.5;
  color: #8a8f96;
}

/* ── 하단 액션 ── */
.actions {
  display: flex;
  flex: none;
  flex-direction: column;
  gap: 8px;
  padding: var(--space-md) var(--space-page);
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
  background-color: #ffffff;
}

.btn {
  height: 52px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
}

.btn--danger {
  background-color: #c9252d;
  color: #ffffff;
}

.btn--danger:disabled {
  background-color: #e9a3a6;
}

.btn--danger:not(:disabled):active {
  opacity: 0.9;
}

.btn--outline {
  background-color: #ffffff;
  border: 1px solid #e4e6e8;
  color: #17191c;
}

.btn--outline:active {
  background-color: #f8f9fa;
}
</style>

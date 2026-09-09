<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'

const router = useRouter()

interface ConnectedInstitution {
  id: string
  name: string
  type: 'bank' | 'card'
  statusText: string
  isWarning: boolean
  isToggled: boolean
}

/** 실 연동 목록/해제 API가 아직 없어 화면 목데이터로 그린다. */
const institutions = ref<ConnectedInstitution[]>([
  {
    id: 'KB',
    name: '국민은행',
    type: 'bank',
    statusText: '연결됨 · 오늘 14:32 업데이트',
    isWarning: false,
    isToggled: true,
  },
  {
    id: 'SHINHAN_CARD',
    name: '신한카드',
    type: 'card',
    statusText: '만료 임박 · 오늘 10:15 업데이트',
    isWarning: true,
    isToggled: true,
  },
  {
    id: 'TOSS',
    name: '토스뱅크',
    type: 'bank',
    statusText: '연결됨 · 오늘 14:32 업데이트',
    isWarning: false,
    isToggled: true,
  },
])

const hasExpiringSoon = computed(() => institutions.value.some((item) => item.isWarning))

function toggle(item: ConnectedInstitution) {
  item.isToggled = !item.isToggled
}

/** 토글을 끈 기관만 실제로 연결을 해제한다 — 유지할 기관은 그대로 둔다. */
function disconnect() {
  const toRemove = institutions.value.filter((item) => !item.isToggled)
  if (toRemove.length === 0) {
    showToast('해제할 기관을 먼저 꺼주세요')
    return
  }
  institutions.value = institutions.value.filter((item) => item.isToggled)
  showToast(`${toRemove.length}개 기관 연결을 해제했어요`)
}

function reconsent() {
  router.push({ path: '/mydata/select', query: { from: 'mypage' } })
}
</script>

<template>
  <div class="page">
    <header class="header">
      <button class="icon-btn" type="button" aria-label="뒤로" @click="router.back()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 5-7 7 7 7" />
        </svg>
      </button>
      <h1 class="header-title">금융정보 연결 관리</h1>
      <button class="icon-btn" type="button" aria-label="닫기" @click="router.push('/mypage')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M6 6l12 12M18 6L6 18" />
        </svg>
      </button>
    </header>

    <div class="content">
      <div v-if="hasExpiringSoon" class="alert">
        <p class="alert-title">
          <svg class="alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 9v4M12 17h.01" />
            <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L14.71 3.86a2 2 0 0 0-3.42 0Z" />
          </svg>
          동의 유효기간 만료 임박
        </p>
        <p class="alert-body">안전한 서비스 유지를 위해 연결을 연장해 주세요. (D-3)</p>
      </div>

      <p class="list-label">연결된 기관 {{ institutions.length }}</p>

      <div class="list">
        <div v-for="item in institutions" :key="item.id" class="item">
          <span class="logo">{{ item.name.slice(0, 1) }}</span>

          <div class="info">
            <strong class="name">{{ item.name }}</strong>
            <span class="status" :class="{ 'is-warning': item.isWarning }">{{ item.statusText }}</span>
          </div>

          <button
            type="button"
            class="switch"
            :class="{ 'is-on': item.isToggled }"
            role="switch"
            :aria-checked="item.isToggled"
            :aria-label="`${item.name} 연결 유지`"
            @click="toggle(item)"
          >
            <span class="switch-knob" />
          </button>
        </div>
      </div>
    </div>

    <div class="actions">
      <button type="button" class="btn btn--outline" @click="disconnect">연결 해제</button>
      <button type="button" class="btn btn--solid" @click="reconsent">다시 동의하기</button>
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

.icon-btn {
  display: flex;
  padding: 8px;
  color: var(--color-primary);
}

.icon-btn svg {
  width: 22px;
  height: 22px;
}

.header-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-primary);
}

.content {
  flex: 1;
  padding: var(--space-md) var(--space-page);
}

.alert {
  padding: var(--space-md);
  margin-bottom: var(--space-lg);
  background-color: #ffefef;
  border: 1px solid #ffd2d2;
  border-radius: 8px;
}

.alert-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 700;
  color: #e53e3e;
}

.alert-icon {
  width: 16px;
  height: 16px;
  flex: none;
}

.alert-body {
  margin-top: 4px;
  padding-left: 22px;
  font-size: 13px;
  color: #e53e3e;
}

.list-label {
  margin-bottom: var(--space-sm);
  font-size: 12px;
  color: var(--color-secondary);
}

.list {
  padding: 0 var(--space-md);
  background-color: #f8f9fa;
  border-radius: 12px;
}

.item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: var(--space-md) 0;
}

.item + .item {
  border-top: 1px solid #ececee;
}

.logo {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background-color: #ffffff;
  border-radius: 50%;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-secondary);
}

.info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.name {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
}

.status {
  font-size: 12px;
  color: var(--color-secondary);
}

.status.is-warning {
  color: #e53e3e;
}

.switch {
  position: relative;
  flex: none;
  width: 44px;
  height: 26px;
  padding: 3px;
  background-color: #d9dbe0;
  border-radius: 9999px;
  transition: background-color 0.15s;
}

.switch.is-on {
  background-color: #1a1a1a;
}

.switch-knob {
  display: block;
  width: 20px;
  height: 20px;
  background-color: #ffffff;
  border-radius: 50%;
  transition: transform 0.15s;
}

.switch.is-on .switch-knob {
  transform: translateX(18px);
}

.actions {
  display: flex;
  flex: none;
  gap: 12px;
  padding: var(--space-md) var(--space-page);
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
  background-color: #ffffff;
}

.btn {
  flex: 1;
  height: 52px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
}

.btn--outline {
  background-color: #ffffff;
  border: 1px solid var(--color-border);
  color: #1a1a1a;
}

.btn--outline:active {
  background-color: var(--color-bg-soft);
}

.btn--solid {
  background-color: #1a1a1a;
  color: #ffffff;
}

.btn--solid:active {
  opacity: 0.85;
}
</style>

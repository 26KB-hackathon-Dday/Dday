<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSignupStore } from '@/stores/signup'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import AgreementItem from '@/components/signup/AgreementItem.vue'

const router = useRouter()
const signup = useSignupStore()

const agreedTransfer = ref(false)
const agreedCollection = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

const canSubmit = computed(() => agreedTransfer.value && agreedCollection.value)

const rows = [
  { label: '제공기관', value: '연결하신 금융기관' },
  { label: '수집항목', value: '계좌 정보, 잔액, 거래 내역' },
  { label: '이용목적', value: '자산 현황 조회 및 지출 분석' },
  { label: '보유기간', value: '회원 탈퇴 시까지' },
  { label: '유효기간', value: '1년 (만료 전 재동의 요청)' },
]

async function submit() {
  if (!canSubmit.value || submitting.value) return

  submitting.value = true
  errorMessage.value = ''
  try {
    await signup.connectMydata()
    router.push('/mydata/done')
  } catch {
    errorMessage.value = '연동에 실패했습니다. 다시 시도해주세요.'
  } finally {
    submitting.value = false
  }
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
      <button class="close" type="button" aria-label="닫기" @click="router.push('/signup/done')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M6 6l12 12M18 6L6 18" />
        </svg>
      </button>
    </header>

    <div class="content">
      <h1 class="title">다음 정보 제공에<br />동의하시나요?</h1>

      <table class="table">
        <tbody>
          <tr v-for="row in rows" :key="row.label">
            <th>{{ row.label }}</th>
            <td>{{ row.value }}</td>
          </tr>
        </tbody>
      </table>

      <div class="list">
        <AgreementItem v-model="agreedTransfer" label="개인신용정보 전송요구 동의" required />
        <AgreementItem v-model="agreedCollection" label="수집·이용 동의" required />
      </div>

      <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>
    </div>

    <div class="actions">
      <PrimaryButton :disabled="!canSubmit" :loading="submitting" @click="submit">
        동의하고 연결하기
      </PrimaryButton>
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

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  margin: 0 -8px;
}

.back,
.close {
  display: flex;
  padding: 8px;
  color: var(--color-primary);
}

.back svg,
.close svg {
  width: 22px;
  height: 22px;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding-bottom: var(--space-md);
}

.title {
  margin-bottom: var(--space-lg);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.table {
  width: 100%;
  border-collapse: collapse;
  overflow: hidden;
  font-size: 13px;
  background-color: var(--color-bg-soft);
  border-radius: var(--radius-md);
}

.table th,
.table td {
  padding: 12px var(--space-md);
  text-align: left;
  vertical-align: top;
  border-bottom: 1px solid var(--color-border);
}

.table tr:last-child th,
.table tr:last-child td {
  border-bottom: none;
}

.table th {
  width: 84px;
  flex: none;
  font-weight: 700;
  color: var(--color-secondary);
  white-space: nowrap;
}

.table td {
  color: var(--color-primary);
}

.list {
  padding: 0 var(--space-sm);
  margin-top: var(--space-lg);
}

.error {
  margin-top: var(--space-md);
  font-size: 14px;
  color: var(--color-danger);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

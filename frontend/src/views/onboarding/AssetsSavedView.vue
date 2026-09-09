<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '@/api/types'
import { mydataApi, type ConnectedAccount, type Institution } from '@/api/mydata'
import { useOnboardingStore } from '@/stores/onboarding'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

/**
 * 자산은 더 이상 직접 입력받지 않는다. 마이데이터로 연동된 계좌 잔액 합계를 그대로 쓴다.
 *
 * 지금은 마이데이터 연동 화면이 온보딩 흐름에 없어서, 이 화면 진입 시 은행 기관 전체를
 * "이미 연동됐다"고 가정하고 Mock 잔액을 불러와 합산한다. 실제 연동 선택 화면이 생기면
 * 그 결과(연동된 계좌 목록)를 여기서 받아쓰도록 바꾼다.
 */
const router = useRouter()
const onboarding = useOnboardingStore()

interface DisplayAccount extends ConnectedAccount {
  institutionName: string
}

const accounts = ref<DisplayAccount[]>([])
const loading = ref(true)
const errorMessage = ref('')
const submitting = ref(false)
const connected = ref(false)

async function connectMydata() {
  loading.value = true
  errorMessage.value = ''
  try {
    const institutions = await mydataApi.findInstitutions()
    const bankInstitutions = institutions.filter((i: Institution) => i.category === 'BANK')
    const result = await mydataApi.connect({
      institutionIds: bankInstitutions.map((i) => i.institutionId),
    })
    // 기관 이름은 서버가 내려준 값을 그대로 쓴다. 프론트에서 코드를 이름으로 바꾸지 않는다.
    accounts.value = result.accounts
    onboarding.totalSaved = result.accounts.reduce((sum, account) => sum + account.balance, 0)
    connected.value = true
  } catch (e) {
    accounts.value = []
    onboarding.totalSaved = 0
    connected.value = false
    errorMessage.value = e instanceof ApiError ? e.message : '마이데이터 연동 정보를 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

onMounted(connectMydata)

async function next() {
  if (!connected.value || submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await onboarding.submitAssets()
    router.push('/onboarding/review')
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '알 수 없는 오류가 발생했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="progress-bar">
      <span class="progress-fill" />
    </div>

    <div class="content">
      <h1 class="title">현재 모아둔 자산이<br />얼마인가요?</h1>
      <p class="hint">마이데이터로 연동된 계좌 잔액을 모두 더한 금액이에요.</p>

      <div v-if="loading" class="loading">마이데이터 연동 정보를 불러오는 중이에요…</div>

      <template v-else>
        <div class="total-card">
          <span class="total-label">총 자산</span>
          <strong class="total-value">{{ onboarding.totalSaved.toLocaleString('ko-KR') }}원</strong>
        </div>

        <div class="account-list">
          <div v-for="account in accounts" :key="account.accountNumber" class="account-row">
            <div class="account-info">
              <strong class="account-institution">{{ account.institutionName }}</strong>
              <span class="account-number">{{ account.accountNumber }}</span>
            </div>
            <strong class="account-balance">{{ account.balance.toLocaleString('ko-KR') }}원</strong>
          </div>
        </div>
      </template>

      <div v-if="errorMessage" class="connection-error" role="alert">
        <p class="error">{{ errorMessage }}</p>
        <button type="button" :disabled="loading" @click="connectMydata">다시 연결하기</button>
      </div>
    </div>

    <div class="actions">
      <PrimaryButton :disabled="loading || !connected" :loading="submitting" @click="next">
        다음
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

.progress-bar {
  height: 4px;
  margin-top: var(--space-sm);
  background-color: var(--color-border);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  display: block;
  width: 90%;
  height: 100%;
  background-color: var(--color-primary);
}

.content {
  flex: 1;
  padding-top: var(--space-lg);
}

.title {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.hint {
  margin-top: var(--space-sm);
  margin-bottom: var(--space-lg);
  font-size: 14px;
  color: var(--color-secondary);
}

.loading {
  padding: var(--space-lg) 0;
  font-size: 14px;
  color: var(--color-secondary);
  text-align: center;
}

.connection-error {
  display: grid;
  justify-items: center;
  gap: var(--space-sm);
  margin-top: var(--space-md);
}

.connection-error button {
  padding: 8px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 600;
}

.total-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: var(--space-lg);
  background-color: var(--color-bg-soft);
  border-radius: var(--radius-lg);
  text-align: center;
}

.total-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-secondary);
}

.total-value {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.account-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: var(--space-md);
}

.account-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.account-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.account-institution {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-primary);
}

.account-number {
  font-size: 12px;
  color: var(--color-secondary);
}

.account-balance {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-primary);
}

.error {
  margin-top: var(--space-md);
  font-size: 13px;
  color: var(--color-danger);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

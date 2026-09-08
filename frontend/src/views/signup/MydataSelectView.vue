<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { mydataApi, type Institution } from '@/api/mydata'
import { useSignupStore } from '@/stores/signup'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import InstitutionItem from '@/components/signup/InstitutionItem.vue'

const router = useRouter()
const signup = useSignupStore()

const institutions = ref<Institution[]>([])
const loading = ref(true)

onMounted(async () => {
  institutions.value = await mydataApi.findInstitutions()
  loading.value = false
})

const banks = computed(() => institutions.value.filter((i) => i.category === 'BANK'))
const cards = computed(() => institutions.value.filter((i) => i.category === 'CARD'))

const allSelected = computed({
  get: () =>
    institutions.value.length > 0 &&
    institutions.value.every((i) => signup.selectedInstitutions.includes(i.institutionId)),
  set: (value: boolean) => {
    signup.selectedInstitutions = value ? institutions.value.map((i) => i.institutionId) : []
  },
})

const isSelected = (id: string) => signup.selectedInstitutions.includes(id)

const canSubmit = computed(() => signup.selectedInstitutions.length > 0)

function skip() {
  if (!window.confirm('금융기관 연결을 건너뛸까요? 나중에 마이페이지에서 다시 연결할 수 있어요.')) return
  signup.selectedInstitutions = []
  router.push('/signup/done')
}

function next() {
  if (!canSubmit.value) return
  router.push('/mydata/consent')
}
</script>

<template>
  <div class="page">
    <header class="header">
      <button class="close" type="button" aria-label="건너뛰기" @click="skip">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M6 6l12 12M18 6L6 18" />
        </svg>
      </button>
    </header>

    <div class="content">
      <h1 class="title">연결할 기관을 선택해 주세요</h1>

      <label class="all-select">
        <input
          type="checkbox"
          :checked="allSelected"
          @change="allSelected = ($event.target as HTMLInputElement).checked"
        />
        <span class="box" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <path d="m5 12.5 4.5 4.5L19 7" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </span>
        전체 선택
      </label>

      <p v-if="loading" class="loading">불러오는 중...</p>

      <template v-else>
        <section v-if="banks.length" class="group">
          <h2 class="group-title">은행</h2>
          <InstitutionItem
            v-for="bank in banks"
            :key="bank.institutionId"
            :institution="bank"
            :model-value="isSelected(bank.institutionId)"
            @update:model-value="signup.toggleInstitution(bank.institutionId)"
          />
        </section>

        <section v-if="cards.length" class="group">
          <h2 class="group-title">카드</h2>
          <InstitutionItem
            v-for="card in cards"
            :key="card.institutionId"
            :institution="card"
            :model-value="isSelected(card.institutionId)"
            @update:model-value="signup.toggleInstitution(card.institutionId)"
          />
        </section>
      </template>
    </div>

    <div class="actions">
      <PrimaryButton :disabled="!canSubmit" @click="next">
        {{ signup.selectedInstitutions.length > 0 ? `${signup.selectedInstitutions.length}개 기관 연결하기` : '기관을 선택해주세요' }}
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
  justify-content: flex-end;
  height: 56px;
  margin-right: -8px;
}

.close {
  display: flex;
  padding: 8px;
  color: var(--color-primary);
}

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
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.all-select {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: var(--space-md);
  margin-bottom: var(--space-sm);
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary);
  background-color: var(--color-bg-soft);
  border-radius: var(--radius-md);
  cursor: pointer;
}

.all-select input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.all-select .box {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  color: transparent;
  transition:
    background-color 0.15s,
    border-color 0.15s,
    color 0.15s;
}

.all-select .box svg {
  width: 13px;
  height: 13px;
}

.all-select input:checked + .box {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: #ffffff;
}

.loading {
  padding: var(--space-lg) 0;
  font-size: 14px;
  color: var(--color-secondary);
  text-align: center;
}

.group {
  padding: 0 var(--space-sm);
  margin-top: var(--space-md);
}

.group-title {
  margin-bottom: 4px;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-secondary);
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

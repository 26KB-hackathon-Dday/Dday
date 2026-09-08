<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useOnboardingStore } from '@/stores/onboarding'
import { REGIONS } from '@/data/regions'
import StepProgress from '@/components/signup/StepProgress.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const router = useRouter()
const onboarding = useOnboardingStore()

const keyword = ref('')

const districts = computed(
  () => REGIONS.find((region) => region.name === onboarding.regionName)?.districts ?? [],
)

const filtered = computed(() =>
  districts.value.filter((district) => district.includes(keyword.value.trim())),
)

function select(district: string) {
  onboarding.districtName = district
}

function next() {
  if (!onboarding.districtName) return
  router.push('/onboarding/region/confirm')
}
</script>

<template>
  <div class="page">
    <StepProgress :current-step="2" :total-steps="4" />

    <div class="content">
      <p class="region-name">{{ onboarding.regionName }}</p>
      <h1 class="title">어느 구/시에 살고 있나요?</h1>

      <div class="search">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="11" cy="11" r="7" />
          <path d="m21 21-4.3-4.3" />
        </svg>
        <input v-model="keyword" class="search-input" placeholder="구/시 검색" />
      </div>

      <div class="grid">
        <button
          v-for="district in filtered"
          :key="district"
          type="button"
          class="chip"
          :class="{ 'is-selected': district === onboarding.districtName }"
          @click="select(district)"
        >
          {{ district }}
        </button>
      </div>
    </div>

    <div class="actions">
      <PrimaryButton :disabled="!onboarding.districtName" @click="next">다음으로</PrimaryButton>
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
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.region-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-secondary);
}

.title {
  margin: 4px 0 var(--space-lg);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 48px;
  padding: 0 var(--space-md);
  background-color: var(--color-bg-soft);
  border-radius: var(--radius-md);
}

.search svg {
  width: 18px;
  height: 18px;
  color: var(--color-secondary);
  flex: none;
}

.search-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-top: var(--space-md);
  padding-bottom: var(--space-md);
  overflow-y: auto;
}

.chip {
  height: 48px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
}

.chip.is-selected {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: #ffffff;
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

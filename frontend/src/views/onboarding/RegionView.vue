<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOnboardingStore } from '@/stores/onboarding'
import { REGIONS } from '@/data/regions'
import StepProgress from '@/components/signup/StepProgress.vue'

const route = useRoute()
const router = useRouter()
const onboarding = useOnboardingStore()

const keyword = ref('')

const filtered = computed(() =>
  REGIONS.filter((region) => region.name.includes(keyword.value.trim())),
)

function select(name: string) {
  onboarding.regionName = name
  onboarding.districtName = ''
  router.push({ path: '/onboarding/region/district', query: route.query })
}
</script>

<template>
  <div class="page">
    <StepProgress :current-step="2" :total-steps="4" />

    <div class="content">
      <h1 class="title">지금 어디에서<br />생활하고 있나요?</h1>

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
        <input v-model="keyword" class="search-input" placeholder="시/도 검색" />
      </div>

      <van-list class="region-list">
        <van-cell
          v-for="region in filtered"
          :key="region.name"
          :title="region.name"
          is-link
          clickable
          @click="select(region.name)"
        />
      </van-list>
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

.title {
  margin-bottom: var(--space-lg);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.35;
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

.region-list {
  flex: 1;
  margin-top: var(--space-md);
  overflow-y: auto;
}
</style>

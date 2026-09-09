<script setup lang="ts">
import { useRouter } from 'vue-router'
import OnboardingCheckpoint from '@/components/onboarding/OnboardingCheckpoint.vue'

const router = useRouter()
</script>

<template>
  <OnboardingCheckpoint
    title="자립 시작 정보 입력 완료"
    @next="router.push('/onboarding/region')"
  />
</template>

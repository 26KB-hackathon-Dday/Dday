<script setup lang="ts">
import { useRoute } from 'vue-router'
import { RouterView } from 'vue-router'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import AppBottomNav from '@/components/layout/AppBottomNav.vue'

const route = useRoute()
</script>

<template>
  <div class="app-shell">
    <!-- 상세처럼 자체 헤더·하단바를 가진 화면은 meta.hideChrome로 공용 크롬을 끈다. -->
    <AppTopBar v-if="!route.meta.hideChrome" />
    <main class="app-shell__body">
      <RouterView />
    </main>
    <AppBottomNav v-if="!route.meta.hideChrome" />
  </div>
</template>

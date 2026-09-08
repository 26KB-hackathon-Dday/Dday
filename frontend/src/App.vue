<script setup lang="ts">
import { useRoute } from 'vue-router'
import { RouterView } from 'vue-router'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import AppBottomNav from '@/components/layout/AppBottomNav.vue'

const route = useRoute()
</script>

<template>
  <div class="app-shell">
    <!-- 온보딩·상세처럼 자체 헤더를 가진 화면은 meta.hideChrome로 공용 크롬을 끈다. -->
    <AppTopBar v-if="!route.meta.hideChrome" />
    <main class="app-shell__body">
      <!-- 온보딩은 뒤로가기가 잦다. 화면 상태는 스토어가 들고 있으므로
           컴포넌트는 캐시하지 않고 매번 새로 만든다. -->
      <RouterView />
    </main>
    <AppBottomNav v-if="!route.meta.hideChrome" />
  </div>
</template>

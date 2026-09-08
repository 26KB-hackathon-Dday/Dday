<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <RouterView v-slot="{ Component }">
    <!-- 온보딩은 뒤로가기가 잦다. 화면 상태는 스토어가 들고 있으므로
         컴포넌트는 캐시하지 않고 매번 새로 만든다. -->
    <component :is="Component" />
  </RouterView>
</template>

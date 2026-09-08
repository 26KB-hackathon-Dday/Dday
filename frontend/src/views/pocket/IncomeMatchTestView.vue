<template>
  <div class="test-page">
    <RecurringIncomeMatchModal
      :open="true"
      @close="handleClose"
      @matched="handleMatched"
      @registered="handleRegistered"
    />
  </div>
</template>

<script setup lang="ts">
import RecurringIncomeMatchModal from '@/components/pocket/RecurringIncomeMatchModal.vue'

const handleClose = () => {
  console.log('모달 닫힘')
}

const handleMatched = () => {
  console.log('기존 정기수입으로 확인됨')
}

const handleRegistered = () => {
  console.log('출처 자동등록 완료')
}
</script>

<style scoped>
.test-page {
  width: 100%;
  min-height: 100vh;
  background: #ffffff;
}
</style>

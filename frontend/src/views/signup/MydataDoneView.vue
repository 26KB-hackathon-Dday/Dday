<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { mydataApi, type Institution } from '@/api/mydata'
import { useSignupStore } from '@/stores/signup'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const route = useRoute()
const router = useRouter()
const signup = useSignupStore()

const institutions = ref<Institution[]>([])

onMounted(async () => {
  institutions.value = await mydataApi.findInstitutions()
})

// 서버가 기관 이름을 내려주므로 코드→이름 매핑이 필요 없다.
// 같은 은행 계좌가 여럿이면 이름이 중복되므로 한 번씩만 보여준다.
const connectedNames = computed(() =>
  [...new Set(signup.connectResult?.accounts.map((a) => a.institutionName) ?? [])],
)

const connectedCount = computed(() => signup.connectResult?.connectedCount ?? 0)

function next() {
  router.push(route.query.from === 'mypage' ? '/mypage' : '/signup/done')
}
</script>

<template>
  <div class="page">
    <div class="content">
      <div class="check-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="m5 12.5 4.5 4.5L19 7" />
        </svg>
      </div>

      <h1 class="title">금융정보 연결이<br />완료되었어요</h1>
      <p class="subtitle">{{ connectedCount }}개의 금융기관이 성공적으로 연결되었습니다.</p>

      <ul class="list">
        <li v-for="name in connectedNames" :key="name">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="m5 12.5 4.5 4.5L19 7" />
          </svg>
          {{ name }}
        </li>
      </ul>
    </div>

    <div class="actions">
      <PrimaryButton @click="next">계속하기</PrimaryButton>
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
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.check-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin-bottom: var(--space-lg);
  color: #ffffff;
  background-color: #27b87e;
  border-radius: 50%;
  animation: pop 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.check-icon svg {
  width: 36px;
  height: 36px;
}

@keyframes pop {
  from {
    transform: scale(0.6);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .check-icon {
    animation: none;
  }
}

.title {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: -0.02em;
  color: var(--color-primary);
}

.subtitle {
  margin-top: var(--space-sm);
  font-size: 14px;
  color: var(--color-secondary);
}

.list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  margin-top: var(--space-lg);
  padding: var(--space-md);
  text-align: left;
  background-color: var(--color-bg-soft);
  border-radius: var(--radius-md);
  list-style: none;
}

.list li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--color-primary);
}

.list li svg {
  flex: none;
  width: 16px;
  height: 16px;
  color: #27b87e;
}

.actions {
  padding: var(--space-md) 0;
  padding-bottom: calc(var(--space-lg) + env(safe-area-inset-bottom));
}
</style>

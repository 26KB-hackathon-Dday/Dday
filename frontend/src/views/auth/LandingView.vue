<script setup lang="ts">
import { useRouter } from 'vue-router'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

const router = useRouter()

/**
 * 자립준비청년의 지원 종료까지 남은 날. 서비스 이름이자 첫 화면의 전부다.
 *
 * 지금은 상수다 — 로그인 전이라 사용자의 실제 보호종료일을 모른다.
 * 1825 = 5년(지원 기간)이고, 개인 D-day는 로그인 후 홈에서 서버 값으로 보여준다.
 */
const DDAY = 1825
</script>

<template>
  <div class="landing">
    <!-- 카카오·네이버 로그인 화면처럼, 화면 정중앙이 아니라 살짝 위에 hero+버튼 묶음이 온다.
         .center가 flex:1로 남는 공간을 먹고 그 안에서 justify-content:center로 세로 중앙에 두되,
         padding-bottom을 더 줘서 무게중심을 위로 살짝 밀어올린다. -->
    <div class="center">
      <div class="hero">
        <h1 class="dday">D-{{ DDAY }}</h1>
        <p class="subtitle">지원이 끝나는 날까지,<br />당신의 금융 자립을 함께 준비해요.</p>
      </div>

      <div class="actions">
        <PrimaryButton @click="router.push('/signup/start')">회원가입</PrimaryButton>
        <PrimaryButton variant="ghost" @click="router.push('/login')">로그인</PrimaryButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.landing {
  display: flex;
  flex-direction: column;
  /* 100dvh는 모바일 주소창이 접혔다 펴져도 높이가 튀지 않는다.
     dvh를 모르는 브라우저는 위의 100vh로 떨어진다 */
  min-height: 100vh;
  min-height: 100dvh;
  padding: 0 var(--space-page);
}

.center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  /* 정중앙보다 살짝 위로 — 아래쪽 여백을 더 둬서 무게중심을 끌어올린다 */
  padding-bottom: 12%;
}

.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-lg);
  margin-bottom: calc(var(--space-lg) * 1.6);
  text-align: center;
}

.dday {
  font-size: 72px;
  font-weight: 800;
  letter-spacing: -0.03em;
  line-height: 1.1;
  color: var(--color-primary);
}

.subtitle {
  font-size: 19px;
  font-weight: 600;
  line-height: 1.6;
  color: var(--color-primary);
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}

/* 세로가 짧은 기기(SE 등)에서는 타이틀을 줄여 버튼이 밀려나지 않게 한다 */
@media (max-height: 640px) {
  .dday {
    font-size: 56px;
  }

  .subtitle {
    font-size: 16px;
  }

  .center {
    padding-bottom: 4%;
  }
}
</style>

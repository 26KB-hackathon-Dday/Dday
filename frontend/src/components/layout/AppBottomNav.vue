<script setup lang="ts">
/**
 * 하단 탭 내비. 탭 목록은 여기서 소유하고, 활성 표시는 라우트로 판단한다.
 * 탭 경로가 모두 최상위라 exact 활성만 보면 된다 (RouterLink가 붙이는 클래스).
 *
 * 아이콘은 임베디드 래스터가 든 손그림 SVG라 CSS 마스크가 안 먹는다 — `<img>`로 렌더하고
 * 활성/비활성은 색이 아니라 불투명도로 가른다.
 */
import homeIcon from '@/assets/icons/home.svg'
import pocketIcon from '@/assets/icons/pocket.svg'
import welfareIcon from '@/assets/icons/welfare.svg'
import mypageIcon from '@/assets/icons/mypage.svg'

const tabs = [
  { to: '/', label: '홈', icon: homeIcon },
  { to: '/pockets', label: '내 포켓', icon: pocketIcon },
  { to: '/grants', label: '지원금', icon: welfareIcon },
  { to: '/mypage', label: '마이페이지', icon: mypageIcon },
]
</script>

<template>
  <nav class="bottomnav">
    <RouterLink
      v-for="tab in tabs"
      :key="tab.to"
      :to="tab.to"
      class="bottomnav__tab"
      exact-active-class="is-active"
    >
      <span class="bottomnav__iconbox">
        <img :src="tab.icon" alt="" class="bottomnav__icon" />
      </span>
      <span class="bottomnav__label">{{ tab.label }}</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.bottomnav {
  position: sticky;
  bottom: 0;
  z-index: 10;
  display: flex;
  height: 64px;
  background: var(--c-bg);
  border-top: 1px solid var(--c-border);
  padding-bottom: env(safe-area-inset-bottom);
}

.bottomnav__tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 0;
  color: var(--c-text-3);
}

.bottomnav__iconbox {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 24px;
}
.bottomnav__icon {
  max-height: 24px;
  max-width: 26px;
  width: auto;
  object-fit: contain;
  opacity: 0.4;
}
.bottomnav__tab.is-active {
  color: #000;
}
.bottomnav__tab.is-active .bottomnav__icon {
  opacity: 1;
}

.bottomnav__label {
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
}
</style>

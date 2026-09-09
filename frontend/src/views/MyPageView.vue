<script setup lang="ts">
/**
 * 마이페이지(MYPAGE-01). 계정·금융정보·서비스 가이드 메뉴와 로그아웃.
 *
 * 대부분의 하위 화면(기본 정보 수정·약관 등)은 아직 없어 탭하면 "준비 중" 토스트만 띄운다.
 * 실제로 도는 건 로그아웃과 '연결된 금융기관'(마이데이터 화면)뿐이다.
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { userApi, type Me } from '@/api/user'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import AppIcon from '@/components/AppIcon.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const router = useRouter()
const auth = useAuthStore()

const me = ref<Me | null>(null)
const showLogoutConfirm = ref(false)

onMounted(async () => {
  me.value = await userApi.fetchMe()
})

function notReady() {
  showToast('준비 중이에요')
}

interface MenuItem {
  text: string
  badge?: string
  badgeTone?: 'on' | 'off'
  onClick: () => unknown
}

const sections = computed<{ label: string; items: MenuItem[] }[]>(() => [
  {
    label: '내 정보 관리',
    items: [
      { text: '기본 정보 수정', onClick: () => router.push('/mypage/edit') },
      { text: '보안 및 비밀번호', onClick: () => router.push('/mypage/password') },
      {
        text: '거주지역 변경',
        onClick: () => router.push({ path: '/onboarding/region', query: { from: 'mypage' } }),
      },
      {
        text: '주거 정보 수정',
        onClick: () => router.push({ path: '/onboarding/housing-type', query: { from: 'mypage' } }),
      },
    ],
  },
  {
    label: '금융정보 관리',
    items: [
      {
        text: '금융기관 연결',
        badge: me.value?.mydataConnected ? '연결됨' : '미연결',
        badgeTone: me.value?.mydataConnected ? 'on' : 'off',
        onClick: () => router.push({ path: '/mydata/select', query: { from: 'mypage' } }),
      },
      {
        text: '연결된 금융기관',
        onClick: () => router.push('/mypage/mydata'),
      },
      {
        text: '정기 수입 작성',
        onClick: () => router.push({ path: '/onboarding/income', query: { from: 'mypage' } }),
      },
    ],
  },
  {
    label: '서비스 가이드',
    items: [
      { text: '이용약관', onClick: () => router.push('/mypage/terms') },
      { text: '개인정보처리방침', onClick: () => router.push('/mypage/privacy') },
    ],
  },
])

async function logout() {
  await authApi.logout().catch(() => {}) // 서버는 무상태 — 실패해도 클라에서 지우면 끝
  auth.logout()
  router.replace('/landing')
}
</script>

<template>
  <div v-if="me" class="page">
    <!-- 프로필 -->
    <section class="profile">
      <span class="profile__avatar"><AppIcon name="profile" :size="34" /></span>
      <div class="profile__id">
        <p class="profile__name">{{ me.name }}</p>
        <p class="profile__email">{{ me.email }}</p>
      </div>
      <button
        type="button"
        class="profile__edit"
        aria-label="기본 정보 수정"
        @click="router.push('/mypage/edit')"
      >
        <AppIcon name="pencil" :size="18" />
      </button>
    </section>

    <!-- 메뉴 그룹 -->
    <section v-for="group in sections" :key="group.label" class="group">
      <p class="group__label">{{ group.label }}</p>
      <ul class="group__list">
        <li v-for="item in group.items" :key="item.text">
          <button type="button" class="row" @click="item.onClick">
            <span class="row__text">{{ item.text }}</span>
            <span class="row__right">
              <span v-if="item.badge" class="badge" :class="`badge--${item.badgeTone}`">
                {{ item.badge }}
              </span>
              <AppIcon name="chevron-right" :size="14" class="row__chevron" />
            </span>
          </button>
        </li>
      </ul>
    </section>

    <!-- 계정 관리 -->
    <section class="group">
      <p class="group__label">계정 관리</p>
      <ul class="group__list">
        <li>
          <button type="button" class="row" @click="showLogoutConfirm = true">
            <span class="row__text">로그아웃</span>
            <AppIcon name="logout" :size="18" class="row__logout" />
          </button>
        </li>
      </ul>
    </section>

    <button type="button" class="withdraw" @click="notReady">회원탈퇴</button>

    <ConfirmDialog
      v-model="showLogoutConfirm"
      title="로그아웃 하시겠습니까?"
      description="언제든 다시 로그인할 수 있어요"
      confirm-text="로그아웃"
      cancel-text="취소"
      @confirm="logout"
    />
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 16px 20px 40px;
}

/* ── 프로필 ── */
.profile {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 25px;
  border: 1px solid #e4e6e8;
  border-radius: 12px;
  background: #f5f6f8;
}
.profile__avatar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 9999px;
  background: #fff;
  color: #9a9fa6;
}
.profile__id {
  flex: 1;
  min-width: 0;
}
.profile__name {
  font-size: 20px;
  font-weight: 500;
  line-height: 28px;
  letter-spacing: -0.2px;
  color: #17191c;
}
.profile__email {
  font-size: 14px;
  line-height: 20px;
  color: #5a5f65;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.profile__edit {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  padding: 8px;
  color: #17191c;
}

/* ── 메뉴 그룹 ── */
.group {
  border: 1px solid #e4e6e8;
  border-radius: 12px;
  background: #f5f6f8;
  overflow: hidden;
}
.group__label {
  padding: 12px 16px 13px;
  border-bottom: 1px solid #e4e6e8;
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  letter-spacing: 0.6px;
  color: #5a5f65;
}
.group__list {
  list-style: none;
}
.group__list li + li .row {
  border-top: 1px solid #e4e6e8;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 16px;
  text-align: left;
}
.row__text {
  font-size: 16px;
  font-weight: 500;
  line-height: 24px;
  color: #17191c;
}
.row__right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.row__chevron {
  color: #c5c6ca;
}
.row__logout {
  color: #c5c6ca;
}
.badge {
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
}
.badge--on {
  background: rgba(57, 123, 199, 0.1);
  color: var(--c-blue);
}
.badge--off {
  background: #ececee;
  color: #8a8f96;
}

/* ── 회원탈퇴 ── */
.withdraw {
  align-self: center;
  margin-top: 4px;
  padding: 4px 8px;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.22px;
  color: #c2c7ce;
  text-decoration: underline;
}
</style>

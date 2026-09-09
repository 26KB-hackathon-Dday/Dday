<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const EFFECTIVE_DATE = '2026.01.01'

const sections = [
  {
    title: '1. 수집하는 개인정보 항목',
    body: 'Dday(이하 "회사")는 회원가입 및 서비스 제공을 위해 다음과 같은 개인정보를 수집합니다.\n  · 필수: 이름, 휴대폰번호, 이메일, 비밀번호\n  · 선택: 위치정보\n  · 자립 계획 수립을 위한 정보: 보호종료일, 거주 지역, 주거 형태, 정기수입, 보유자산\n  · 마이데이터 연동 시(선택): 연결한 금융기관명, 계좌·카드 정보, 거래 내역\n  · 서비스 이용 과정에서 자동 생성되는 정보: 접속 로그, 기기 정보',
  },
  {
    title: '2. 개인정보의 수집 및 이용 목적',
    body: '회사는 수집한 개인정보를 다음의 목적을 위해 이용합니다.\n  · 회원 식별 및 본인 확인, 로그인 인증\n  · 보호종료일 기준 자립 준비 기간(D-day) 계산 및 맞춤 자립 계획 제공\n  · 예산·지출 관리, 마이데이터 연동을 통한 자산·신용 현황 제공\n  · 정부·지자체 지원제도 안내\n  · 서비스 부정이용 방지 및 고지사항 전달',
  },
  {
    title: '3. 개인정보의 보유 및 이용기간',
    body: '회사는 원칙적으로 회원 탈퇴 시 지체 없이 수집한 개인정보를 파기합니다. 다만 관계 법령에 따라 보존할 필요가 있는 경우 회사는 관계 법령에서 정한 기간 동안 회원정보를 보관합니다.\n  · 계약 또는 청약철회 등에 관한 기록: 5년 (전자상거래 등에서의 소비자보호에 관한 법률)\n  · 소비자의 불만 또는 분쟁처리에 관한 기록: 3년 (전자상거래 등에서의 소비자보호에 관한 법률)\n  · 접속에 관한 기록: 3개월 (통신비밀보호법)',
  },
  {
    title: '4. 개인정보의 제3자 제공',
    body: '회사는 회원의 개인정보를 원칙적으로 외부에 제공하지 않습니다. 다만 아래의 경우에는 예외로 합니다.\n  · 회원이 사전에 제3자 제공에 동의한 경우 (예: 마이데이터 연동을 위한 금융기관과의 정보 송수신)\n  · 법령의 규정에 의거하거나, 수사 목적으로 법령에 정해진 절차와 방법에 따라 수사기관의 요구가 있는 경우',
  },
  {
    title: '5. 개인정보 처리의 위탁',
    body: '회사는 원활한 서비스 제공을 위해 필요한 범위 내에서 개인정보 처리를 외부 업체에 위탁할 수 있으며, 위탁 시 관계 법령에 따라 위탁계약 등을 통해 개인정보가 안전하게 관리되도록 필요한 사항을 규정합니다.',
  },
  {
    title: '6. 정보주체의 권리·의무 및 행사방법',
    body: '회원은 언제든지 마이페이지의 "기본 정보 수정"에서 등록된 개인정보를 조회·수정할 수 있으며, "회원탈퇴"를 통해 수집·이용에 대한 동의를 철회할 수 있습니다. 개인정보 열람·정정·삭제·처리정지를 요구하고자 하는 경우 개인정보 보호책임자에게 서면, 전화 또는 이메일로 연락하시면 지체 없이 조치하겠습니다.',
  },
  {
    title: '7. 개인정보의 안전성 확보조치',
    body: '회사는 개인정보보호법에 따라 다음과 같은 안전성 확보 조치를 취하고 있습니다.\n  · 비밀번호 등 중요정보의 암호화 저장\n  · 개인정보에 대한 접근권한 관리 및 접근통제\n  · 개인정보 처리 시스템 등의 취약점 점검 및 보완',
  },
  {
    title: '8. 개인정보 보호책임자',
    body: '회사는 개인정보 처리에 관한 업무를 총괄해서 책임지고, 개인정보 처리와 관련한 회원의 불만처리 및 피해구제 등을 위하여 아래와 같이 개인정보 보호책임자를 지정하고 있습니다.\n  · 담당부서: Dday 운영팀\n  · 연락처: support@dday.example.com',
  },
  {
    title: '9. 개인정보처리방침의 변경',
    body: '이 개인정보처리방침은 법령·정책 또는 보안기술의 변경에 따라 내용의 추가·삭제 및 수정이 있을 시에는 시행 최소 7일 전부터 서비스 내 공지사항을 통하여 고지할 것입니다.',
  },
]
</script>

<template>
  <div class="page">
    <header class="header">
      <button class="back" type="button" aria-label="뒤로" @click="router.back()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 5-7 7 7 7" />
        </svg>
      </button>
      <h1 class="header-title">개인정보처리방침</h1>
      <span class="spacer" aria-hidden="true" />
    </header>

    <div class="content">
      <p class="effective">시행일 {{ EFFECTIVE_DATE }}</p>

      <section v-for="section in sections" :key="section.title" class="article">
        <h2 class="article-title">{{ section.title }}</h2>
        <p class="article-body">{{ section.body }}</p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
}

.header {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 8px;
}

.back {
  display: flex;
  padding: 8px;
  color: var(--color-primary);
}

.back svg {
  width: 24px;
  height: 24px;
}

.header-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-primary);
}

.spacer {
  width: 40px;
}

.content {
  flex: 1;
  padding: 0 var(--space-page) calc(var(--space-lg) + env(safe-area-inset-bottom));
}

.effective {
  margin-bottom: var(--space-lg);
  font-size: 13px;
  color: var(--color-secondary);
}

.article {
  padding: var(--space-md) 0;
  border-top: 1px solid var(--color-border);
}

.article:first-of-type {
  border-top: none;
}

.article-title {
  margin-bottom: var(--space-sm);
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary);
}

.article-body {
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--color-secondary);
  white-space: pre-line;
}
</style>

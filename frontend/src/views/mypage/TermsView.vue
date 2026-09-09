<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const EFFECTIVE_DATE = '2026.01.01'

const articles = [
  {
    title: '제1조 (목적)',
    body: '이 약관은 Dday(이하 "회사")가 제공하는 자립 지원 서비스(이하 "서비스")의 이용과 관련하여 회사와 회원 간의 권리, 의무 및 책임사항, 기타 필요한 사항을 규정함을 목적으로 합니다.',
  },
  {
    title: '제2조 (정의)',
    body: '① "서비스"란 회원의 보호종료 시점을 기준으로 자립 계획을 세우고, 예산·지출·금융 정보를 관리할 수 있도록 회사가 제공하는 일체의 서비스를 말합니다.\n② "회원"이란 이 약관에 동의하고 서비스 이용 계약을 체결한 자를 말합니다.\n③ "마이데이터"란 회원이 동의한 범위 내에서 연동한 금융기관의 계좌·카드·거래 정보를 말합니다.',
  },
  {
    title: '제3조 (약관의 효력 및 변경)',
    body: '① 이 약관은 서비스 화면에 게시하거나 기타의 방법으로 회원에게 공지함으로써 효력이 발생합니다.\n② 회사는 관련 법령을 위반하지 않는 범위에서 이 약관을 변경할 수 있으며, 변경 시 적용일자 및 변경사유를 명시하여 최소 7일 전(회원에게 불리한 변경은 30일 전)부터 공지합니다.\n③ 회원이 변경된 약관에 동의하지 않는 경우, 서비스 이용을 중단하고 탈퇴할 수 있습니다.',
  },
  {
    title: '제4조 (서비스의 제공 및 변경)',
    body: '① 회사는 다음과 같은 서비스를 제공합니다.\n  1. 보호종료일 기준 자립 준비 기간(D-day) 계산 및 안내\n  2. 예산·지출 내역(포켓) 관리\n  3. 마이데이터 연동을 통한 자산·신용 정보 조회\n  4. 정부·지자체 지원제도(복지) 안내\n② 회사는 서비스의 내용, 운영상 또는 기술상 필요에 따라 제공하는 서비스의 전부 또는 일부를 변경할 수 있으며, 이 경우 변경 사유와 내용을 사전에 공지합니다.',
  },
  {
    title: '제5조 (회원가입)',
    body: '① 회원가입은 이용희망자가 약관 내용에 동의하고, 회사가 정한 가입 양식에 따라 이름·휴대폰번호·이메일 등 필수 정보를 기입하여 신청함으로써 성립합니다.\n② 회사는 다음 각 호에 해당하는 신청에 대하여 승인을 거부하거나 사후에 이용계약을 해지할 수 있습니다.\n  1. 타인의 명의를 도용한 경우\n  2. 가입 신청 시 필요사항을 허위로 기재한 경우\n  3. 이미 가입된 회원과 동일한 정보로 재가입하는 경우',
  },
  {
    title: '제6조 (회원 탈퇴 및 자격 상실)',
    body: '① 회원은 마이페이지에서 언제든지 탈퇴를 요청할 수 있으며, 회사는 관련 법령이 정하는 바에 따라 이를 즉시 처리합니다.\n② 탈퇴 시 회원의 서비스 이용 기록은 관련 법령 및 회사의 개인정보처리방침에 따라 처리됩니다.',
  },
  {
    title: '제7조 (개인정보의 보호)',
    body: '회사는 관련 법령이 정하는 바에 따라 회원의 개인정보를 보호하기 위해 노력하며, 개인정보의 수집·이용·제공에 관한 상세한 사항은 별도로 정하는 개인정보처리방침에 따릅니다.',
  },
  {
    title: '제8조 (회원의 의무)',
    body: '① 회원은 관련 법령, 이 약관의 규정, 이용안내 및 서비스와 관련하여 공지한 주의사항을 준수하여야 합니다.\n② 회원은 다음 각 호의 행위를 하여서는 안 됩니다.\n  1. 타인의 정보를 도용하는 행위\n  2. 회사가 제공하는 정보를 회사의 사전 승낙 없이 영리 목적으로 이용하거나 제3자에게 제공하는 행위\n  3. 서비스의 운영을 고의로 방해하는 행위',
  },
  {
    title: '제9조 (서비스 이용의 제한)',
    body: '회사는 회원이 이 약관의 의무를 위반하거나 서비스의 정상적인 운영을 방해한 경우, 사전 통지 후 서비스 이용을 단계적으로 제한하거나 이용계약을 해지할 수 있습니다.',
  },
  {
    title: '제10조 (면책조항)',
    body: '① 회사는 천재지변, 회원의 귀책사유 등 회사가 통제할 수 없는 사유로 서비스를 제공할 수 없는 경우 책임이 면제됩니다.\n② 회사가 안내하는 지원제도·금융 정보는 참고용이며, 실제 지원 여부 및 조건은 관계 기관의 확인을 따릅니다.',
  },
  {
    title: '제11조 (분쟁의 해결)',
    body: '이 약관과 관련하여 회사와 회원 간에 발생한 분쟁에 대해서는 대한민국 법을 적용하며, 관할 법원은 민사소송법에 따른 관할 법원으로 합니다.',
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
      <h1 class="header-title">이용약관</h1>
      <span class="spacer" aria-hidden="true" />
    </header>

    <div class="content">
      <p class="effective">시행일 {{ EFFECTIVE_DATE }}</p>

      <section v-for="article in articles" :key="article.title" class="article">
        <h2 class="article-title">{{ article.title }}</h2>
        <p class="article-body">{{ article.body }}</p>
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

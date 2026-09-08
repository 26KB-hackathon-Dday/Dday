/**
 * 필수 포켓 카테고리 카드의 레이아웃 확인용 예시 데이터다.
 *
 * 현재 백엔드는 포켓 월 요약과 거래 목록은 제공하지만 카테고리별 목표 예산과 사용액을
 * 함께 내려주는 API는 제공하지 않는다. 실제 응답인 것처럼 보이지 않도록 별도 파일에 두며,
 * 상세 API가 준비되면 이 상수와 화면의 안내 문구를 제거한다.
 */
export interface EssentialCategoryUsageMock {
  id: string
  name: string
  description: string
  usedAmount: number
  budgetAmount: number
  icon: string
}

export const ESSENTIAL_CATEGORY_USAGE_MOCK: EssentialCategoryUsageMock[] = [
  {
    id: 'housing',
    name: '주거비',
    description: '월세, 관리비 등',
    usedAmount: 450_000,
    budgetAmount: 500_000,
    icon: 'home',
  },
  {
    id: 'utilities',
    name: '공과금',
    description: '전기, 가스, 수도',
    usedAmount: 82_000,
    budgetAmount: 100_000,
    icon: 'doc',
  },
  {
    id: 'communications',
    name: '통신비',
    description: '휴대폰, 인터넷',
    usedAmount: 55_000,
    budgetAmount: 60_000,
    icon: 'globe',
  },
]

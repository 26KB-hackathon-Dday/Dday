/** 원 단위 금액을 화면 공통 표기인 천 단위 구분 + `원`으로 변환한다. */
export const formatWon = (value: number) => `${Math.round(value).toLocaleString('ko-KR')}원`

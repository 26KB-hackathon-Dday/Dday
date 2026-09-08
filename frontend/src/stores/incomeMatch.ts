import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useIncomeMatchStore = defineStore('incomeMatch', () => {
  /*
   * 새로 감지한 실제 입금 거래
   *
   * TODO:
   * 추후 백엔드 응답값으로 교체
   */
  const transactionId = ref('transaction-001')

  const depositedAmount = ref(298_430)

  /*
   * 화면에 보여줄 입금자명
   */
  const senderName = ref('CU 강남점')

  /*
   * 실제 자동매칭에 사용할
   * 계좌 / 거래 상대 식별 정보
   *
   * 계좌번호 원문을 프론트에서
   * 직접 관리하기보다는 추후
   * 백엔드가 sourceKey 형태로 내려주는 것을 권장
   */
  const incomeSourceKey = ref('source-cu-gangnam')

  /*
   * 온보딩에서 사용자가
   * 등록했던 예상 정기수입
   */
  const expectedIncomeId = ref(1)

  const expectedIncomeName = ref('편의점 아르바이트')

  const expectedMonthlyAmount = ref(300_000)

  /*
   * 이번 거래를 정기수입으로
   * 확인한 상태
   */
  const matched = ref(false)

  /*
   * 이 출처를 앞으로
   * 자동 매칭하도록 등록했는지
   */
  const sourceRegistered = ref(false)

  const confirmMatch = () => {
    matched.value = true
  }

  const rejectMatch = () => {
    matched.value = false
  }

  const registerSource = () => {
    sourceRegistered.value = true
  }

  const resetMatch = () => {
    matched.value = false
    sourceRegistered.value = false
  }

  return {
    transactionId,
    depositedAmount,

    senderName,
    incomeSourceKey,

    expectedIncomeId,
    expectedIncomeName,
    expectedMonthlyAmount,

    matched,
    sourceRegistered,

    confirmMatch,
    rejectMatch,
    registerSource,
    resetMatch,
  }
})

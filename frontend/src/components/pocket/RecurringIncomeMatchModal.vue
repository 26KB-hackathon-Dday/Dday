<template>
  <div v-if="open" class="modal-overlay">
    <!-- =========================
         1단계
         기존 정기수입과 같은지 확인
    ========================== -->
    <section v-if="step === 'match'" class="income-modal" role="dialog" aria-modal="true">
      <h2 class="income-modal__title">등록한 수입으로 보여요</h2>

      <p class="income-modal__description">
        <strong>
          {{ matchStore.senderName }}
        </strong>
        에서
        <strong>
          {{ formatCurrency(matchStore.depositedAmount) }}
        </strong>
        이 들어왔어요.
      </p>

      <p class="income-modal__question">
        등록한
        <strong> '{{ matchStore.expectedIncomeName }}' </strong>
        월
        {{ formatCurrency(matchStore.expectedMonthlyAmount) }}
        수입과 같은 수입인가요?
      </p>

      <div class="income-modal__notice">
        <p>'네, 맞아요'를 선택하면 정기 수입으로 처리되며, 전체 예산이 늘어나지 않아요.</p>
      </div>

      <div class="income-modal__button-row">
        <button class="income-modal__secondary" type="button" @click="handleNotMatch">
          아니에요
        </button>

        <button class="income-modal__primary" type="button" @click="handleMatch">네, 맞아요</button>
      </div>
    </section>

    <!-- =========================
         2단계
         앞으로 자동 매칭할지 확인
    ========================== -->
    <section v-else class="income-modal" role="dialog" aria-modal="true">
      <h2 class="income-modal__title">앞으로도 자동으로 인식할까요?</h2>

      <p class="income-modal__question">
        앞으로
        <strong>
          {{ matchStore.senderName }}
        </strong>
        에서 들어오는 돈을
        <strong> '{{ matchStore.expectedIncomeName }}' </strong>
        수입으로 등록하시겠어요?
      </p>

      <div class="income-modal__notice">
        <p>
          등록하면 다음부터 같은 출처에서 들어오는 돈은 자동으로
          {{ matchStore.expectedIncomeName }}
          수입으로 처리해요.
        </p>
      </div>

      <div class="income-modal__button-row">
        <button class="income-modal__secondary" type="button" @click="handleSkipRegister">
          아니요
        </button>

        <button class="income-modal__primary" type="button" @click="handleRegister">네</button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { useIncomeMatchStore } from '@/stores/incomeMatch'
import { useUnexpectedIncomeStore } from '@/stores/unexpectedIncome'

interface Props {
  open: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  close: []
  matched: []
  registered: []
}>()

const router = useRouter()

const matchStore = useIncomeMatchStore()

const unexpectedIncomeStore = useUnexpectedIncomeStore()

type ModalStep = 'match' | 'register'

const step = ref<ModalStep>('match')

/*
 * 모달이 다시 열릴 때
 * 항상 첫 질문부터 시작
 */
watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      step.value = 'match'
    }
  },
)

/*
 * "네, 맞아요"
 *
 * 이미 온보딩에서 월 예상수입에
 * 포함된 돈이므로
 * 총예산은 절대 증가시키지 않는다.
 */
const handleMatch = () => {
  matchStore.confirmMatch()

  /*
   * 중요:
   *
   * 여기서 pocketBudget.totalBudget
   * 값을 절대 변경하면 안 된다.
   */

  step.value = 'register'
}

/*
 * "아니에요"
 *
 * 정기수입이 아니라
 * 예상 밖에 새로 들어온 돈으로 처리.
 *
 * 다음:
 * 이번 달 예산에 얼마를 추가할까요?
 */
const handleNotMatch = async () => {
  matchStore.rejectMatch()

  unexpectedIncomeStore.setDetectedIncome(matchStore.depositedAmount, matchStore.transactionId)

  emit('close')

  await router.push({
    name: 'pocket-unexpected-income-amount',
  })
}

/*
 * 같은 수입은 맞지만
 * 앞으로 자동등록은 하지 않음.
 *
 * 이번 거래만 정기수입으로 처리하고 종료.
 */
const handleSkipRegister = () => {
  /*
   * TODO:
   * 백엔드 API
   *
   * transactionId에 대해
   * expectedIncomeId 수입으로
   * 확정 처리
   *
   * source mapping은 생성하지 않음
   */

  console.log('정기수입 일치 처리', {
    transactionId: matchStore.transactionId,

    expectedIncomeId: matchStore.expectedIncomeId,

    registerSource: false,
  })

  emit('matched')
  emit('close')

  step.value = 'match'
}

/*
 * 같은 수입 + 앞으로 자동등록
 */
const handleRegister = () => {
  matchStore.registerSource()

  /*
   * TODO:
   * 백엔드 API 호출
   *
   * 예:
   *
   * POST /api/income-source-mappings
   *
   * {
   *   expectedIncomeId: 1,
   *   sourceKey: "source-cu-gangnam",
   *   senderName: "CU 강남점"
   * }
   *
   * 백엔드에서 이 식별값을 저장하고
   * 이후 해당 출처 입금은
   * 자동으로 편의점 아르바이트
   * 수입으로 처리
   */

  console.log('정기수입 출처 자동등록', {
    transactionId: matchStore.transactionId,

    expectedIncomeId: matchStore.expectedIncomeId,

    expectedIncomeName: matchStore.expectedIncomeName,

    senderName: matchStore.senderName,

    incomeSourceKey: matchStore.incomeSourceKey,

    registerSource: true,
  })

  emit('registered')
  emit('close')

  step.value = 'match'
}

const formatCurrency = (value: number) => {
  return `${value.toLocaleString('ko-KR')}원`
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;

  display: flex;
  align-items: flex-end;
  justify-content: center;

  padding: 0 14px 82px;

  background: rgba(0, 0, 0, 0.38);
}

.income-modal {
  width: 100%;
  max-width: 402px;

  padding: 24px 22px 22px;

  border-radius: 14px;

  background: #ffffff;

  box-shadow: 0 14px 40px rgba(0, 0, 0, 0.12);
}

.income-modal__title {
  margin: 0;

  color: #171717;

  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;

  letter-spacing: -0.4px;
}

.income-modal__description {
  margin: 14px 0 0;

  color: #777777;

  font-size: 13px;
  line-height: 1.65;
}

.income-modal__description strong {
  color: #555555;
  font-weight: 600;
}

.income-modal__question {
  margin: 4px 0 0;

  color: #777777;

  font-size: 13px;
  line-height: 1.65;
}

.income-modal__question strong {
  color: #555555;
  font-weight: 600;
}

.income-modal__notice {
  margin-top: 18px;

  padding: 13px 14px;

  border-radius: 8px;

  background: #f6f6f7;
}

.income-modal__notice p {
  margin: 0;

  color: #777777;

  font-size: 12px;
  line-height: 1.6;
}

.income-modal__button-row {
  display: grid;
  grid-template-columns: 1fr 1fr;

  gap: 10px;

  margin-top: 20px;
}

.income-modal__primary,
.income-modal__secondary {
  width: 100%;
  height: 52px;

  border: 0;
  border-radius: 9px;

  font-size: 14px;
  font-weight: 700;

  cursor: pointer;
}

.income-modal__primary {
  color: #ffffff;
  background: #111111;
}

.income-modal__secondary {
  color: #555555;
  background: #eeeeef;
}

.income-modal__primary:active,
.income-modal__secondary:active {
  opacity: 0.85;
}
</style>

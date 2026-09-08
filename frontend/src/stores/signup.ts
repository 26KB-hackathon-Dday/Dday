import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import { mydataApi } from '@/api/mydata'
import { useAuthStore } from './auth'

/** 약관 동의 항목. 키는 `agreements` 배열에 그대로 들어간다. */
export interface Agreements {
  /** [필수] 서비스 이용약관 */
  service: boolean
  /** [필수] 개인정보 수집·이용 */
  privacy: boolean
  /** [필수] 마이데이터 전송요구 */
  mydata: boolean
  /** [선택] 마케팅 정보 수신 */
  marketing: boolean
}

/** 필수 약관 키. 여기 있는 것만 다 켜지면 다음 단계로 넘어간다. */
const REQUIRED_AGREEMENTS = ['service', 'privacy', 'mydata'] as const

/**
 * 6단계 회원가입 동안 입력값을 모아두는 스토어.
 *
 * 각 단계 화면은 자기 값만 쓰고, 실제 가입 API는 마지막에 {@link submit} 한 번만
 * 부른다. 단계 중간에 서버로 보내면 뒤로가기·중단 처리가 복잡해진다.
 *
 * 단계: 이름 → 휴대폰 인증 → 이메일 → 비밀번호 → 약관 → 기관 선택
 */
export const useSignupStore = defineStore('signup', () => {
  // 1. 이름
  const name = ref('')

  // 2. 휴대폰 (하이픈 없는 11자리)
  const phone = ref('')
  const phoneVerified = ref(false)

  // 3. 이메일
  const email = ref('')
  /** 중복 확인을 통과했는지. 이메일을 다시 고치면 false로 되돌린다 */
  const emailChecked = ref(false)

  // 4. 비밀번호
  const password = ref('')

  // 5. 약관
  const agreements = ref<Agreements>({
    service: false,
    privacy: false,
    mydata: false,
    marketing: false,
  })

  // 6. 마이데이터 기관 선택 (institutionId 목록)
  const selectedInstitutions = ref<string[]>([])

  const submitting = ref(false)

  /** 필수 약관을 모두 동의했는가 */
  const requiredAgreed = computed(() => REQUIRED_AGREEMENTS.every((key) => agreements.value[key]))

  /** 동의한 항목만 키 배열로. 가입 요청의 `agreements` 필드가 이 모양이다 */
  const agreedKeys = computed(() =>
    Object.entries(agreements.value)
      .filter(([, agreed]) => agreed)
      .map(([key]) => key),
  )

  /** 마지막 단계까지 다 채웠는가 — 가입 버튼 활성화 조건 */
  const canSubmit = computed(
    () =>
      !!name.value &&
      phoneVerified.value &&
      emailChecked.value &&
      !!password.value &&
      requiredAgreed.value,
  )

  /** 이메일을 고치면 중복 확인을 무효로 만든다. 안 그러면 확인한 적 없는 주소로 가입된다. */
  function setEmail(value: string) {
    if (value !== email.value) emailChecked.value = false
    email.value = value
  }

  /** 휴대폰 번호를 고치면 인증도 무효로 만든다. */
  function setPhone(value: string) {
    if (value !== phone.value) phoneVerified.value = false
    phone.value = value
  }

  function toggleInstitution(institutionId: string) {
    const index = selectedInstitutions.value.indexOf(institutionId)
    if (index === -1) selectedInstitutions.value.push(institutionId)
    else selectedInstitutions.value.splice(index, 1)
  }

  /**
   * 모아둔 값으로 가입하고, 받은 토큰을 auth 스토어에 넣는다.
   *
   * 실패하면 ApiError가 그대로 올라간다 — 화면에서 `e.message`를 띄운다.
   */
  async function submit() {
    submitting.value = true
    try {
      const result = await authApi.signup({
        name: name.value,
        phone: phone.value,
        email: email.value,
        password: password.value,
        agreements: agreedKeys.value,
        institutionIds: selectedInstitutions.value,
      })
      useAuthStore().setTokens(result)
      return result
    } finally {
      submitting.value = false
    }
  }

  /** 가입 직후 선택한 기관을 실제로 연동한다. 완료 화면에서 부른다. */
  async function connectMydata() {
    return mydataApi.connect({ institutionIds: selectedInstitutions.value })
  }

  /** 가입이 끝났거나 중단했을 때 입력값을 비운다. */
  function reset() {
    name.value = ''
    phone.value = ''
    phoneVerified.value = false
    email.value = ''
    emailChecked.value = false
    password.value = ''
    agreements.value = { service: false, privacy: false, mydata: false, marketing: false }
    selectedInstitutions.value = []
  }

  return {
    name,
    phone,
    phoneVerified,
    email,
    emailChecked,
    password,
    agreements,
    selectedInstitutions,
    submitting,
    requiredAgreed,
    agreedKeys,
    canSubmit,
    setEmail,
    setPhone,
    toggleInstitution,
    submit,
    connectMydata,
    reset,
  }
})

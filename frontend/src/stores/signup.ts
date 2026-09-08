import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import { mydataApi, type ConnectResponse } from '@/api/mydata'
import { useAuthStore } from './auth'

/** 하이픈 없는 11자리(01012345678) → 서버가 받는 "010-1234-5678" 형식 */
export function toHyphenatedPhone(digits: string) {
  if (digits.length < 4) return digits
  if (digits.length < 8) return `${digits.slice(0, 3)}-${digits.slice(3)}`
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
}

/**
 * 6단계 회원가입 동안 입력값을 모아두는 스토어.
 *
 * 각 단계 화면은 자기 값만 쓰고, 실제 가입 API는 마지막에 {@link submit} 한 번만
 * 부른다. 단계 중간에 서버로 보내면 뒤로가기·중단 처리가 복잡해진다.
 *
 * 필드는 백엔드 `SignupRequest`(domain/auth/dto/request/SignupRequest.java)와 1:1로 맞춘다.
 *
 * 단계: 이름 → 휴대폰 인증 → 이메일 → 비밀번호 → 약관
 */
export const useSignupStore = defineStore('signup', () => {
  // 1. 이름
  const name = ref('')

  // 2. 휴대폰 (하이픈 없는 11자리로 들고 있다가, 요청 직전에 하이픈을 붙인다)
  const phone = ref('')
  const phoneVerified = ref(false)

  // 3. 이메일
  const email = ref('')
  /** 중복 확인을 통과했는지. 이메일을 다시 고치면 false로 되돌린다 */
  const emailChecked = ref(false)

  // 4. 비밀번호
  const password = ref('')

  // 5. 약관 — 필드명은 SignupRequest와 동일하다
  const agreedTerms = ref(false)
  const agreedPrivacy = ref(false)
  const agreedLocation = ref(false)

  // 6. 마이데이터 — 가입 API와는 별도 도메인(아직 백엔드 미병합, Mock)이라
  //    SignupRequest에는 실리지 않는다. 가입 완료 후 별도로 연동한다.
  const selectedInstitutions = ref<string[]>([])
  /** 마이데이터 연동 결과. 완료 화면(MydataDoneView)이 목록을 그리는 데 쓴다. */
  const connectResult = ref<ConnectResponse | null>(null)

  const submitting = ref(false)

  /** 필수 약관(이용약관·개인정보)을 모두 동의했는가 */
  const requiredAgreed = computed(() => agreedTerms.value && agreedPrivacy.value)

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

  /** 선택한 기관을 실제로 연동한다. 전송요구 동의 화면에서 부른다. */
  async function connectMydata() {
    connectResult.value = await mydataApi.connect({ institutionIds: selectedInstitutions.value })
    return connectResult.value
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
        phone: toHyphenatedPhone(phone.value),
        email: email.value,
        password: password.value,
        agreedTerms: agreedTerms.value,
        agreedPrivacy: agreedPrivacy.value,
        agreedLocation: agreedLocation.value,
      })
      const auth = useAuthStore()
      auth.setTokens(result)
      // 갓 가입한 계정은 온보딩이 안 끝난 상태다. 같은 기기의 이전 계정 값이 남아있지 않도록 명시적으로 false로 둔다.
      auth.setOnboardingCompleted(false)
      return result
    } finally {
      submitting.value = false
    }
  }

  /** 가입이 끝났거나 중단했을 때 입력값을 비운다. */
  function reset() {
    name.value = ''
    phone.value = ''
    phoneVerified.value = false
    email.value = ''
    emailChecked.value = false
    password.value = ''
    agreedTerms.value = false
    agreedPrivacy.value = false
    agreedLocation.value = false
    selectedInstitutions.value = []
    connectResult.value = null
  }

  return {
    name,
    phone,
    phoneVerified,
    email,
    emailChecked,
    password,
    agreedTerms,
    agreedPrivacy,
    agreedLocation,
    selectedInstitutions,
    connectResult,
    submitting,
    requiredAgreed,
    canSubmit,
    setEmail,
    setPhone,
    toggleInstitution,
    connectMydata,
    submit,
    reset,
  }
})

/**
 * 백엔드 응답 봉투. 모든 API가 이 모양으로 온다.
 *
 * 정본은 백엔드의 `global/common/dto/ApiResponse.java`다. 그쪽이 바뀌면 여기도 고친다.
 */
export interface ApiResponse<T> {
  success: boolean
  /** 성공/실패 코드. 백엔드 enum 상수 이름 그대로 (예: MEMBER_NOT_FOUND) */
  code: string
  /** 사용자에게 그대로 보여줄 문구. 백엔드 ErrorCode enum이 이 문구의 정본이다 */
  message: string
  data: T
  /** 검증 실패일 때만 온다 */
  errors?: FieldError[]
}

export interface FieldError {
  field: string
  /** 백엔드 검증 어노테이션의 message 그대로 — 폼 아래 그대로 띄우면 된다 */
  reason: string
}

/**
 * 백엔드가 success:false로 응답했을 때 던져지는 에러.
 *
 * `message`를 그대로 토스트에 띄우면 되고, 분기가 필요하면 `code`로 판단한다
 * (문구는 바뀔 수 있지만 code는 계약이다).
 */
export class ApiError extends Error {
  constructor(
    readonly code: string,
    message: string,
    readonly status: number,
    readonly errors?: FieldError[],
    readonly data?: unknown,
  ) {
    super(message)
    this.name = 'ApiError'
  }

  /** 검증 실패면 { 필드명: 문구 } 로 바꿔준다. 폼에 그대로 뿌리기 좋다. */
  get fieldErrors(): Record<string, string> {
    return Object.fromEntries((this.errors ?? []).map((e) => [e.field, e.reason]))
  }
}

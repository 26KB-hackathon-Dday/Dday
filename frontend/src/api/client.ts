import { ApiError, type ApiResponse } from './types'

/**
 * 백엔드 호출 래퍼. 봉투를 벗겨서 `data`만 돌려준다.
 *
 * 주소는 항상 `/api/...` 상대경로다 — 절대 URL을 쓰지 않는다.
 * 로컬은 Vite dev 프록시가, 배포는 Cloudflare Worker가 백엔드로 넘겨준다.
 * 그래서 환경별 분기가 코드에 없다.
 *
 * ```ts
 * const members = await api.get<MemberListResponse[]>('/api/members')
 * ```
 *
 * 실패하면 {@link ApiError}를 던진다. 호출부는 try/catch로 받아서
 * `e.message`를 그대로 띄우면 된다 — 그 문구가 백엔드 ErrorCode의 정본이다.
 */
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })

  // 백엔드는 에러도 같은 봉투로 준다. 200이 아니어도 일단 파싱한다.
  let body: ApiResponse<T>
  try {
    body = await res.json()
  } catch {
    // 봉투가 아닌 응답(프록시 오류, 502 등)
    throw new ApiError('NETWORK_ERROR', '서버에 연결할 수 없습니다.', res.status)
  }

  if (!body.success) {
    throw new ApiError(body.code, body.message, res.status, body.errors, body.data)
  }
  return body.data
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body ?? {}) }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body ?? {}) }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(body ?? {}) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}

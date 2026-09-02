/**
 * Cloudflare Worker — 정적 파일 서빙 + 백엔드 API 프록시.
 *
 * 이 Worker가 없으면 브라우저가 백엔드를 아예 못 부른다.
 * 프론트는 Cloudflare에서 HTTPS로 뜨는데 백엔드(EC2)는 HTTP라서,
 * HTTPS 페이지가 HTTP를 부르는 건 브라우저가 mixed content로 차단하기 때문이다.
 *
 *   브라우저 ──HTTPS──▶ Worker ──HTTP──▶ EC2
 *                       (서버끼리는 그 규칙이 없다)
 *
 * 덤으로 브라우저 입장에선 프론트와 API가 같은 오리진이라 **CORS 설정이 아예 필요 없다.**
 * 그래서 프론트 코드는 환경과 무관하게 항상 `/api/...`만 부르면 된다
 * (로컬은 vite.config.ts의 dev 프록시가 같은 역할을 한다).
 */
export interface Env {
  /** wrangler.jsonc의 assets.binding — 빌드된 dist/를 서빙한다. */
  ASSETS: Fetcher
  /** 백엔드 오리진. wrangler.jsonc의 vars에 있다. */
  BACKEND_ORIGIN: string
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url)

    // /health 도 넘긴다. 배선이 살아있는지 프론트에서 바로 확인할 수 있고,
    // /api 엔드포인트가 하나도 없는 시점에도 프록시를 검증할 수 있다.
    if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/health')) {
      const target = new URL(url.pathname + url.search, env.BACKEND_ORIGIN)
      // Request를 통째로 넘겨 메서드·헤더·바디를 그대로 전달한다.
      // redirect: 'manual' — 백엔드가 리다이렉트를 주면 Worker가 따라가지 않고
      // 브라우저에 그대로 넘긴다. 안 그러면 Location의 http:// 주소로 따라가버린다.
      return fetch(new Request(target, request), { redirect: 'manual' })
    }

    // 나머지는 전부 정적 파일. 없는 경로는 index.html로 떨어진다
    // (wrangler.jsonc의 not_found_handling: single-page-application).
    return env.ASSETS.fetch(request)
  },
}

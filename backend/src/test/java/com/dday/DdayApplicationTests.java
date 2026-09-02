package com.dday;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 스프링 컨텍스트가 실제로 뜨는지 확인한다. <b>DB(docker compose mysql)가 떠 있어야 통과한다.</b>
 *
 * <p>단위 테스트처럼 보이지만 실제로는 배선 테스트다 — 빈 등록 실패, 잘못된 yml,
 * DB 접속 불가를 여기서 잡는다. 이게 깨지면 서버도 안 뜬다.
 */
@SpringBootTest
class DdayApplicationTests {

    @Test
    void 스프링_컨텍스트가_뜬다() {
        // 컨텍스트 로딩 자체가 검증이다.
    }
}

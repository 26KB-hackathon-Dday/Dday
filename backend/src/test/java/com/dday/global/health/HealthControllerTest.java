package com.dday.global.health;

import com.dday.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨트롤러 테스트는 {@code standaloneSetup}으로 짠다 — 스프링 컨텍스트도 DB도 띄우지 않아
 * 빠르다. 대신 XML/자바 설정 배선은 타지 않으므로, 여기가 green이어도 실제 기동은
 * {@link com.dday.DdayApplicationTests}가 따로 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private HealthController healthController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(healthController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /health 는 200과 HEALTHY 코드를 준다")
    void 헬스체크는_성공_봉투를_돌려준다() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("HEALTHY"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("DB 헬스체크는 SELECT 1 결과를 data에 담는다")
    void DB_헬스체크는_쿼리_결과를_담아_돌려준다() throws Exception {
        given(jdbcTemplate.queryForObject(anyString(), any(Class.class))).willReturn(1);

        mockMvc.perform(get("/health/db"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DB_HEALTHY"))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("DB가 죽어 있으면 500 INTERNAL_SERVER_ERROR 봉투가 나간다")
    void DB가_죽으면_500을_돌려준다() throws Exception {
        given(jdbcTemplate.queryForObject(anyString(), any(Class.class)))
                .willThrow(new RuntimeException("connection refused"));

        mockMvc.perform(get("/health/db"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
    }
}

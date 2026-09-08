package com.dday.domain.credit.controller;

import com.dday.domain.credit.dto.response.CreditScoreHistoryResponse;
import com.dday.domain.credit.dto.response.CreditScoreItemResponse;
import com.dday.domain.credit.service.CreditScoreService;
import com.dday.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CreditScoreControllerTest {

    @Mock
    private CreditScoreService creditScoreService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CreditScoreController(creditScoreService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUser(1L))
                .build();
    }

    @Test
    void 최근_신용점수를_인증_사용자_ID로_조회해_ApiResponse로_반환한다() throws Exception {
        given(creditScoreService.findRecent(1L)).willReturn(CreditScoreHistoryResponse.of(List.of(
                CreditScoreItemResponse.builder()
                        .creditScoreId(2L).score(812).agency("KCB").diff(7)
                        .percentile(new BigDecimal("57.0"))
                        .updatedAt(LocalDateTime.of(2026, 9, 1, 10, 0))
                        .build(),
                CreditScoreItemResponse.builder()
                        .creditScoreId(1L).score(805).agency("KCB").diff(null)
                        .percentile(new BigDecimal("57.9"))
                        .updatedAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                        .build())));

        mockMvc.perform(get("/api/credit/scores/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CREDIT_SCORES_FOUND"))
                .andExpect(jsonPath("$.data.latestScore").value(812))
                .andExpect(jsonPath("$.data.diffFromPrevious").value(7))
                .andExpect(jsonPath("$.data.latestPercentile").value(57.0))
                .andExpect(jsonPath("$.data.items[0].percentile").value(57.0))
                .andExpect(jsonPath("$.data.items[0].creditScoreId").value(2))
                .andExpect(jsonPath("$.data.items[0].diff").value(7))
                .andExpect(jsonPath("$.data.items[1].diff").value(nullValue()));
    }

    @Test
    void 기록이_없어도_200과_빈_목록을_준다() throws Exception {
        given(creditScoreService.findRecent(1L)).willReturn(CreditScoreHistoryResponse.empty());

        mockMvc.perform(get("/api/credit/scores/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CREDIT_SCORES_FOUND"))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    private HandlerMethodArgumentResolver authenticatedUser(Long userId) {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                return userId;
            }
        };
    }
}

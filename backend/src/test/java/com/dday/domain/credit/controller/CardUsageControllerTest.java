package com.dday.domain.credit.controller;

import com.dday.domain.credit.dto.response.CardUsageItemResponse;
import com.dday.domain.credit.dto.response.CardUsageResponse;
import com.dday.domain.credit.dto.response.CardUsageTrendResponse;
import com.dday.domain.credit.service.CardUsageService;
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
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CardUsageControllerTest {

    @Mock
    private CardUsageService cardUsageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CardUsageController(cardUsageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUser(1L))
                .build();
    }

    @Test
    void 카드_이용률을_인증_사용자_ID로_조회해_ApiResponse로_반환한다() throws Exception {
        given(cardUsageService.findUsage(1L)).willReturn(CardUsageResponse.builder()
                .months(6)
                .currentMonth("2026-09")
                .totalCreditLimit(3_000_000L)
                .currentUsage(900_000L)
                .currentUtilization(new BigDecimal("30.0"))
                .cards(List.of(CardUsageItemResponse.builder()
                        .cardId(1L)
                        .cardName("신한카드 Deep Dream")
                        .creditLimit(3_000_000L)
                        .usage(900_000L)
                        .utilization(new BigDecimal("30.0"))
                        .build()))
                .trend(List.of(CardUsageTrendResponse.builder()
                        .month("2026-08")
                        .usage(2_100_000L)
                        .utilization(new BigDecimal("70.0"))
                        .build()))
                .build());

        mockMvc.perform(get("/api/credit/card-usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CARD_USAGE_FOUND"))
                .andExpect(jsonPath("$.data.currentMonth").value("2026-09"))
                .andExpect(jsonPath("$.data.currentUtilization").value(30.0))
                .andExpect(jsonPath("$.data.cards[0].cardName").value("신한카드 Deep Dream"))
                .andExpect(jsonPath("$.data.trend[0].utilization").value(70.0));
    }

    @Test
    void 신용카드가_없어도_200과_빈_목록을_준다() throws Exception {
        given(cardUsageService.findUsage(1L)).willReturn(CardUsageResponse.empty());

        mockMvc.perform(get("/api/credit/card-usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CARD_USAGE_FOUND"))
                .andExpect(jsonPath("$.data.cards").isEmpty());
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

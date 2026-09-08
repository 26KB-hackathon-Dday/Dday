package com.dday.domain.pocket.controller;

import com.dday.domain.pocket.dto.response.CategoryListResponse;
import com.dday.domain.pocket.dto.response.PocketMonthlyResponse;
import com.dday.domain.pocket.dto.response.TransactionDetailResponse;
import com.dday.domain.pocket.service.CategoryService;
import com.dday.domain.pocket.service.PocketService;
import com.dday.domain.pocket.service.TransactionQueryService;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PocketQueryControllerTest {

    @Mock
    private PocketService pocketService;
    @Mock
    private TransactionQueryService transactionQueryService;
    @Mock
    private CategoryService categoryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PocketController(pocketService, transactionQueryService),
                        new TransactionController(transactionQueryService),
                        new CategoryController(categoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUser(1L))
                .build();
    }

    @Test
    void 월별_포켓_현황을_ApiResponse로_반환한다() throws Exception {
        given(pocketService.findMonthly(1L, "2026-09"))
                .willReturn(PocketMonthlyResponse.builder()
                        .month("2026-09")
                        .totalBudgetAmount(1_000_000L)
                        .pockets(List.of())
                        .build());

        mockMvc.perform(get("/api/pockets/monthly").param("month", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MONTHLY_POCKETS_FOUND"))
                .andExpect(jsonPath("$.data.month").value("2026-09"))
                .andExpect(jsonPath("$.data.totalBudgetAmount").value(1_000_000));
    }

    @Test
    void 카테고리_필터를_포켓_유형으로_바인딩한다() throws Exception {
        given(categoryService.findAll(com.dday.domain.pocket.entity.PocketType.FREE))
                .willReturn(CategoryListResponse.builder().categories(List.of()).build());

        mockMvc.perform(get("/api/categories").param("pocketType", "FREE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CATEGORIES_FOUND"));
    }

    @Test
    void 거래_상세는_인증_사용자_ID로_조회한다() throws Exception {
        given(transactionQueryService.findById(1L, 10L))
                .willReturn(TransactionDetailResponse.builder().transactionId(10L).build());

        mockMvc.perform(get("/api/transactions/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TRANSACTION_FOUND"))
                .andExpect(jsonPath("$.data.transactionId").value(10));
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

package com.dday.domain.pocket.controller;

import com.dday.domain.pocket.dto.response.CategoryListResponse;
import com.dday.domain.pocket.dto.response.PocketMonthlyResponse;
import com.dday.domain.pocket.dto.response.MonthlyPocketSettlementResponse;
import com.dday.domain.pocket.dto.response.PocketCategoryUsageResponse;
import com.dday.domain.pocket.dto.response.PocketResponse;
import com.dday.domain.pocket.dto.response.TransactionDetailResponse;
import com.dday.domain.pocket.dto.response.AutoClassificationResponse;
import com.dday.domain.pocket.dto.response.PocketInitializeResponse;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.service.CategoryService;
import com.dday.domain.pocket.service.PocketService;
import com.dday.domain.pocket.service.PocketCategoryUsageService;
import com.dday.domain.pocket.service.MonthlyPocketSettlementService;
import com.dday.domain.pocket.service.TransactionQueryService;
import com.dday.domain.pocket.service.TransactionClassificationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PocketQueryControllerTest {

    @Mock
    private PocketService pocketService;
    @Mock
    private PocketCategoryUsageService pocketCategoryUsageService;
    @Mock
    private MonthlyPocketSettlementService monthlyPocketSettlementService;
    @Mock
    private TransactionQueryService transactionQueryService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private TransactionClassificationService transactionClassificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PocketController(
                                pocketService, pocketCategoryUsageService,
                                monthlyPocketSettlementService, transactionQueryService),
                        new TransactionController(transactionQueryService, transactionClassificationService),
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
    void 카테고리별_사용_현황을_인증_사용자와_포켓_유형으로_조회한다() throws Exception {
        given(pocketCategoryUsageService.findMonthly(1L, PocketType.ESSENTIAL, "2026-09"))
                .willReturn(PocketCategoryUsageResponse.builder()
                        .month("2026-09")
                        .pocketType(PocketType.ESSENTIAL)
                        .totalUsedAmount(450_000L)
                        .categories(List.of())
                        .unclassifiedUsedAmount(0L)
                        .build());

        mockMvc.perform(get("/api/pockets/ESSENTIAL/category-usage")
                        .param("month", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("POCKET_CATEGORY_USAGE_FOUND"))
                .andExpect(jsonPath("$.data.month").value("2026-09"))
                .andExpect(jsonPath("$.data.totalUsedAmount").value(450_000));
    }

    @Test
    void 월말_포켓_정산_결과를_ApiResponse로_반환한다() throws Exception {
        given(monthlyPocketSettlementService.settle(1L, "2026-09"))
                .willReturn(MonthlyPocketSettlementResponse.builder()
                        .month("2026-09")
                        .totalTargetAmount(800_000L)
                        .totalUsedAmount(750_000L)
                        .totalRemainingAmount(100_000L)
                        .totalOverAmount(50_000L)
                        .pockets(List.of())
                        .build());

        mockMvc.perform(post("/api/pockets/monthly-settlements")
                        .param("month", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MONTHLY_POCKETS_SETTLED"))
                .andExpect(jsonPath("$.data.month").value("2026-09"))
                .andExpect(jsonPath("$.data.totalUsedAmount").value(750_000))
                .andExpect(jsonPath("$.data.totalOverAmount").value(50_000));
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

    @Test
    void 미분류_거래_자동_분류_결과를_반환한다() throws Exception {
        given(transactionClassificationService.classifyUnclassified(1L))
                .willReturn(AutoClassificationResponse.builder()
                        .targetCount(3).userRuleCount(2).defaultFreeCount(1)
                        .remainingUnclassifiedCount(0).build());

        mockMvc.perform(post("/api/transactions/classify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TRANSACTIONS_CLASSIFIED"))
                .andExpect(jsonPath("$.data.targetCount").value(3))
                .andExpect(jsonPath("$.data.userRuleCount").value(2));
    }

    @Test
    void 기본_포켓_초기화는_생성_건수를_반환한다() throws Exception {
        given(pocketService.initialize(1L)).willReturn(PocketInitializeResponse.builder()
                .createdCount(2).totalCount(4).build());

        mockMvc.perform(post("/api/pockets/initialize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("POCKETS_INITIALIZED"))
                .andExpect(jsonPath("$.data.createdCount").value(2))
                .andExpect(jsonPath("$.data.totalCount").value(4));
    }

    @Test
    void 포켓_이름과_설명을_부분_수정한다() throws Exception {
        given(pocketService.update(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
                .willReturn(PocketResponse.builder()
                        .pocketId(10L).pocketType(PocketType.FREE)
                        .pocketName("생활비").description("자유롭게 사용").build());

        mockMvc.perform(patch("/api/pockets/10")
                        .contentType("application/json")
                        .content("""
                                {"pocketName":"생활비","description":"자유롭게 사용"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("POCKET_UPDATED"))
                .andExpect(jsonPath("$.data.pocketName").value("생활비"));
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

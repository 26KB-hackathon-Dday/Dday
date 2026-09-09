package com.dday.domain.credit.controller;

import com.dday.domain.credit.dto.response.PaymentHistoryResponse;
import com.dday.domain.credit.dto.response.PaymentRecordResponse;
import com.dday.domain.credit.dto.response.PaymentSyncResponse;
import com.dday.domain.credit.dto.response.PaymentTypeHistoryResponse;
import com.dday.domain.credit.entity.PaymentStatus;
import com.dday.domain.credit.entity.PaymentType;
import com.dday.domain.credit.service.NonFinancialPaymentService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NonFinancialPaymentControllerTest {

    @Mock
    private NonFinancialPaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NonFinancialPaymentController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUser(1L))
                .build();
    }

    @Test
    void 납부_이력을_인증_사용자_ID로_조회해_ApiResponse로_반환한다() throws Exception {
        given(paymentService.findHistory(1L)).willReturn(PaymentHistoryResponse.builder()
                .monthsCovered(12)
                .onTimeCount(35)
                .lateCount(1)
                .unpaidCount(0)
                .types(List.of(PaymentTypeHistoryResponse.builder()
                        .paymentType(PaymentType.TELECOM)
                        .label("통신요금")
                        .institutionName("SK텔레콤")
                        .latestBillingMonth("2026-09")
                        .onTimeStreak(5)
                        .lateCount(1)
                        .records(List.of(PaymentRecordResponse.builder()
                                .billingMonth("2026-09")
                                .amount(38_500L)
                                .dueDate(LocalDate.of(2026, 10, 25))
                                .paidDate(LocalDate.of(2026, 10, 24))
                                .status(PaymentStatus.PAID)
                                .statusLabel("정상 납부")
                                .build()))
                        .build()))
                .build());

        mockMvc.perform(get("/api/credit/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PAYMENTS_FOUND"))
                .andExpect(jsonPath("$.data.monthsCovered").value(12))
                .andExpect(jsonPath("$.data.lateCount").value(1))
                .andExpect(jsonPath("$.data.types[0].label").value("통신요금"))
                .andExpect(jsonPath("$.data.types[0].onTimeStreak").value(5))
                .andExpect(jsonPath("$.data.types[0].records[0].statusLabel").value("정상 납부"));
    }

    @Test
    void 이력이_없어도_200과_빈_목록을_준다() throws Exception {
        given(paymentService.findHistory(1L)).willReturn(PaymentHistoryResponse.empty());

        mockMvc.perform(get("/api/credit/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PAYMENTS_FOUND"))
                .andExpect(jsonPath("$.data.types").isEmpty());
    }

    @Test
    void 동기화는_새로_만든_건수를_돌려준다() throws Exception {
        given(paymentService.sync(1L)).willReturn(PaymentSyncResponse.builder()
                .createdCount(36)
                .monthsCovered(12)
                .build());

        mockMvc.perform(post("/api/credit/payments/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PAYMENTS_SYNCED"))
                .andExpect(jsonPath("$.data.createdCount").value(36));
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

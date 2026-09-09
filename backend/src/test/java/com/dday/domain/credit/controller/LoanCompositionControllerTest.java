package com.dday.domain.credit.controller;

import com.dday.domain.credit.dto.response.LoanCompositionResponse;
import com.dday.domain.credit.dto.response.LoanItemResponse;
import com.dday.domain.credit.dto.response.LoanSectorResponse;
import com.dday.domain.credit.service.LoanCompositionService;
import com.dday.domain.mydata.entity.FinancialSector;
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
class LoanCompositionControllerTest {

    @Mock
    private LoanCompositionService loanCompositionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new LoanCompositionController(loanCompositionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUser(1L))
                .build();
    }

    @Test
    void 대출_구성을_인증_사용자_ID로_조회해_ApiResponse로_반환한다() throws Exception {
        given(loanCompositionService.findComposition(1L))
                .willReturn(LoanCompositionResponse.builder()
                        .loanCount(2)
                        .totalBalance(11_000_000L)
                        .sectors(List.of(LoanSectorResponse.builder()
                                .sector(FinancialSector.NON_BANK)
                                .label("제2금융권")
                                .loanCount(1)
                                .totalBalance(3_000_000L)
                                .build()))
                        .loans(List.of(LoanItemResponse.builder()
                                .accountId(1L)
                                .institutionName("현대캐피탈")
                                .sector(FinancialSector.NON_BANK)
                                .sectorLabel("제2금융권")
                                .productName("다이렉트론")
                                .balance(3_000_000L)
                                .interestRate(new BigDecimal("15.40"))
                                .build()))
                        .build());

        mockMvc.perform(get("/api/credit/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LOANS_FOUND"))
                .andExpect(jsonPath("$.data.loanCount").value(2))
                .andExpect(jsonPath("$.data.totalBalance").value(11_000_000))
                .andExpect(jsonPath("$.data.sectors[0].label").value("제2금융권"))
                .andExpect(jsonPath("$.data.loans[0].institutionName").value("현대캐피탈"))
                .andExpect(jsonPath("$.data.loans[0].interestRate").value(15.40));
    }

    @Test
    void 대출이_없어도_200과_빈_목록을_준다() throws Exception {
        given(loanCompositionService.findComposition(1L))
                .willReturn(LoanCompositionResponse.empty());

        mockMvc.perform(get("/api/credit/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LOANS_FOUND"))
                .andExpect(jsonPath("$.data.loans").isEmpty());
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

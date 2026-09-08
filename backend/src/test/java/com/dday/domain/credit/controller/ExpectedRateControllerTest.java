package com.dday.domain.credit.controller;

import com.dday.domain.credit.dto.response.ExpectedRateResponse;
import com.dday.domain.credit.dto.response.LenderRateResponse;
import com.dday.domain.credit.dto.response.LenderType;
import com.dday.domain.credit.service.ExpectedRateService;
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
class ExpectedRateControllerTest {

    @Mock
    private ExpectedRateService expectedRateService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExpectedRateController(expectedRateService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUser(1L))
                .build();
    }

    @Test
    void 예상_금리를_인증_사용자_ID로_조회해_ApiResponse로_반환한다() throws Exception {
        given(expectedRateService.findExpected(1L)).willReturn(ExpectedRateResponse.builder()
                .score(704)
                .principal(10_000_000L)
                .targetScore(751)
                .scoreGap(47)
                .lenders(List.of(LenderRateResponse.builder()
                        .lenderType(LenderType.BANK)
                        .institutionCount(17)
                        .currentRate(new BigDecimal("7.04"))
                        .currentAnnualInterest(704_000L)
                        .targetRate(new BigDecimal("6.58"))
                        .targetAnnualInterest(658_000L)
                        .annualSaving(46_000L)
                        .build()))
                .build());

        mockMvc.perform(get("/api/credit/rates/expected"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("EXPECTED_RATES_FOUND"))
                .andExpect(jsonPath("$.data.score").value(704))
                .andExpect(jsonPath("$.data.targetScore").value(751))
                .andExpect(jsonPath("$.data.scoreGap").value(47))
                .andExpect(jsonPath("$.data.lenders[0].lenderType").value("BANK"))
                .andExpect(jsonPath("$.data.lenders[0].currentRate").value(7.04))
                .andExpect(jsonPath("$.data.lenders[0].annualSaving").value(46000));
    }

    @Test
    void 신용점수_기록이_없어도_200과_빈_목록을_준다() throws Exception {
        given(expectedRateService.findExpected(1L)).willReturn(ExpectedRateResponse.empty());

        mockMvc.perform(get("/api/credit/rates/expected"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("EXPECTED_RATES_FOUND"))
                .andExpect(jsonPath("$.data.lenders").isEmpty());
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

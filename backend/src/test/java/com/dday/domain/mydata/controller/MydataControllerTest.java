package com.dday.domain.mydata.controller;

import com.dday.domain.mydata.dto.response.MydataConnectResponse;
import com.dday.domain.mydata.dto.response.MydataSyncResponse;
import com.dday.domain.mydata.service.MydataConnectService;
import com.dday.domain.mydata.service.MydataService;
import com.dday.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MydataControllerTest {

    @Mock private MydataService mydataService;
    @Mock private MydataConnectService mydataConnectService;
    @InjectMocks private MydataController mydataController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mydataController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUser(1L))
                .build();
    }

    @Test
    void 동기화_결과를_ApiResponse의_data로_반환한다() throws Exception {
        LocalDateTime from = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 10, 1, 0, 0);
        given(mydataService.sync(eq(1L), eq(from), eq(to)))
                .willReturn(MydataSyncResponse.builder()
                        .accountCount(2).insertedTransactionCount(5).syncedAt(to).build());

        mockMvc.perform(post("/api/mydata/sync")
                        .param("from", "2026-07-01T00:00:00")
                        .param("to", "2026-10-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MYDATA_SYNCED"))
                .andExpect(jsonPath("$.data.accountCount").value(2))
                .andExpect(jsonPath("$.data.insertedTransactionCount").value(5));
    }

    @Test
    void 연결하면_첫_동기화_결과를_ApiResponse의_data로_반환한다() throws Exception {
        given(mydataConnectService.connect(1L))
                .willReturn(MydataConnectResponse.builder()
                        .connectedCount(2)
                        .accounts(List.of())
                        .insertedTransactionCount(14)
                        .build());

        mockMvc.perform(post("/api/mydata/connect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MYDATA_CONNECTED"))
                .andExpect(jsonPath("$.data.connectedCount").value(2))
                .andExpect(jsonPath("$.data.insertedTransactionCount").value(14));
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

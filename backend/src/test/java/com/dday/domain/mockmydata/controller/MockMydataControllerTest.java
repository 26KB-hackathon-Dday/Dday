package com.dday.domain.mockmydata.controller;

import com.dday.domain.mockmydata.dto.MockMydataErrorCode;
import com.dday.domain.mockmydata.dto.response.MockAccountListResponse;
import com.dday.domain.mockmydata.dto.response.MockAccountResponse;
import com.dday.domain.mockmydata.service.MockMydataService;
import com.dday.global.exception.BusinessException;
import com.dday.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MockMydataControllerTest {

    @Mock
    private MockMydataService mockMydataService;

    @InjectMocks
    private MockMydataController mockMydataController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mockMydataController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 계좌_목록은_ApiResponse의_data로_반환한다() throws Exception {
        MockAccountResponse account = MockAccountResponse.builder()
                .accountId("ACC-001")
                .orgCode("KB")
                .accountNum("123-456")
                .balance(500_000L)
                .build();
        given(mockMydataService.findAccounts(1L))
                .willReturn(MockAccountListResponse.builder().accounts(List.of(account)).build());

        mockMvc.perform(get("/mock/mydata/users/1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("ACCOUNTS_FOUND"))
                .andExpect(jsonPath("$.data.accounts[0].accountId").value("ACC-001"))
                .andExpect(jsonPath("$.data.accounts[0].mockAccountId").doesNotExist());
    }

    @Test
    void 잘못된_날짜_형식은_400을_반환한다() throws Exception {
        mockMvc.perform(get("/mock/mydata/accounts/ACC-001/transactions")
                        .param("from", "2026-08-01")
                        .param("to", "2026-09-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 존재하지_않는_카드는_404를_반환한다() throws Exception {
        given(mockMydataService.findCardTransactions(
                org.mockito.ArgumentMatchers.eq("CARD-404"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .willThrow(new BusinessException(MockMydataErrorCode.MOCK_MYDATA_CARD_NOT_FOUND));

        mockMvc.perform(get("/mock/mydata/cards/CARD-404/transactions")
                        .param("from", "2026-08-01T00:00:00")
                        .param("to", "2026-09-01T00:00:00"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MOCK_MYDATA_CARD_NOT_FOUND"));
    }
}

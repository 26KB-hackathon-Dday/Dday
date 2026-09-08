package com.dday.domain.mockmydata.controller;

import com.dday.domain.mockmydata.dto.MockMydataSuccessCode;
import com.dday.domain.mockmydata.dto.response.*;
import com.dday.domain.mockmydata.service.MockMydataService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "Mock MyData")
@Profile("local")
@RestController
@RequestMapping("/mock/mydata")
@RequiredArgsConstructor
public class MockMydataController {

    private final MockMydataService mockMydataService;

    @Operation(summary = "Mock MyData 계좌 목록 조회")
    @GetMapping("/users/{serviceUserId}/accounts")
    public ResponseEntity<ApiResponse<MockAccountListResponse>> findAccounts(
            @PathVariable Long serviceUserId) {
        return ApiResponse.of(MockMydataSuccessCode.ACCOUNTS_FOUND,
                mockMydataService.findAccounts(serviceUserId));
    }

    @Operation(summary = "Mock MyData 카드 목록 조회")
    @GetMapping("/users/{serviceUserId}/cards")
    public ResponseEntity<ApiResponse<MockCardListResponse>> findCards(
            @PathVariable Long serviceUserId) {
        return ApiResponse.of(MockMydataSuccessCode.CARDS_FOUND,
                mockMydataService.findCards(serviceUserId));
    }

    @Operation(summary = "Mock MyData 계좌 거래 조회")
    @GetMapping("/accounts/{externalAccountId}/transactions")
    public ResponseEntity<ApiResponse<MockAccountTransactionListResponse>> findAccountTransactions(
            @PathVariable String externalAccountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.of(MockMydataSuccessCode.ACCOUNT_TRANSACTIONS_FOUND,
                mockMydataService.findAccountTransactions(externalAccountId, from, to));
    }

    @Operation(summary = "Mock MyData 카드 거래 조회")
    @GetMapping("/cards/{externalCardId}/transactions")
    public ResponseEntity<ApiResponse<MockCardTransactionListResponse>> findCardTransactions(
            @PathVariable String externalCardId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.of(MockMydataSuccessCode.CARD_TRANSACTIONS_FOUND,
                mockMydataService.findCardTransactions(externalCardId, from, to));
    }
}

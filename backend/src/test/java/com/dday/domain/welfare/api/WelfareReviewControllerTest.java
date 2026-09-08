package com.dday.domain.welfare.api;

import com.dday.domain.welfare.dto.response.WelfareReviewItemResponse;
import com.dday.domain.welfare.entity.AgencyType;
import com.dday.domain.welfare.service.WelfareProgramService;
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
class WelfareReviewControllerTest {

    @Mock
    WelfareProgramService service;

    @InjectMocks
    WelfareReviewController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 리뷰_큐를_data에_담아_돌려준다() throws Exception {
        var item = WelfareReviewItemResponse.builder()
                .programId("WLF00004661").name("청년월세 지원사업").agencyType(AgencyType.CENTRAL)
                .issues(List.of("TARGET_LOOKS_LIKE_NOTICE"))
                .targetDescription("※ '26년 신규수혜자 신청접수 기간: 3.30 ~ 5.29")
                .build();
        given(service.getReviewQueue()).willReturn(List.of(item));

        mockMvc.perform(get("/internal/welfare/review-queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WELFARE_REVIEW_QUEUE_FOUND"))
                .andExpect(jsonPath("$.data[0].programId").value("WLF00004661"))
                .andExpect(jsonPath("$.data[0].issues[0]").value("TARGET_LOOKS_LIKE_NOTICE"));
    }

    @Test
    void 큐가_비면_빈_배열() throws Exception {
        given(service.getReviewQueue()).willReturn(List.of());

        mockMvc.perform(get("/internal/welfare/review-queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}

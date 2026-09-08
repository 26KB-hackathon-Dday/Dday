package com.dday.domain.welfare.api;

import com.dday.domain.welfare.dto.WelfareErrorCode;
import com.dday.domain.welfare.dto.response.ProgramStatus;
import com.dday.domain.welfare.dto.response.WelfareProgramSummaryResponse;
import com.dday.domain.welfare.service.WelfareProgramService;
import com.dday.global.common.dto.PageResponse;
import com.dday.global.exception.BusinessException;
import com.dday.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WelfareProgramControllerTest {

    @Mock
    WelfareProgramService service;

    @InjectMocks
    WelfareProgramController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void 목록은_PageResponse를_data에_담아_돌려준다() throws Exception {
        var card = WelfareProgramSummaryResponse.builder()
                .programId("WLF00004661").name("청년 월세 특별지원").category("주거")
                .benefitText("월 200,000원").periodText("~ 2026.12.31 마감").status(ProgramStatus.OPEN)
                .build();
        given(service.search(any(), any(), any()))
                .willReturn(PageResponse.of(new org.springframework.data.domain.PageImpl<>(
                        List.of(card), org.springframework.data.domain.PageRequest.of(0, 20), 1), c -> c));

        mockMvc.perform(get("/api/v1/welfare-programs").param("q", "월세"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WELFARE_PROGRAMS_FOUND"))
                .andExpect(jsonPath("$.data.content[0].programId").value("WLF00004661"))
                .andExpect(jsonPath("$.data.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void 없는_제도_상세는_404_PROGRAM_NOT_FOUND() throws Exception {
        given(service.getByProgramId(eq("nope")))
                .willThrow(new BusinessException(WelfareErrorCode.PROGRAM_NOT_FOUND));

        mockMvc.perform(get("/api/v1/welfare-programs/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PROGRAM_NOT_FOUND"));
    }
}

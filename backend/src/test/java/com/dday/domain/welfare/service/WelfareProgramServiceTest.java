package com.dday.domain.welfare.service;

import com.dday.domain.welfare.dto.WelfareErrorCode;
import com.dday.domain.welfare.entity.CurationStatus;
import com.dday.domain.welfare.entity.ProgramSource;
import com.dday.domain.welfare.entity.WelfareProgram;
import com.dday.domain.welfare.repository.WelfareProgramRepository;
import com.dday.global.common.dto.PageResponse;
import com.dday.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WelfareProgramServiceTest {

    @Mock
    WelfareProgramRepository repository;

    @InjectMocks
    WelfareProgramService service;

    @Test
    void 빈_검색어와_카테고리는_null로_넘어간다() {
        given(repository.search(isNull(), isNull(), any(Pageable.class)))
                .willReturn(Page.empty());

        service.search("  ", "", PageRequest.of(0, 20));

        verify(repository).search(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void 검색어는_trim해서_넘긴다() {
        given(repository.search(eq("월세"), eq("주거"), any(Pageable.class)))
                .willReturn(Page.empty());

        service.search(" 월세 ", "주거", PageRequest.of(0, 20));

        verify(repository).search(eq("월세"), eq("주거"), any(Pageable.class));
    }

    @Test
    void 검색_결과를_PageResponse로_감싸_매핑한다() {
        WelfareProgram program = program("WLF00004661", "청년 월세 특별지원", "주거");
        Page<WelfareProgram> page = new PageImpl<>(List.of(program), PageRequest.of(0, 20), 1);
        given(repository.search(any(), any(), any())).willReturn(page);

        PageResponse<?> result = service.search(null, null, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void 존재하지_않는_programId면_PROGRAM_NOT_FOUND() {
        given(repository.findByServId("nope")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByProgramId("nope"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(WelfareErrorCode.PROGRAM_NOT_FOUND);
    }

    @Test
    void 검토중_제도는_상세_조회에서_PROGRAM_NOT_FOUND() {
        WelfareProgram program = program("WLF00004661", "청년월세 지원사업", "주거");
        ReflectionTestUtils.setField(program, "curationStatus", CurationStatus.NEEDS_REVIEW);
        given(repository.findByServId("WLF00004661")).willReturn(Optional.of(program));

        assertThatThrownBy(() -> service.getByProgramId("WLF00004661"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(WelfareErrorCode.PROGRAM_NOT_FOUND);
    }

    @Test
    void 검토중이어도_MANUAL_CURATION이면_상세가_보인다() {
        WelfareProgram program = program("WLF00004661", "청년월세 지원사업", "주거");
        ReflectionTestUtils.setField(program, "curationStatus", CurationStatus.NEEDS_REVIEW);
        ReflectionTestUtils.setField(program, "source", ProgramSource.MANUAL_CURATION);
        given(repository.findByServId("WLF00004661")).willReturn(Optional.of(program));

        assertThat(service.getByProgramId("WLF00004661").getProgramId()).isEqualTo("WLF00004661");
    }

    @Test
    void 상세_조회는_servId로_찾는다() {
        WelfareProgram program = program("WLF00001175", "자립수당", "생활");
        given(repository.findByServId("WLF00001175")).willReturn(Optional.of(program));

        var result = service.getByProgramId("WLF00001175");

        assertThat(result.getProgramId()).isEqualTo("WLF00001175");
        assertThat(result.getName()).isEqualTo("자립수당");
    }

    @Test
    void 리뷰_큐는_NEEDS_REVIEW_AND_API_CANDIDATE만_조회해_매핑한다() {
        WelfareProgram program = program("WLF00004661", "청년월세 지원사업", "주거");
        ReflectionTestUtils.setField(program, "curationIssues", "TARGET_LOOKS_LIKE_NOTICE");
        given(repository.findByCurationStatusAndSourceOrderByServId(
                CurationStatus.NEEDS_REVIEW, ProgramSource.API_CANDIDATE))
                .willReturn(List.of(program));

        var queue = service.getReviewQueue();

        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).getProgramId()).isEqualTo("WLF00004661");
        assertThat(queue.get(0).getIssues()).containsExactly("TARGET_LOOKS_LIKE_NOTICE");
    }

    /** 엔티티에 세터가 없어(AGENTS §4) 테스트에서는 리플렉션으로 필드를 채운다. */
    private static WelfareProgram program(String servId, String name, String category) {
        WelfareProgram program = BeanUtils.instantiateClass(WelfareProgram.class);
        ReflectionTestUtils.setField(program, "servId", servId);
        ReflectionTestUtils.setField(program, "servNm", name);
        ReflectionTestUtils.setField(program, "category", category);
        ReflectionTestUtils.setField(program, "supportAmount", new BigDecimal("200000"));
        ReflectionTestUtils.setField(program, "ongoingApplication", true);
        return program;
    }
}

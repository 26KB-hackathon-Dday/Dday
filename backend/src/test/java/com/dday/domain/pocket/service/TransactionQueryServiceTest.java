package com.dday.domain.pocket.service;

import com.dday.domain.mydata.entity.ClassificationStatus;
import com.dday.domain.mydata.entity.FinancialTransaction;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.repository.CategoryRepository;
import com.dday.domain.pocket.repository.PocketRepository;
import com.dday.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceTest {

    @Mock
    private PocketRepository pocketRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private FinancialTransactionRepository transactionRepository;

    @InjectMocks
    private TransactionQueryService transactionQueryService;

    @Test
    void 포켓_거래는_월_반개구간과_필터와_페이징을_Repository에_전달한다() {
        Pocket pocket = Pocket.builder()
                .pocketType(PocketType.FREE)
                .pocketName("자유")
                .build();
        ReflectionTestUtils.setField(pocket, "pocketId", 10L);
        given(pocketRepository.findByUserUserIdAndPocketType(1L, PocketType.FREE))
                .willReturn(Optional.of(pocket));
        given(categoryRepository.existsByCategoryIdAndActiveTrue(20L)).willReturn(true);
        given(transactionRepository.findPocketPage(
                eq(1L), eq(10L), any(), any(), eq(20L),
                eq(ClassificationStatus.UNCLASSIFIED), any()))
                .willReturn(new PageImpl<>(List.of()));

        transactionQueryService.findAll(1L, PocketType.FREE, "2026-09", 20L,
                ClassificationStatus.UNCLASSIFIED, 1, 20);

        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findPocketPage(
                eq(1L), eq(10L), from.capture(), to.capture(), eq(20L),
                eq(ClassificationStatus.UNCLASSIFIED), pageable.capture());
        assertThat(from.getValue()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0));
        assertThat(to.getValue()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void 비상금_포켓의_거래를_조회할_수_있다() {
        Pocket pocket = Pocket.builder()
                .pocketType(PocketType.EMERGENCY)
                .pocketName("비상금")
                .build();
        ReflectionTestUtils.setField(pocket, "pocketId", 30L);
        given(pocketRepository.findByUserUserIdAndPocketType(1L, PocketType.EMERGENCY))
                .willReturn(Optional.of(pocket));
        given(transactionRepository.findPocketPage(
                eq(1L), eq(30L), any(), any(), isNull(), isNull(), any()))
                .willReturn(new PageImpl<>(List.of()));

        var result = transactionQueryService.findAll(
                1L, PocketType.EMERGENCY, "2026-09", null, null, 0, 20);

        assertThat(result.getContent()).isEmpty();
        verify(transactionRepository).findPocketPage(
                eq(1L), eq(30L), any(), any(), isNull(), isNull(), any());
    }

    @Test
    void 미래자산은_소비_거래_조회에서_거부한다() {
        assertThatThrownBy(() -> transactionQueryService.findAll(
                1L, PocketType.FUTURE_ASSET, "2026-09", null, null, 0, 20))
                .isInstanceOf(BusinessException.class)
                .hasMessage("거래 내역은 필수, 자유 또는 비상금 포켓만 조회할 수 있습니다.");
    }

    @Test
    void 다른_사용자의_거래는_상세에서_찾을_수_없다() {
        given(transactionRepository.findDetailByIdAndUserId(100L, 1L))
                .willReturn(Optional.<FinancialTransaction>empty());

        assertThatThrownBy(() -> transactionQueryService.findById(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("거래 정보를 찾을 수 없습니다.");
    }
}

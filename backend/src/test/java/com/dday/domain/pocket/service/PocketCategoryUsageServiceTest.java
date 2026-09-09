package com.dday.domain.pocket.service;

import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.pocket.entity.Category;
import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.repository.CategoryRepository;
import com.dday.domain.pocket.repository.PocketRepository;
import com.dday.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PocketCategoryUsageServiceTest {

    @Mock
    private PocketRepository pocketRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private FinancialTransactionRepository transactionRepository;

    @InjectMocks
    private PocketCategoryUsageService pocketCategoryUsageService;

    @Test
    void 활성_카테고리에_월_사용액을_결합하고_미분류를_별도_합산한다() {
        Pocket pocket = Pocket.builder()
                .pocketType(PocketType.ESSENTIAL)
                .pocketName("필수 포켓")
                .build();
        ReflectionTestUtils.setField(pocket, "pocketId", 10L);
        Category housing = category(1L, "HOUSING", "주거·관리비");
        Category utility = category(2L, "UTILITY", "공과금");
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 10, 1, 0, 0);

        given(pocketRepository.findByUserUserIdAndPocketType(1L, PocketType.ESSENTIAL))
                .willReturn(Optional.of(pocket));
        given(categoryRepository.findAllActive(PocketType.ESSENTIAL))
                .willReturn(List.of(housing, utility));
        given(transactionRepository.sumSpendingByCategory(1L, 10L, from, to))
                .willReturn(List.of(new Object[]{1L, 450_000L}, new Object[]{null, 20_000L}));

        var result = pocketCategoryUsageService.findMonthly(
                1L, PocketType.ESSENTIAL, "2026-09");

        assertThat(result.getMonth()).isEqualTo("2026-09");
        assertThat(result.getPocketType()).isEqualTo(PocketType.ESSENTIAL);
        assertThat(result.getCategories()).extracting(
                        item -> item.getCategoryCode(), item -> item.getUsedAmount())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("HOUSING", 450_000L),
                        org.assertj.core.groups.Tuple.tuple("UTILITY", 0L));
        assertThat(result.getUnclassifiedUsedAmount()).isEqualTo(20_000L);
        assertThat(result.getTotalUsedAmount()).isEqualTo(470_000L);
    }

    @Test
    void 비상금은_카테고리별_사용_현황을_조회할_수_없다() {
        assertThatThrownBy(() -> pocketCategoryUsageService.findMonthly(
                1L, PocketType.EMERGENCY, "2026-09"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("소비 거래는 필수 또는 자유 포켓만 조회할 수 있습니다.");
    }

    @Test
    void 잘못된_월은_포켓을_조회하기_전에_거부한다() {
        assertThatThrownBy(() -> pocketCategoryUsageService.findMonthly(
                1L, PocketType.ESSENTIAL, "2026-9"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("month는 yyyy-MM 형식이어야 합니다.");
    }

    private Category category(Long id, String code, String name) {
        Category category = Category.builder()
                .categoryCode(code)
                .categoryName(name)
                .defaultPocketType(PocketType.ESSENTIAL)
                .build();
        ReflectionTestUtils.setField(category, "categoryId", id);
        return category;
    }
}

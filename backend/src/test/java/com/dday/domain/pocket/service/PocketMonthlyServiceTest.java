package com.dday.domain.pocket.service;

import com.dday.domain.budget.entity.AllocationMethod;
import com.dday.domain.budget.entity.MonthlyBudget;
import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.budget.repository.MonthlyBudgetRepository;
import com.dday.domain.budget.repository.MonthlyPocketBudgetRepository;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.repository.PocketRepository;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PocketMonthlyServiceTest {

    @Mock
    private PocketRepository pocketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MonthlyBudgetRepository monthlyBudgetRepository;
    @Mock
    private MonthlyPocketBudgetRepository monthlyPocketBudgetRepository;
    @Mock
    private FinancialTransactionRepository transactionRepository;

    @InjectMocks
    private PocketService pocketService;

    @Test
    void 월별_포켓은_정상_소비_집계로_잔액과_사용률을_계산한다() {
        MonthlyBudget monthlyBudget = MonthlyBudget.builder()
                .budgetMonth(LocalDate.of(2026, 9, 1))
                .totalBudgetAmount(1_000_000L)
                .build();
        ReflectionTestUtils.setField(monthlyBudget, "monthlyBudgetId", 1L);
        List<MonthlyPocketBudget> budgets = List.of(
                budget(pocket(1L, PocketType.ESSENTIAL), 500_000L),
                budget(pocket(2L, PocketType.FREE), 300_000L),
                budget(pocket(3L, PocketType.EMERGENCY), 100_000L),
                budget(pocket(4L, PocketType.FUTURE_ASSET), 100_000L));
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 10, 1, 0, 0);
        given(monthlyBudgetRepository.findByUserUserIdAndBudgetMonth(1L, LocalDate.of(2026, 9, 1)))
                .willReturn(Optional.of(monthlyBudget));
        given(monthlyPocketBudgetRepository.findAllByMonthlyBudgetAndUser(1L, 1L))
                .willReturn(budgets);
        given(transactionRepository.sumSpendingByPocket(1L, from, to))
                .willReturn(List.of(new Object[]{1L, 300_000L}, new Object[]{2L, 350_000L}));

        var result = pocketService.findMonthly(1L, "2026-09");

        assertThat(result.getTotalBudgetAmount()).isEqualTo(1_000_000L);
        assertThat(result.getPockets()).extracting(summary -> summary.getPocketType())
                .containsExactly(PocketType.ESSENTIAL, PocketType.FREE,
                        PocketType.EMERGENCY, PocketType.FUTURE_ASSET);
        assertThat(result.getPockets().get(0).getRemainingAmount()).isEqualTo(200_000L);
        assertThat(result.getPockets().get(0).getUsageRate()).isEqualByComparingTo("60.00");
        assertThat(result.getPockets().get(1).getOverAmount()).isEqualTo(50_000L);
        assertThat(result.getPockets().get(2).getRemainingAmount()).isEqualTo(100_000L);
        assertThat(result.getPockets().get(3).getUsedAmount()).isNull();
    }

    @Test
    void 잘못된_월_형식은_DB를_조회하기_전에_거부한다() {
        assertThatThrownBy(() -> pocketService.findMonthly(1L, "2026-9"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("month는 yyyy-MM 형식이어야 합니다.");
    }

    private MonthlyPocketBudget budget(Pocket pocket, Long targetAmount) {
        return MonthlyPocketBudget.builder()
                .pocket(pocket)
                .targetAmount(targetAmount)
                .allocationMethod(AllocationMethod.USER_INPUT)
                .build();
    }

    private Pocket pocket(Long id, PocketType type) {
        Pocket pocket = Pocket.builder().pocketType(type).pocketName(type.name()).build();
        ReflectionTestUtils.setField(pocket, "pocketId", id);
        return pocket;
    }
}

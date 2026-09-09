package com.dday.domain.pocket.service;

import com.dday.domain.budget.entity.AllocationMethod;
import com.dday.domain.budget.entity.MonthlyBudget;
import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.budget.repository.MonthlyBudgetRepository;
import com.dday.domain.budget.repository.MonthlyPocketBudgetRepository;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
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
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MonthlyPocketSettlementServiceTest {

    @Mock
    private MonthlyBudgetRepository monthlyBudgetRepository;
    @Mock
    private MonthlyPocketBudgetRepository monthlyPocketBudgetRepository;
    @Mock
    private FinancialTransactionRepository transactionRepository;

    @InjectMocks
    private MonthlyPocketSettlementService settlementService;

    @Test
    void 필수와_자유_포켓의_월말_잔액과_초과액을_계산한다() {
        MonthlyBudget monthlyBudget = monthlyBudget();
        List<MonthlyPocketBudget> budgets = List.of(
                budget(pocket(4L, PocketType.FUTURE_ASSET), 100_000L),
                budget(pocket(2L, PocketType.FREE), 300_000L),
                budget(pocket(1L, PocketType.ESSENTIAL), 500_000L),
                budget(pocket(3L, PocketType.EMERGENCY), 100_000L));
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 10, 1, 0, 0);
        given(monthlyBudgetRepository.findByUserUserIdAndBudgetMonth(1L,
                LocalDate.of(2026, 9, 1))).willReturn(Optional.of(monthlyBudget));
        given(monthlyPocketBudgetRepository.findAllByMonthlyBudgetAndUser(10L, 1L))
                .willReturn(budgets);
        // 비상금과 미래자산 집계 행이 있어도 서비스가 정산 대상 두 포켓에만 반영해야 한다.
        given(transactionRepository.sumSpendingByPocket(1L, from, to)).willReturn(List.of(
                new Object[]{1L, 400_000L},
                new Object[]{2L, 350_000L},
                new Object[]{3L, 70_000L},
                new Object[]{4L, 20_000L}));

        var result = settlementService.settle(1L, "2026-09");

        assertThat(result.getMonth()).isEqualTo("2026-09");
        assertThat(result.getTotalTargetAmount()).isEqualTo(800_000L);
        assertThat(result.getTotalUsedAmount()).isEqualTo(750_000L);
        assertThat(result.getTotalRemainingAmount()).isEqualTo(100_000L);
        assertThat(result.getTotalOverAmount()).isEqualTo(50_000L);
        assertThat(result.getPockets()).extracting(pocket -> pocket.getPocketType())
                .containsExactly(PocketType.ESSENTIAL, PocketType.FREE);
        assertThat(result.getPockets().get(0).getRemainingAmount()).isEqualTo(100_000L);
        assertThat(result.getPockets().get(0).getOverAmount()).isZero();
        assertThat(result.getPockets().get(1).getRemainingAmount()).isZero();
        assertThat(result.getPockets().get(1).getOverAmount()).isEqualTo(50_000L);
    }

    @Test
    void 거래가_없으면_사용액은_0원이고_목표액_전체가_남는다() {
        MonthlyBudget monthlyBudget = monthlyBudget();
        given(monthlyBudgetRepository.findByUserUserIdAndBudgetMonth(1L,
                LocalDate.of(2026, 9, 1))).willReturn(Optional.of(monthlyBudget));
        given(monthlyPocketBudgetRepository.findAllByMonthlyBudgetAndUser(10L, 1L))
                .willReturn(List.of(
                        budget(pocket(1L, PocketType.ESSENTIAL), 500_000L),
                        budget(pocket(2L, PocketType.FREE), 300_000L)));
        given(transactionRepository.sumSpendingByPocket(1L,
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 10, 1, 0, 0))).willReturn(List.of());

        var result = settlementService.settle(1L, "2026-09");

        assertThat(result.getTotalUsedAmount()).isZero();
        assertThat(result.getTotalRemainingAmount()).isEqualTo(800_000L);
        assertThat(result.getTotalOverAmount()).isZero();
    }

    @Test
    void 필수나_자유_포켓_예산이_빠지면_정산하지_않는다() {
        MonthlyBudget monthlyBudget = monthlyBudget();
        given(monthlyBudgetRepository.findByUserUserIdAndBudgetMonth(1L,
                LocalDate.of(2026, 9, 1))).willReturn(Optional.of(monthlyBudget));
        given(monthlyPocketBudgetRepository.findAllByMonthlyBudgetAndUser(10L, 1L))
                .willReturn(List.of(budget(pocket(1L, PocketType.ESSENTIAL), 500_000L)));

        assertThatThrownBy(() -> settlementService.settle(1L, "2026-09"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 월의 포켓 예산을 찾을 수 없습니다.");
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void 잘못된_월_형식은_조회하기_전에_거부한다() {
        assertThatThrownBy(() -> settlementService.settle(1L, "2026-9"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("month는 yyyy-MM 형식이어야 합니다.");
        verifyNoInteractions(monthlyBudgetRepository, monthlyPocketBudgetRepository,
                transactionRepository);
    }

    private MonthlyBudget monthlyBudget() {
        MonthlyBudget budget = MonthlyBudget.builder()
                .budgetMonth(LocalDate.of(2026, 9, 1))
                .totalBudgetAmount(1_000_000L)
                .build();
        ReflectionTestUtils.setField(budget, "monthlyBudgetId", 10L);
        return budget;
    }

    private MonthlyPocketBudget budget(Pocket pocket, Long targetAmount) {
        return MonthlyPocketBudget.builder()
                .pocket(pocket)
                .targetAmount(targetAmount)
                .allocationMethod(AllocationMethod.USER_INPUT)
                .build();
    }

    private Pocket pocket(Long id, PocketType pocketType) {
        Pocket pocket = Pocket.builder()
                .pocketType(pocketType)
                .pocketName(pocketType.name())
                .build();
        ReflectionTestUtils.setField(pocket, "pocketId", id);
        return pocket;
    }
}

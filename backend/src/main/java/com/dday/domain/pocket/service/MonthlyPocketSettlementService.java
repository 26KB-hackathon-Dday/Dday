package com.dday.domain.pocket.service;

import com.dday.domain.budget.dto.BudgetErrorCode;
import com.dday.domain.budget.entity.MonthlyBudget;
import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.budget.repository.MonthlyBudgetRepository;
import com.dday.domain.budget.repository.MonthlyPocketBudgetRepository;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.pocket.dto.PocketErrorCode;
import com.dday.domain.pocket.dto.response.MonthlyPocketSettlementResponse;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 월말 시점의 소비 포켓 정산 결과를 계산한다.
 *
 * <p>정산 결과를 별도로 저장하지 않고 확정된 월 예산과 거래 원장을 매 요청마다 다시 집계한다.
 * 같은 사용자와 같은 월로 여러 번 요청해도 원본 데이터를 변경하지 않으므로 결과가 중복 저장되지
 * 않는다. 거래 취소나 재분류가 반영되면 다음 호출에서 최신 원장 기준 결과를 받을 수 있다.
 */
@Service
@RequiredArgsConstructor
public class MonthlyPocketSettlementService {

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM").withResolverStyle(ResolverStyle.STRICT);
    private static final Set<PocketType> SETTLEMENT_TARGETS =
            EnumSet.of(PocketType.ESSENTIAL, PocketType.FREE);

    private final MonthlyBudgetRepository monthlyBudgetRepository;
    private final MonthlyPocketBudgetRepository monthlyPocketBudgetRepository;
    private final FinancialTransactionRepository transactionRepository;

    /**
     * 필수·자유 포켓의 목표액, 사용액, 잔액과 초과액을 월 단위로 계산한다.
     *
     * <p>미래자산과 비상금은 이 정산의 대상이 아니다. 월 경계는 {@code [월 시작, 다음 달 시작)}
     * 반개구간으로 잡아 월별 일수, 윤년, 초·밀리초 정밀도 차이에도 마지막 거래가 빠지지 않게 한다.
     */
    @Transactional(readOnly = true)
    public MonthlyPocketSettlementResponse settle(Long userId, String month) {
        YearMonth yearMonth = parseMonth(month);
        MonthlyBudget monthlyBudget = monthlyBudgetRepository
                .findByUserUserIdAndBudgetMonth(userId, yearMonth.atDay(1))
                .orElseThrow(() -> new BusinessException(BudgetErrorCode.MONTHLY_BUDGET_NOT_FOUND));

        List<MonthlyPocketBudget> targets = monthlyPocketBudgetRepository
                .findAllByMonthlyBudgetAndUser(monthlyBudget.getMonthlyBudgetId(), userId).stream()
                .filter(budget -> SETTLEMENT_TARGETS.contains(budget.getPocket().getPocketType()))
                .sorted(Comparator.comparing(budget -> budget.getPocket().getPocketType().ordinal()))
                .toList();
        // 필수 또는 자유 포켓 예산이 하나라도 없으면 일부 결과만 정산 완료처럼 반환하지 않는다.
        if (targets.size() != SETTLEMENT_TARGETS.size()) {
            throw new BusinessException(BudgetErrorCode.MONTHLY_POCKET_BUDGET_NOT_FOUND);
        }

        LocalDateTime from = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime to = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        Map<Long, Long> spendingByPocket = new HashMap<>();
        transactionRepository.sumSpendingByPocket(userId, from, to)
                .forEach(row -> spendingByPocket.put(
                        (Long) row[0], ((Number) row[1]).longValue()));

        List<MonthlyPocketSettlementResponse.PocketSettlement> settlements = targets.stream()
                .map(budget -> toSettlement(budget,
                        spendingByPocket.getOrDefault(budget.getPocket().getPocketId(), 0L)))
                .toList();

        return MonthlyPocketSettlementResponse.builder()
                .month(yearMonth.toString())
                .totalTargetAmount(sum(settlements, AmountType.TARGET))
                .totalUsedAmount(sum(settlements, AmountType.USED))
                .totalRemainingAmount(sum(settlements, AmountType.REMAINING))
                .totalOverAmount(sum(settlements, AmountType.OVER))
                .pockets(settlements)
                .build();
    }

    private MonthlyPocketSettlementResponse.PocketSettlement toSettlement(
            MonthlyPocketBudget budget, Long usedAmount) {
        Long targetAmount = budget.getTargetAmount();
        return MonthlyPocketSettlementResponse.PocketSettlement.builder()
                .pocketId(budget.getPocket().getPocketId())
                .pocketType(budget.getPocket().getPocketType())
                .pocketName(budget.getPocket().getPocketName())
                .targetAmount(targetAmount)
                .usedAmount(usedAmount)
                .remainingAmount(Math.max(targetAmount - usedAmount, 0L))
                .overAmount(Math.max(usedAmount - targetAmount, 0L))
                .build();
    }

    private Long sum(List<MonthlyPocketSettlementResponse.PocketSettlement> settlements,
                     AmountType amountType) {
        return settlements.stream().mapToLong(settlement -> switch (amountType) {
            case TARGET -> settlement.getTargetAmount();
            case USED -> settlement.getUsedAmount();
            case REMAINING -> settlement.getRemainingAmount();
            case OVER -> settlement.getOverAmount();
        }).sum();
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month, MONTH_FORMATTER);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new BusinessException(PocketErrorCode.INVALID_MONTH);
        }
    }

    private enum AmountType {
        TARGET, USED, REMAINING, OVER
    }
}

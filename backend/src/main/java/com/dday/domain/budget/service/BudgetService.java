package com.dday.domain.budget.service;

import com.dday.domain.budget.dto.BudgetErrorCode;
import com.dday.domain.budget.dto.request.MonthlyBudgetConfirmRequest;
import com.dday.domain.budget.dto.response.MonthlyBudgetConfirmResponse;
import com.dday.domain.budget.entity.AllocationMethod;
import com.dday.domain.budget.entity.MonthlyBudget;
import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.budget.repository.MonthlyBudgetRepository;
import com.dday.domain.budget.repository.MonthlyPocketBudgetRepository;
import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.repository.PocketRepository;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {
    private final UserRepository userRepository;
    private final PocketRepository pocketRepository;
    private final MonthlyBudgetRepository monthlyBudgetRepository;
    private final MonthlyPocketBudgetRepository monthlyPocketBudgetRepository;

    @Transactional
    public MonthlyBudgetConfirmResponse confirmCurrent(Long userId, MonthlyBudgetConfirmRequest request) {
        LocalDate budgetMonth = LocalDate.now().withDayOfMonth(1);
        if (monthlyBudgetRepository.findByUserUserIdAndBudgetMonth(userId, budgetMonth).isPresent()) {
            throw new BusinessException(BudgetErrorCode.MONTHLY_BUDGET_ALREADY_EXISTS);
        }

        var requestedTypes = new HashSet<PocketType>();
        long allocatedTotal = 0L;
        for (var pocket : request.getPockets()) {
            if (!requestedTypes.add(pocket.getPocketType())) {
                throw new BusinessException(BudgetErrorCode.INVALID_POCKET_BUDGETS);
            }
            allocatedTotal = Math.addExact(allocatedTotal, pocket.getAmount());
        }
        if (requestedTypes.size() != PocketType.values().length) {
            throw new BusinessException(BudgetErrorCode.INVALID_POCKET_BUDGETS);
        }
        if (allocatedTotal != request.getTotalBudgetAmount()) {
            throw new BusinessException(BudgetErrorCode.BUDGET_TOTAL_MISMATCH);
        }

        User user = userRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(BudgetErrorCode.POCKETS_NOT_INITIALIZED));
        List<Pocket> pockets = pocketRepository.findAllByUserUserId(userId);
        if (pockets.size() != PocketType.values().length) {
            throw new BusinessException(BudgetErrorCode.POCKETS_NOT_INITIALIZED);
        }
        var pocketByType = new EnumMap<PocketType, Pocket>(PocketType.class);
        pockets.forEach(pocket -> pocketByType.put(pocket.getPocketType(), pocket));

        MonthlyBudget monthlyBudget = monthlyBudgetRepository.save(MonthlyBudget.builder()
                .user(user)
                .budgetMonth(budgetMonth)
                .totalBudgetAmount(request.getTotalBudgetAmount())
                .draftSummary("사용자가 직접 확정한 월 예산")
                .draftCreatedAt(LocalDateTime.now())
                .build());
        monthlyBudget.confirm(LocalDateTime.now());

        List<MonthlyPocketBudget> savedPockets = request.getPockets().stream()
                .map(item -> MonthlyPocketBudget.builder()
                        .monthlyBudget(monthlyBudget)
                        .pocket(pocketByType.get(item.getPocketType()))
                        .targetAmount(item.getAmount())
                        .allocationMethod(AllocationMethod.USER_INPUT)
                        .build())
                .toList();
        monthlyPocketBudgetRepository.saveAll(savedPockets);

        return MonthlyBudgetConfirmResponse.builder()
                .monthlyBudgetId(monthlyBudget.getMonthlyBudgetId())
                .month(budgetMonth.toString().substring(0, 7))
                .totalBudgetAmount(monthlyBudget.getTotalBudgetAmount())
                .pockets(savedPockets.stream().map(item -> MonthlyBudgetConfirmResponse.PocketBudgetResponse.builder()
                        .pocketType(item.getPocket().getPocketType().name())
                        .targetAmount(item.getTargetAmount())
                        .build()).toList())
                .build();
    }
}

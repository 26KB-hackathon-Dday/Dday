package com.dday.domain.pocket.service;

import com.dday.domain.budget.dto.BudgetErrorCode;
import com.dday.domain.budget.entity.MonthlyBudget;
import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.budget.repository.MonthlyBudgetRepository;
import com.dday.domain.budget.repository.MonthlyPocketBudgetRepository;
import com.dday.domain.pocket.dto.PocketErrorCode;
import com.dday.domain.pocket.dto.request.PocketUpdateRequest;
import com.dday.domain.pocket.dto.response.PocketInitializeResponse;
import com.dday.domain.pocket.dto.response.PocketMonthlyResponse;
import com.dday.domain.pocket.dto.response.PocketResponse;
import com.dday.domain.pocket.dto.response.PocketSummaryResponse;
import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.repository.PocketRepository;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.user.dto.UserErrorCode;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 포켓의 초기 상태와 월별 사용 현황을 조회하는 애플리케이션 서비스다.
 *
 * <p>포켓 자체의 생명주기와 월 예산({@link MonthlyBudget}), 포켓별 배정 예산
 * ({@link MonthlyPocketBudget}), 실제 거래 합계를 한 응답으로 조합한다. 원 단위 금액은
 * 정수이므로 {@link Long}을 유지하고, 나눗셈이 필요한 사용률만 {@link BigDecimal}로 계산한다.
 *
 * <p>인터페이스를 두지 않는다 — 구현체가 하나뿐인데 인터페이스를 만들면 파일만 두 배가 된다
 * (AGENTS.md §7).
 */
@Service
@RequiredArgsConstructor
public class PocketService {

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM").withResolverStyle(ResolverStyle.STRICT);

    private final PocketRepository pocketRepository;
    private final UserRepository userRepository;
    private final MonthlyBudgetRepository monthlyBudgetRepository;
    private final MonthlyPocketBudgetRepository monthlyPocketBudgetRepository;
    private final FinancialTransactionRepository transactionRepository;

    /**
     * 사용자에게 누락된 기본 포켓만 생성하고 실제 생성 건수를 반환한다.
     *
     * <p>사전 조회는 정상적인 재호출에서 INSERT를 줄이고, 최종 중복 방지는 DB 유일 제약과
     * {@code insert ignore}가 담당한다. 따라서 동시에 두 요청이 들어와도 합계에는 실제로
     * 삽입된 행만 반영된다.
     */
    @Transactional
    public PocketInitializeResponse initialize(Long userId) {
        // 탈퇴 회원이나 존재하지 않는 회원에게 포켓 행이 생기지 않도록 먼저 활성 상태를 검증한다.
        User user = userRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // EnumMap은 PocketType 선언 순서를 유지하므로 초기 생성 순서도 화면의 기본 순서와 같다.
        Map<PocketType, String> names = new EnumMap<>(PocketType.class);
        names.put(PocketType.ESSENTIAL, "필수 포켓");
        names.put(PocketType.FREE, "자유 포켓");
        names.put(PocketType.EMERGENCY, "비상금 포켓");
        names.put(PocketType.FUTURE_ASSET, "미래자산 포켓");
        pocketRepository.findAllByUserUserId(user.getUserId())
                .forEach(pocket -> names.remove(pocket.getPocketType()));

        // 사전 조회와 삽입 사이에 동시 요청이 들어와도 DB의 유일 제약 + INSERT IGNORE가 중복을 막는다.
        int createdCount = names.entrySet().stream()
                .mapToInt(entry -> pocketRepository.insertIfAbsent(
                        user.getUserId(), entry.getKey().name(), entry.getValue()))
                .sum();
        return PocketInitializeResponse.builder()
                .createdCount(createdCount)
                .totalCount(PocketType.values().length)
                .build();
    }

    /**
     * 요청한 달의 포켓별 목표액과 실제 사용액을 결합해 월간 현황을 만든다.
     *
     * <p>{@code month}는 {@code yyyy-MM}만 허용한다. 거래 기간은 해당 월의 첫 시각 이상,
     * 다음 달 첫 시각 미만인 반개구간으로 계산해 월말의 초·밀리초 정밀도에 의존하지 않는다.
     * 월별 포켓 예산 네 개가 모두 준비되지 않은 상태는 불완전한 예산으로 간주한다.
     */
    @Transactional(readOnly = true)
    public PocketMonthlyResponse findMonthly(Long userId, String month) {
        YearMonth yearMonth = parseMonth(month);
        MonthlyBudget monthlyBudget = monthlyBudgetRepository
                .findByUserUserIdAndBudgetMonth(userId, yearMonth.atDay(1))
                .orElseThrow(() -> new BusinessException(BudgetErrorCode.MONTHLY_BUDGET_NOT_FOUND));
        List<MonthlyPocketBudget> budgets = monthlyPocketBudgetRepository
                .findAllByMonthlyBudgetAndUser(monthlyBudget.getMonthlyBudgetId(), userId);
        if (budgets.size() != PocketType.values().length) {
            throw new BusinessException(BudgetErrorCode.MONTHLY_POCKET_BUDGET_NOT_FOUND);
        }

        // [월 시작, 다음 달 시작) 범위로 조회하면 윤년과 월별 일수 차이를 별도로 처리할 필요가 없다.
        LocalDateTime from = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime to = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        Map<Long, Long> spending = new HashMap<>();
        // 거래 전체를 메모리로 가져오지 않고 DB에서 포켓별 합계를 계산한다.
        transactionRepository.sumSpendingByPocket(userId, from, to)
                .forEach(row -> spending.put((Long) row[0], ((Number) row[1]).longValue()));

        List<PocketSummaryResponse> pockets = budgets.stream()
                .sorted(Comparator.comparing(budget -> budget.getPocket().getPocketType().ordinal()))
                .map(budget -> summary(budget,
                        spending.getOrDefault(budget.getPocket().getPocketId(), 0L)))
                .toList();
        return PocketMonthlyResponse.builder()
                .month(yearMonth.toString())
                .totalBudgetAmount(monthlyBudget.getTotalBudgetAmount())
                .pockets(pockets)
                .build();
    }

    /**
     * 내 포켓 목록.
     *
     * <p>정렬은 DB가 아니라 여기서 한다. {@code pocket_type}이 문자열로 저장돼 있어 DB 정렬은
     * 알파벳순(EMERGENCY, ESSENTIAL, FREE, FUTURE_ASSET)이 되는데, 화면에 필요한 건
     * <b>필수 → 자유 → 비상금 → 미래자산</b>이라는 우선순위이기 때문이다.
     * 그 순서는 {@code PocketType}의 선언 순서가 들고 있다.
     */
    @Transactional(readOnly = true)
    public List<PocketResponse> findAll(Long userId) {
        return pocketRepository.findAllByUserUserId(userId).stream()
                .sorted(Comparator.comparing(pocket -> pocket.getPocketType().ordinal()))
                .map(PocketResponse::from)
                .toList();
    }

    /** 남의 포켓은 조회되지 않는다. 소유자가 다르면 "없음"으로 답한다 (존재 여부를 흘리지 않는다). */
    @Transactional(readOnly = true)
    public PocketResponse findById(Long userId, Long pocketId) {
        Pocket pocket = pocketRepository.findByPocketIdAndUserUserId(pocketId, userId)
                .orElseThrow(() -> new BusinessException(PocketErrorCode.POCKET_NOT_FOUND));
        return PocketResponse.from(pocket);
    }

    /**
     * 필수·자유·비상금 포켓의 화면 표시 정보만 부분 수정한다.
     *
     * <p>조회 조건에 사용자 ID를 함께 넣어 남의 포켓과 존재하지 않는 포켓을 모두 같은 404로
     * 처리한다. 포켓 유형과 금액은 이 API의 입력에 없으므로 고정 구조와 월별 예산에 영향을 주지 않는다.
     */
    @Transactional
    public PocketResponse update(Long userId, Long pocketId, PocketUpdateRequest request) {
        Pocket pocket = pocketRepository.findByPocketIdAndUserUserId(pocketId, userId)
                .orElseThrow(() -> new BusinessException(PocketErrorCode.POCKET_NOT_FOUND));
        if (pocket.getPocketType() == PocketType.FUTURE_ASSET) {
            throw new BusinessException(PocketErrorCode.POCKET_UPDATE_NOT_ALLOWED);
        }
        if (request.getPocketName() == null && request.getDescription() == null) {
            throw new BusinessException(PocketErrorCode.EMPTY_POCKET_UPDATE);
        }

        String pocketName = request.getPocketName();
        if (pocketName != null) {
            pocketName = pocketName.trim();
            if (pocketName.isEmpty()) {
                throw new BusinessException(PocketErrorCode.INVALID_POCKET_NAME);
            }
        }
        // 설명도 화면에 그대로 표시되므로 앞뒤 공백만 제거하고 내부 줄바꿈과 문장은 유지한다.
        String description = request.getDescription() == null
                ? null : request.getDescription().trim();
        pocket.rename(pocketName, description);
        return PocketResponse.from(pocket);
    }

    private PocketSummaryResponse summary(MonthlyPocketBudget budget, Long usedAmount) {
        Pocket pocket = budget.getPocket();
        Long targetAmount = budget.getTargetAmount();
        PocketSummaryResponse.PocketSummaryResponseBuilder response = PocketSummaryResponse.builder()
                .pocketId(pocket.getPocketId())
                .pocketType(pocket.getPocketType())
                .pocketName(pocket.getPocketName())
                .targetAmount(targetAmount);
        if (pocket.getPocketType() == PocketType.ESSENTIAL
                || pocket.getPocketType() == PocketType.FREE
                || pocket.getPocketType() == PocketType.EMERGENCY) {
            // 소비가 연결될 수 있는 포켓은 실제 정상 지출을 기준으로 사용액을 계산한다.
            // 잔액과 초과액은 동시에 양수가 되지 않도록 각각 0을 하한으로 둔다.
            response.usedAmount(usedAmount)
                    .remainingAmount(Math.max(targetAmount - usedAmount, 0L))
                    .overAmount(Math.max(usedAmount - targetAmount, 0L))
                    .usageRate(usageRate(targetAmount, usedAmount));
        }
        // 미래자산 포켓은 이번 조회에서 목표액만 제공하며 소비성 파생 값은 비워 둔다.
        return response.build();
    }

    /** 목표액 대비 사용액을 백분율로 환산한다. 0원 목표는 0으로 정의해 0 나눗셈을 피한다. */
    private BigDecimal usageRate(Long targetAmount, Long usedAmount) {
        if (targetAmount == 0L) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(usedAmount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(targetAmount), 2, RoundingMode.HALF_UP);
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month, MONTH_FORMATTER);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessException(PocketErrorCode.INVALID_MONTH);
        }
    }
}

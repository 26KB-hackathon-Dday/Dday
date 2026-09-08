package com.dday.domain.onboarding.service;

import com.dday.domain.housing.entity.HousingCost;
import com.dday.domain.housing.repository.HousingCostRepository;
import com.dday.domain.income.entity.RecurringIncome;
import com.dday.domain.income.repository.RecurringIncomeRepository;
import com.dday.domain.onboarding.dto.OnboardingErrorCode;
import com.dday.domain.onboarding.dto.request.*;
import com.dday.domain.onboarding.dto.response.*;
import com.dday.domain.user.entity.HousingType;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 온보딩 흐름 전체.
 *
 * <p><b>화면을 하나 넘길 때마다 저장한다.</b> 마지막에 몰아서 저장하면 중간에 이탈했을 때
 * 입력이 통째로 날아간다. 그래서 단계마다 API가 하나씩 있고, {@code User}도 단계별
 * 도메인 메서드로 나뉘어 있다.
 *
 * <p><b>회원은 항상 {@code userId} + ACTIVE로 찾는다.</b> 토큰은 탈퇴 후에도 만료 전까지
 * 서명이 유효해서, 상태를 걸지 않으면 탈퇴한 회원이 온보딩을 이어갈 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    /**
     * 주거비 입력을 건너뛰는 주거형태.
     *
     * <p>시설에서 보호 중이면 본인이 내는 주거비가 없다. <b>현재 enum에는 시설 유형이 없어
     * 비어 있다</b> — {@code IN_CARE_FACILITY}·{@code FOSTER_HOME}·{@code GROUP_HOME}이
     * {@link HousingType}에 추가되면 여기에만 넣으면 화면 분기가 따라온다.
     */
    private static final Set<HousingType> HOUSING_COST_SKIPPED = Set.of();

    private final UserRepository userRepository;
    private final RecurringIncomeRepository recurringIncomeRepository;
    private final HousingCostRepository housingCostRepository;

    // ── 진행 상태 ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public OnboardingProgressResponse getProgress(Long userId) {
        return OnboardingProgressResponse.of(getActiveUser(userId).isOnboardingCompleted());
    }

    // ── 1단계: 보호종료일 ────────────────────────────────────────────────────

    /** 저장하고 곧바로 D-day를 계산해 돌려준다. 화면이 저장 직후 결과를 보여주기 때문이다. */
    @Transactional
    public ProtectionDateResponse saveProtectionDate(Long userId, ProtectionDateRequest request) {
        User user = getActiveUser(userId);
        user.updateProtectionEndDate(request.getProtectionEndDate());

        return ProtectionDateResponse.of(request.getProtectionEndDate(), LocalDate.now());
    }

    // ── 2단계: 거주지역 ──────────────────────────────────────────────────────

    @Transactional
    public void saveRegion(Long userId, RegionRequest request) {
        getActiveUser(userId).updateRegion(
                request.getRegionCode(), request.getRegionName(), request.getDistrictName());
    }

    // ── 3단계: 주거형태 ──────────────────────────────────────────────────────

    @Transactional
    public HousingTypeResponse saveHousingType(Long userId, HousingTypeRequest request) {
        HousingType housingType = request.getHousingType();
        getActiveUser(userId).updateHousingType(housingType);

        return HousingTypeResponse.of(housingType, skipsHousingCost(housingType));
    }

    /** 주거비를 물어볼 필요가 없는 주거형태인지. 분기 이유는 {@link #HOUSING_COST_SKIPPED} 참고. */
    private boolean skipsHousingCost(HousingType housingType) {
        return HOUSING_COST_SKIPPED.contains(housingType);
    }

    // ── 4단계: 주거비 ────────────────────────────────────────────────────────

    /**
     * 주거비 upsert. 회원당 한 행이라 있으면 갱신, 없으면 만든다.
     *
     * <p>세 값을 통째로 덮어쓴다 — {@code null}을 건너뛰면 "월세 있음 → 없음"으로 바뀌었을 때
     * 옛 월세가 남는다. 화면이 세 값을 항상 함께 보내는 전제다.
     */
    @Transactional
    public HousingCostSaveResponse saveHousingCost(Long userId, HousingCostRequest request) {
        User user = getActiveUser(userId);

        HousingCost housingCost = housingCostRepository.findById(userId)
                .orElseGet(() -> housingCostRepository.save(
                        HousingCost.builder().user(user).build()));

        housingCost.update(request.getDeposit(), request.getMonthlyRent(), request.getMaintenanceFee());

        return HousingCostSaveResponse.of(housingCost.estimatedMonthly());
    }

    // ── 5단계: 정기수입 ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public IncomeListResponse getIncomes(Long userId) {
        List<RecurringIncome> incomes = findIncomes(userId);
        return IncomeListResponse.of(incomes, sum(incomes));
    }

    @Transactional
    public IncomeSaveResponse addIncome(Long userId, IncomeRequest request) {
        User user = getActiveUser(userId);

        RecurringIncome saved = recurringIncomeRepository.save(RecurringIncome.builder()
                .user(user)
                .incomeName(request.getName())
                .incomeType(request.getIncomeType())
                .expectedAmount(request.getAmount())
                .depositTiming(request.getPaymentTiming())
                .autoMatchEnabled(false)
                .build());

        return IncomeSaveResponse.of(saved.getRecurringIncomeId(), sum(findIncomes(userId)));
    }

    /**
     * 삭제. <b>조회 단계에서 소유자를 함께 건다</b> — id만으로 찾아 뒤에서 검사하면
     * 검사를 빠뜨린 코드가 남의 데이터를 지운다.
     *
     * <p>남의 수입이든 없는 id든 같은 404를 준다. 구분해 주면 어떤 id가 존재하는지 알려주는 꼴이다.
     */
    @Transactional
    public IncomeDeleteResponse deleteIncome(Long userId, Long incomeId) {
        RecurringIncome income = recurringIncomeRepository
                .findByRecurringIncomeIdAndUserUserId(incomeId, userId)
                .orElseThrow(() -> new BusinessException(OnboardingErrorCode.INCOME_NOT_FOUND));

        recurringIncomeRepository.delete(income);
        recurringIncomeRepository.flush();   // 삭제를 반영해야 아래 합계가 맞는다

        return IncomeDeleteResponse.of(sum(findIncomes(userId)));
    }

    // ── 6단계: 자산 ──────────────────────────────────────────────────────────

    @Transactional
    public void saveAssets(Long userId, AssetRequest request) {
        getActiveUser(userId).updateAssets(request.getTotalSaved(), request.getSettlementReceived());
    }

    // ── 완료 ─────────────────────────────────────────────────────────────────

    /**
     * 온보딩을 마치고 첫 자립계획 요약을 돌려준다.
     *
     * <p>보호종료일을 건너뛴 회원도 완료할 수 있다 — 그 경우 {@code dDay}가 {@code null}이다.
     * 여기서 막으면 날짜를 모르는 사용자가 온보딩을 끝내지 못한다.
     */
    @Transactional
    public OnboardingCompleteResponse complete(Long userId) {
        User user = getActiveUser(userId);
        user.completeOnboarding();

        // TODO: 예산 도메인이 준비되면 첫 예산 초안을 만든다.
        //       budgetService.createDraftBudget(userId);
        //       초안이 생기면 아래 monthlyExpense에 초안의 고정지출 합계를 더해야 한다.

        Long dDay = user.getProtectionEndDate() == null
                ? null
                : OnboardingCalculator.dDay(user.getProtectionEndDate(), LocalDate.now());

        long monthlyIncome = sum(findIncomes(userId));

        // 주거비를 입력하지 않았거나(행 없음) 월세·관리비가 모두 비면(estimatedMonthly == null) 0으로 본다.
        // 응답의 monthlyExpense는 합계라 null을 둘 자리가 없다.
        long monthlyExpense = nullToZero(housingCostRepository.findById(userId)
                .map(HousingCost::estimatedMonthly)
                .orElse(null));

        log.info("온보딩 완료: userId={}, dDay={}", userId, dDay);

        return OnboardingCompleteResponse.of(dDay, monthlyIncome, monthlyExpense);
    }

    // ── 공통 ─────────────────────────────────────────────────────────────────

    private List<RecurringIncome> findIncomes(Long userId) {
        return recurringIncomeRepository.findAllByUserUserIdOrderByRecurringIncomeIdAsc(userId);
    }

    private long sum(List<RecurringIncome> incomes) {
        return incomes.stream()
                .map(RecurringIncome::getExpectedAmount)
                .mapToLong(OnboardingService::nullToZero)
                .sum();
    }

    private static long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private User getActiveUser(Long userId) {
        return userRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(OnboardingErrorCode.USER_NOT_FOUND));
    }
}

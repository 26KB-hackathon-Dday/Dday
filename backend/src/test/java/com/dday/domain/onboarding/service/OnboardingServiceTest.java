package com.dday.domain.onboarding.service;

import com.dday.domain.housing.entity.HousingCost;
import com.dday.domain.housing.repository.HousingCostRepository;
import com.dday.domain.income.entity.IncomeType;
import com.dday.domain.income.entity.RecurringIncome;
import com.dday.domain.income.repository.RecurringIncomeRepository;
import com.dday.domain.onboarding.dto.OnboardingErrorCode;
import com.dday.domain.onboarding.dto.request.*;
import com.dday.domain.onboarding.dto.response.ProtectionStatus;
import com.dday.domain.user.entity.HousingType;
import com.dday.domain.user.entity.SettlementReceived;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import com.dday.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OnboardingServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RecurringIncomeRepository recurringIncomeRepository;
    @Mock
    private HousingCostRepository housingCostRepository;

    private OnboardingService onboardingService;
    private User user;

    @BeforeEach
    void setUp() {
        onboardingService = new OnboardingService(
                userRepository, recurringIncomeRepository, housingCostRepository);

        user = User.builder()
                .email("onboard@dday.com")
                .passwordHash("$2a$10$" + "x".repeat(53))
                .name("테스터")
                .phone("010-0000-0000")
                .termsAgreedAt(LocalDateTime.now())
                .agreedLocation(false)
                .build();
        ReflectionTestUtils.setField(user, "userId", USER_ID);

        given(userRepository.findByUserIdAndStatus(USER_ID, UserStatus.ACTIVE))
                .willReturn(Optional.of(user));
        given(recurringIncomeRepository.findAllByUserUserIdOrderByRecurringIncomeIdAsc(USER_ID))
                .willReturn(List.of());
        given(housingCostRepository.findById(USER_ID)).willReturn(Optional.empty());
    }

    private static ErrorCode errorCodeOf(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    private RecurringIncome income(long id, String name, long amount) {
        RecurringIncome i = RecurringIncome.builder()
                .user(user).incomeName(name).incomeType(IncomeType.SALARY)
                .expectedAmount(amount).autoMatchEnabled(false)
                .build();
        ReflectionTestUtils.setField(i, "recurringIncomeId", id);
        return i;
    }

    // ── 회원 확인 ────────────────────────────────────────────────────────────

    @Test
    void 탈퇴한_회원은_온보딩을_이어갈_수_없다() {
        // 토큰은 탈퇴 후에도 만료 전까지 서명이 유효하다. 상태를 걸지 않으면 통과해버린다.
        given(userRepository.findByUserIdAndStatus(USER_ID, UserStatus.ACTIVE))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.getProgress(USER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(OnboardingErrorCode.USER_NOT_FOUND));
    }

    // ── 보호종료일 ───────────────────────────────────────────────────────────

    @Test
    void 보호종료일을_저장하면_지원_종료일과_상태가_함께_온다() {
        var request = new ProtectionDateRequest();
        ReflectionTestUtils.setField(request, "protectionEndDate", LocalDate.now().minusYears(1));

        var response = onboardingService.saveProtectionDate(USER_ID, request);

        assertThat(user.getProtectionEndDate()).isEqualTo(LocalDate.now().minusYears(1));
        assertThat(response.getSupportEndDate()).isEqualTo(LocalDate.now().plusYears(4));
        assertThat(response.getProtectionStatus()).isEqualTo(ProtectionStatus.DISCHARGED);
        assertThat(response.getDDay()).isPositive();
    }

    @Test
    void 보호_예정일이_미래면_보호중으로_나온다() {
        var request = new ProtectionDateRequest();
        ReflectionTestUtils.setField(request, "protectionEndDate", LocalDate.now().plusMonths(3));

        assertThat(onboardingService.saveProtectionDate(USER_ID, request).getProtectionStatus())
                .isEqualTo(ProtectionStatus.IN_PROTECTION);
    }

    // ── 거주지역 · 주거형태 ──────────────────────────────────────────────────

    @Test
    void 거주지역은_코드와_이름이_함께_저장된다() {
        var request = new RegionRequest();
        ReflectionTestUtils.setField(request, "regionCode", "1168000000");
        ReflectionTestUtils.setField(request, "regionName", "서울특별시");
        ReflectionTestUtils.setField(request, "districtName", "강남구");

        onboardingService.saveRegion(USER_ID, request);

        assertThat(user.getRegionCode()).isEqualTo("1168000000");
        assertThat(user.getRegionName()).isEqualTo("서울특별시");
        assertThat(user.getDistrictName()).isEqualTo("강남구");
    }

    @Test
    void 현재_주거형태는_모두_주거비를_입력받는다() {
        // 시설 보호 유형이 enum에 없어 skipHousingCost는 항상 false다.
        for (HousingType type : HousingType.values()) {
            var request = new HousingTypeRequest();
            ReflectionTestUtils.setField(request, "housingType", type);

            var response = onboardingService.saveHousingType(USER_ID, request);

            assertThat(response.getHousingType()).isEqualTo(type);
            assertThat(response.isSkipHousingCost()).isFalse();
        }
    }

    // ── 주거비 ───────────────────────────────────────────────────────────────

    @Test
    void 주거비가_없으면_새로_만들고_월_예상_주거비를_계산한다() {
        given(housingCostRepository.save(any())).willAnswer(i -> i.getArgument(0));

        var request = new HousingCostRequest();
        ReflectionTestUtils.setField(request, "deposit", 10_000_000L);
        ReflectionTestUtils.setField(request, "monthlyRent", 450_000L);
        ReflectionTestUtils.setField(request, "maintenanceFee", 70_000L);

        var response = onboardingService.saveHousingCost(USER_ID, request);

        assertThat(response.getEstimatedMonthly()).isEqualTo(520_000L);
    }

    @Test
    void 주거비가_이미_있으면_새로_만들지_않고_덮어쓴다() {
        HousingCost existing = HousingCost.builder()
                .user(user).deposit(1L).monthlyRent(300_000L).maintenanceFee(50_000L).build();
        given(housingCostRepository.findById(USER_ID)).willReturn(Optional.of(existing));

        var request = new HousingCostRequest();
        ReflectionTestUtils.setField(request, "deposit", 20_000_000L);
        ReflectionTestUtils.setField(request, "monthlyRent", 500_000L);
        ReflectionTestUtils.setField(request, "maintenanceFee", 60_000L);

        var response = onboardingService.saveHousingCost(USER_ID, request);

        verify(housingCostRepository, never()).save(any());
        assertThat(existing.getDeposit()).isEqualTo(20_000_000L);
        assertThat(response.getEstimatedMonthly()).isEqualTo(560_000L);
    }

    // ── 정기수입 ─────────────────────────────────────────────────────────────

    @Test
    void 수입_목록은_합계와_함께_온다() {
        given(recurringIncomeRepository.findAllByUserUserIdOrderByRecurringIncomeIdAsc(USER_ID))
                .willReturn(List.of(income(1L, "알바", 500_000L), income(2L, "자립수당", 300_000L)));

        var response = onboardingService.getIncomes(USER_ID);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getTotalMonthly()).isEqualTo(800_000L);
        assertThat(response.getItems().get(0).getName()).isEqualTo("알바");
    }

    @Test
    void 남의_수입은_삭제할_수_없다() {
        // 조회 단계에서 소유자를 함께 걸기 때문에 빈 값이 되어 삭제가 진행되지 않는다.
        given(recurringIncomeRepository.findByRecurringIncomeIdAndUserUserId(99L, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.deleteIncome(USER_ID, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(OnboardingErrorCode.INCOME_NOT_FOUND));

        verify(recurringIncomeRepository, never()).delete(any());
    }

    @Test
    void 수입을_삭제하면_남은_합계가_돌아온다() {
        RecurringIncome target = income(1L, "알바", 500_000L);
        given(recurringIncomeRepository.findByRecurringIncomeIdAndUserUserId(1L, USER_ID))
                .willReturn(Optional.of(target));
        given(recurringIncomeRepository.findAllByUserUserIdOrderByRecurringIncomeIdAsc(USER_ID))
                .willReturn(List.of(income(2L, "자립수당", 300_000L)));

        var response = onboardingService.deleteIncome(USER_ID, 1L);

        verify(recurringIncomeRepository).delete(target);
        assertThat(response.getTotalMonthly()).isEqualTo(300_000L);
    }

    // ── 자산 ─────────────────────────────────────────────────────────────────

    @Test
    void 자산과_자립정착금_수령여부가_저장된다() {
        var request = new AssetRequest();
        ReflectionTestUtils.setField(request, "totalSaved", 15_000_000L);
        ReflectionTestUtils.setField(request, "settlementReceived", SettlementReceived.RECEIVED);

        onboardingService.saveAssets(USER_ID, request);

        assertThat(user.getInitialAsset()).isEqualTo(15_000_000L);
        assertThat(user.getSettlementReceived()).isEqualTo(SettlementReceived.RECEIVED);
    }

    // ── 완료 ─────────────────────────────────────────────────────────────────

    @Test
    void 완료하면_플래그가_서고_수입과_지출이_요약된다() {
        user.updateProtectionEndDate(LocalDate.now().minusYears(1));
        given(recurringIncomeRepository.findAllByUserUserIdOrderByRecurringIncomeIdAsc(USER_ID))
                .willReturn(List.of(income(1L, "알바", 1_800_000L)));
        given(housingCostRepository.findById(USER_ID)).willReturn(Optional.of(
                HousingCost.builder().user(user).monthlyRent(650_000L).maintenanceFee(70_000L).build()));

        var response = onboardingService.complete(USER_ID);

        assertThat(user.isOnboardingCompleted()).isTrue();
        assertThat(response.getMonthlyIncome()).isEqualTo(1_800_000L);
        assertThat(response.getMonthlyExpense()).isEqualTo(720_000L);
        assertThat(response.getDDay()).isNotNull();
    }

    @Test
    void 보호종료일을_건너뛴_회원도_완료할_수_있다() {
        // 날짜를 모르는 사용자를 막으면 온보딩을 영영 끝내지 못한다. dDay만 null이 된다.
        var response = onboardingService.complete(USER_ID);

        assertThat(user.isOnboardingCompleted()).isTrue();
        assertThat(response.getDDay()).isNull();
        assertThat(response.getMonthlyExpense()).isZero();
    }

    @Test
    void 주거비를_입력하지_않았으면_지출은_0이다() {
        given(housingCostRepository.findById(USER_ID)).willReturn(Optional.of(
                HousingCost.builder().user(user).deposit(50_000_000L).build()));   // 전세 — 월 지출 없음

        // estimatedMonthly가 null이어도 합계 응답은 0이어야 한다.
        assertThat(onboardingService.complete(USER_ID).getMonthlyExpense()).isZero();
    }
}

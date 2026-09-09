package com.dday.domain.credit.service;

import com.dday.domain.credit.dto.response.PaymentHistoryResponse;
import com.dday.domain.credit.dto.response.PaymentRecordResponse;
import com.dday.domain.credit.dto.response.PaymentSyncResponse;
import com.dday.domain.credit.dto.response.PaymentTypeHistoryResponse;
import com.dday.domain.credit.entity.NonFinancialPayment;
import com.dday.domain.credit.entity.PaymentStatus;
import com.dday.domain.credit.entity.PaymentType;
import com.dday.domain.credit.repository.NonFinancialPaymentRepository;
import com.dday.domain.user.dto.UserErrorCode;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 비금융 납부 이력(통신요금·건강보험료·국민연금)을 만들고 조회한다.
 *
 * <p>지금은 외부 기관이 없어 <b>데모 이력을 서버가 만들어 넣는다.</b> 통신사·건강보험공단·
 * 국민연금공단이 실제로 붙으면 {@link #sync} 안쪽만 바꾸면 되도록, 만드는 일과 읽는 일을
 * 한 서비스에 두되 메서드로 갈라 놓았다.
 *
 * <p>인터페이스를 두지 않는다 (AGENTS.md §7).
 */
@Service
@RequiredArgsConstructor
public class NonFinancialPaymentService {

    /** 데모 이력을 몇 개월치 만들지. 기준월을 포함해 거슬러 올라간다. */
    private static final int DEMO_MONTHS = 12;

    /**
     * 데모에서 연체를 한 번 섞는 위치 — 기준월로부터 이 개월 전의 통신요금.
     *
     * <p>전부 정상 납부로 채우면 화면이 연체 상태를 한 번도 못 그린다. 무작위로 넣지 않는 건
     * 같은 회원이 볼 때마다 이력이 달라지면 안 되기 때문이다.
     */
    private static final int DEMO_LATE_MONTHS_AGO = 5;

    private final NonFinancialPaymentRepository paymentRepository;
    private final UserRepository userRepository;

    /** 데모 이력을 채운다. 이미 있는 달은 건드리지 않으므로 여러 번 불러도 안전하다. */
    @Transactional
    public PaymentSyncResponse sync(Long userId) {
        return sync(userId, YearMonth.now());
    }

    /**
     * 기준월을 받는 쪽. 테스트가 "이번 달"에 흔들리지 않도록 밖에서 넣는다
     * ({@code OnboardingCalculator}가 {@code today}를 받는 것과 같은 이유).
     */
    PaymentSyncResponse sync(Long userId, YearMonth baseMonth) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        Set<String> existing = new HashSet<>();
        for (NonFinancialPayment payment : paymentRepository.findAllByUserUserId(userId)) {
            existing.add(key(payment.getPaymentType(), YearMonth.from(payment.getBillingMonth())));
        }

        List<NonFinancialPayment> created = new ArrayList<>();
        for (PaymentType type : PaymentType.values()) {
            for (int monthsAgo = 0; monthsAgo < DEMO_MONTHS; monthsAgo++) {
                YearMonth month = baseMonth.minusMonths(monthsAgo);
                if (existing.contains(key(type, month))) {
                    continue;
                }
                created.add(demoPayment(user, type, month, monthsAgo));
            }
        }
        paymentRepository.saveAll(created);

        return PaymentSyncResponse.builder()
                .createdCount(created.size())
                .monthsCovered(DEMO_MONTHS)
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentHistoryResponse findHistory(Long userId) {
        List<NonFinancialPayment> payments = paymentRepository.findAllByUserUserId(userId);
        if (payments.isEmpty()) {
            return PaymentHistoryResponse.empty();
        }

        Map<PaymentType, List<NonFinancialPayment>> byType = new EnumMap<>(PaymentType.class);
        for (NonFinancialPayment payment : payments) {
            byType.computeIfAbsent(payment.getPaymentType(), type -> new ArrayList<>()).add(payment);
        }

        List<PaymentTypeHistoryResponse> types = new ArrayList<>();
        for (PaymentType type : PaymentType.values()) {
            List<NonFinancialPayment> ofType = byType.get(type);
            if (ofType != null) {
                types.add(toTypeHistory(type, ofType));
            }
        }

        Set<LocalDate> months = new HashSet<>();
        payments.forEach(payment -> months.add(payment.getBillingMonth()));

        return PaymentHistoryResponse.builder()
                .monthsCovered(months.size())
                .onTimeCount(countOf(payments, PaymentStatus.PAID))
                .lateCount(countOf(payments, PaymentStatus.LATE))
                .unpaidCount(countOf(payments, PaymentStatus.UNPAID))
                .types(types)
                .build();
    }

    private PaymentTypeHistoryResponse toTypeHistory(PaymentType type,
                                                     List<NonFinancialPayment> ofType) {
        // 최신 청구월부터. 화면도 이 순서로 그리고, 연속 납부도 앞에서부터 센다.
        ofType.sort(Comparator.comparing(NonFinancialPayment::getBillingMonth).reversed());
        NonFinancialPayment latest = ofType.get(0);

        return PaymentTypeHistoryResponse.builder()
                .paymentType(type)
                .label(type.getLabel())
                .institutionName(latest.getInstitutionName())
                .latestBillingMonth(PaymentRecordResponse.from(latest).getBillingMonth())
                .onTimeStreak(onTimeStreak(ofType))
                .lateCount(countOf(ofType, PaymentStatus.LATE))
                .records(ofType.stream().map(PaymentRecordResponse::from).toList())
                .build();
    }

    /** 최신순 목록에서 앞부터 정상 납부가 몇 번 이어지는지. 연체·미납을 만나면 멈춘다. */
    private int onTimeStreak(List<NonFinancialPayment> latestFirst) {
        int streak = 0;
        for (NonFinancialPayment payment : latestFirst) {
            if (payment.getStatus() != PaymentStatus.PAID) {
                break;
            }
            streak++;
        }
        return streak;
    }

    private int countOf(List<NonFinancialPayment> payments, PaymentStatus status) {
        return (int) payments.stream().filter(payment -> payment.getStatus() == status).count();
    }

    private String key(PaymentType type, YearMonth month) {
        return type.name() + '|' + month;
    }

    /**
     * 데모 한 건. 금액과 납부기한은 종류마다 고정이다 — 무작위로 만들면 같은 회원이 볼 때마다
     * 이력이 달라진다.
     */
    private NonFinancialPayment demoPayment(User user, PaymentType type, YearMonth month,
                                            int monthsAgo) {
        DemoSpec spec = DemoSpec.of(type);
        LocalDate dueDate = month.plusMonths(1).atDay(spec.dueDay);

        boolean late = type == PaymentType.TELECOM && monthsAgo == DEMO_LATE_MONTHS_AGO;
        PaymentStatus status = late ? PaymentStatus.LATE : PaymentStatus.PAID;
        LocalDate paidDate = late ? dueDate.plusDays(6) : dueDate.minusDays(1);

        return NonFinancialPayment.builder()
                .user(user)
                .paymentType(type)
                .institutionName(spec.institutionName)
                .billingMonth(month.atDay(1))
                .amount(spec.amount)
                .dueDate(dueDate)
                .paidDate(paidDate)
                .status(status)
                .build();
    }

    /** 종류별 데모 기본값. 실제 기관이 붙으면 통째로 사라질 자리다. */
    private record DemoSpec(String institutionName, long amount, int dueDay) {

        static DemoSpec of(PaymentType type) {
            return switch (type) {
                case TELECOM -> new DemoSpec("SK텔레콤", 38_500L, 25);
                case HEALTH_INSURANCE -> new DemoSpec("국민건강보험공단", 68_200L, 10);
                case NATIONAL_PENSION -> new DemoSpec("국민연금공단", 90_000L, 10);
            };
        }
    }
}

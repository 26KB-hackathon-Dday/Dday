package com.dday.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 회원 한 명. {@code users} 테이블 매핑이다.
 *
 * <p><b>이 엔티티에는 인증(auth)에 필요한 컬럼만 있다.</b> 온보딩·프로필 컬럼
 * ({@code region_code}, {@code housing_type}, {@code initial_asset} 등)은 user/onboarding 도메인이
 * 담당자와 함께 여기에 덧붙일 몫이다. {@code ddl-auto: update}는 컬럼을 추가만 하므로
 * 나중에 필드를 더해도 기존 데이터는 그대로 남는다.
 *
 * <p>비밀번호는 평문으로 들어오지 않는다. 해시는 {@code AuthService}가 만들어 넘긴다 —
 * 엔티티가 {@code PasswordEncoder}를 알면 도메인이 스프링 빈에 묶인다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 255)
    private String email;

    /** BCrypt 해시. 길이가 항상 60이라 CHAR(60)으로 잡는다. */
    @Column(name = "password_hash", nullable = false, columnDefinition = "CHAR(60)")
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    /** 필수약관에 동의한 시각. 동의 없이는 가입이 안 되므로 실제로는 항상 채워진다. */
    @Column(name = "terms_agreed_at")
    private LocalDateTime termsAgreedAt;

    /** 위치정보 동의(선택). 미동의여도 가입은 된다. */
    @Column(name = "agreed_location", nullable = false)
    private boolean agreedLocation;

    /**
     * 보호종료일. 지원 종료 예정일과 D-day 계산의 기준이다.
     *
     * <p>시각이 아니라 날짜다 — "2027-03-01 00:00"과 "2027-03-01 09:00"은 D-day가 같은데
     * {@code LocalDateTime}으로 두면 둘이 다른 값이 되어 비교마다 시각을 잘라내야 한다.
     */
    @Column(name = "protection_end_date")
    private LocalDate protectionEndDate;

    /** 거주지 법정동 코드. 지원금 지역 조건 매칭에 쓴다. */
    @Column(name = "region_code", columnDefinition = "CHAR(10)")
    private String regionCode;

    /** 시·도 이름. 코드만 두면 화면에 지역을 띄울 때마다 코드표를 조회해야 한다. */
    @Column(name = "region_name", length = 30)
    private String regionName;

    /** 시·군·구 이름. */
    @Column(name = "district_name", length = 30)
    private String districtName;

    @Enumerated(EnumType.STRING)
    @Column(name = "housing_type", length = 30)
    private HousingType housingType;

    /**
     * 온보딩을 끝냈는지. 보호종료일이 있는지로 유추하지 않고 플래그를 따로 둔다 —
     * 온보딩에서 보호종료일을 모르겠다고 넘긴 사람이 영영 온보딩 화면에 갇힌다.
     */
    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    /** 마이데이터를 한 번이라도 연결했는지. */
    @Column(name = "mydata_connected", nullable = false)
    private boolean mydataConnected;

    @Column(name = "mydata_connected_at")
    private LocalDateTime mydataConnectedAt;

    /**
     * 마이데이터 동의 만료 시각. 법정 유효기간이 있어 지나면 재동의를 받아야 하고,
     * 그 전에는 동기화를 돌려선 안 된다.
     */
    @Column(name = "mydata_consent_expires_at")
    private LocalDateTime mydataConsentExpiresAt;

    /** 온보딩 시점의 보유 자산(원). 예산 초안의 출발점이다. */
    @Column(name = "initial_asset")
    private Long initialAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_received", length = 20)
    private SettlementReceived settlementReceived;

    /**
     * 탈퇴 사유. 자유 입력이라 enum이 아니다 — 선택지가 아직 확정되지 않았고,
     * 확정되면 여기에 코드 컬럼을 하나 더 두고 이 필드는 "기타" 상세로 남기면 된다.
     * 탈퇴하지 않은 회원은 {@code null}이므로 nullable이다.
     */
    @Column(name = "withdrawal_reason", length = 100)
    private String withdrawReason;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private User(String email, String passwordHash, String name, String phone,
                 LocalDateTime termsAgreedAt, boolean agreedLocation) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phone = phone;
        this.termsAgreedAt = termsAgreedAt;
        this.agreedLocation = agreedLocation;
        this.status = UserStatus.ACTIVE;
    }

    /** 비밀번호 재설정·변경. 넘어오는 값은 이미 해시된 것이어야 한다. */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    /**
     * 마이페이지 기본정보 수정. {@code null}인 항목은 "안 바꾼다"는 뜻이라 건너뛴다 —
     * PATCH라서 프론트가 바뀐 필드만 보내기 때문이다. (이메일은 여기서 못 바꾼다:
     * 로그인 아이디라 바꾸려면 중복 확인·재인증이 필요하고, 그건 auth 흐름이다)
     */
    public void updateProfile(String name, String phone) {
        if (name != null) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
    }

    /** 행을 지우지 않는다. 상태만 바꾸고 사유와 시각을 남긴다. */
    public void withdraw(String reason, LocalDateTime withdrawnAt) {
        this.status = UserStatus.WITHDRAWN;
        this.withdrawReason = reason;
        this.withdrawnAt = withdrawnAt;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    // ── 온보딩 ──────────────────────────────────────────────────────────────
    //
    // 온보딩은 화면을 하나씩 넘기며 저장하므로 단계별 메서드로 나눈다. 한 번에 다 받는
    // 메서드 하나로 두면 중간 단계에서는 안 넣은 값을 null로 넘겨야 하고, 그러면
    // "아직 입력 안 함"과 "비우기"가 구분되지 않는다.

    public void updateProtectionEndDate(LocalDate protectionEndDate) {
        this.protectionEndDate = protectionEndDate;
    }

    public void updateRegion(String regionCode, String regionName, String districtName) {
        this.regionCode = regionCode;
        this.regionName = regionName;
        this.districtName = districtName;
    }

    public void updateHousingType(HousingType housingType) {
        this.housingType = housingType;
    }

    /** 모아둔 자산과 자립정착금 수령 여부. 온보딩 자산 화면에서 함께 받는다. */
    public void updateAssets(Long initialAsset, SettlementReceived settlementReceived) {
        this.initialAsset = initialAsset;
        this.settlementReceived = settlementReceived;
    }

    /**
     * 온보딩 완료 표시.
     *
     * <p>단계별 값은 이미 저장돼 있으므로 플래그만 세운다. 되돌리는 메서드는 두지 않는다 —
     * 완료를 취소하는 흐름이 없고, 있으면 실수로 남의 온보딩을 되돌릴 여지만 생긴다.
     */
    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    /** 마이데이터 연결 완료. 최초 연결 시각은 처음 한 번만 남긴다 — 재연결로 덮어쓰면 언제부터 쓴 건지 사라진다. */
    public void connectMydata(LocalDateTime connectedAt, LocalDateTime consentExpiresAt) {
        this.mydataConnected = true;
        if (this.mydataConnectedAt == null) {
            this.mydataConnectedAt = connectedAt;
        }
        this.mydataConsentExpiresAt = consentExpiresAt;
    }

    /** 연결된 기관을 마지막 하나까지 해제했을 때 부른다. 재연결하면 다시 동의를 받으므로 만료시각도 지운다. */
    public void disconnectMydata() {
        this.mydataConnected = false;
        this.mydataConsentExpiresAt = null;
    }

    /** 동의가 아직 살아있는지. 만료됐으면 동기화를 돌리지 않고 재동의를 받아야 한다. */
    public boolean hasValidMydataConsent(LocalDateTime now) {
        return this.mydataConnected
                && this.mydataConsentExpiresAt != null
                && this.mydataConsentExpiresAt.isAfter(now);
    }
}

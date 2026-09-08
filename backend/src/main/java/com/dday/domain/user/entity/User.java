package com.dday.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
     * 온보딩 완료 여부를 판단하는 근거. <b>쓰기는 온보딩 도메인 몫이라 읽기 전용으로 매핑한다.</b>
     * (auth가 실수로 이 값을 덮어쓰면 남의 도메인 상태가 깨진다)
     */
    @Column(name = "protection_end_date", insertable = false, updatable = false)
    private LocalDateTime protectionEndDate;

    /**
     * 탈퇴 사유. 자유 입력이라 enum이 아니다 — 선택지가 아직 확정되지 않았고,
     * 확정되면 여기에 코드 컬럼을 하나 더 두고 이 필드는 "기타" 상세로 남기면 된다.
     * 탈퇴하지 않은 회원은 {@code null}이므로 nullable이다.
     */
    @Column(name = "withdraw_reason", length = 500)
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

    /**
     * 온보딩을 마쳤는지. 지금은 보호종료일이 설정됐는지로 판단한다 —
     * 온보딩 도메인이 완료 플래그를 따로 만들면 그때 이 메서드만 고치면 된다.
     */
    public boolean isOnboardingCompleted() {
        return this.protectionEndDate != null;
    }
}

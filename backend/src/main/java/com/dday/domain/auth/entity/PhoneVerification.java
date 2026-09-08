package com.dday.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 발송한 휴대폰 인증번호 한 건.
 *
 * <p>재발송할 때 기존 행을 덮어쓰지 않고 <b>매번 새 행을 쌓는다.</b> 검증은 항상 가장 최근 행만
 * 본다({@code findTopBy...OrderByIdDesc}). 덮어쓰면 "언제 몇 번 보냈는지"가 사라져
 * 나중에 발송 제한을 붙일 때 근거가 없어진다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "phone_verification",
        // 검증은 (phone, purpose)로 최신 행을 찾는 조회뿐이다. 인덱스가 없으면 풀스캔이 된다.
        indexes = @Index(name = "idx_phone_verification_phone_purpose", columnList = "phone, purpose")
)
public class PhoneVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long verificationId;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, columnDefinition = "CHAR(6)")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationPurpose purpose;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    /**
     * 인증에 성공한 시각. 아직 검증 전이면 {@code null}이다.
     *
     * <p>{@code expiresAt}(코드 자체의 수명)과 다른 값이다. 코드는 3분이지만 인증을 마친 뒤
     * 가입 폼을 채우는 데는 그보다 오래 걸린다 — 가입 시점에 "언제 인증했는지"를 따로 봐야 한다.
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private PhoneVerification(String phone, String code, VerificationPurpose purpose, LocalDateTime expiresAt) {
        this.phone = phone;
        this.code = code;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.verified = false;
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public void markVerified(LocalDateTime now) {
        this.verified = true;
        this.verifiedAt = now;
    }

    /**
     * 인증을 마친 지 {@code validity} 안쪽인지. 가입·재설정을 실제로 진행해도 되는지의 판단이다.
     *
     * <p>검증하지 않은 채 남아 있는 행은 무조건 false다 — {@code verifiedAt}이 {@code null}이다.
     */
    public boolean isVerifiedWithin(Duration validity, LocalDateTime now) {
        return verified
                && verifiedAt != null
                && verifiedAt.plus(validity).isAfter(now);
    }
}

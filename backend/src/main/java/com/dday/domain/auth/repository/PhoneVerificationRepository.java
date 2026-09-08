package com.dday.domain.auth.repository;

import com.dday.domain.auth.entity.PhoneVerification;
import com.dday.domain.auth.entity.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    /**
     * 그 번호로 마지막에 보낸 인증번호. {@code createdAt}이 아니라 id로 정렬하는 이유는
     * DATETIME이 초 단위라 같은 초에 두 번 발송하면 순서가 뒤집힐 수 있기 때문이다.
     */
    Optional<PhoneVerification> findTopByPhoneOrderByVerificationIdDesc(String phone);

    Optional<PhoneVerification> findTopByPhoneAndPurposeOrderByVerificationIdDesc(
            String phone, VerificationPurpose purpose);

    /**
     * 그 번호로 <b>인증까지 마친</b> 마지막 건. 가입 직전에 "정말 인증을 통과했는지" 확인하는 데 쓴다.
     *
     * <p>검증 안 된 행을 건너뛰는 게 핵심이다. 최신 행 하나만 보면, 인증을 마친 뒤 재발송 버튼을
     * 눌러 검증 안 된 행이 하나 더 쌓인 사용자가 가입에 실패한다.
     */
    Optional<PhoneVerification> findTopByPhoneAndPurposeAndVerifiedTrueOrderByVerificationIdDesc(
            String phone, VerificationPurpose purpose);
}

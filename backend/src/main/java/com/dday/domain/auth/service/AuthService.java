package com.dday.domain.auth.service;

import com.dday.domain.auth.dto.AuthErrorCode;
import com.dday.domain.auth.dto.request.*;
import com.dday.domain.auth.dto.response.*;
import com.dday.domain.auth.entity.PhoneVerification;
import com.dday.domain.auth.entity.VerificationPurpose;
import com.dday.domain.auth.repository.PhoneVerificationRepository;
import com.dday.domain.credit.service.CreditDemoProvisioner;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.common.jwt.JwtTokenProvider;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 인증 흐름 전체.
 *
 * <p><b>SMS는 실제로 보내지 않는다(목).</b> 발송 API는 고정 코드 {@value #MOCK_CODE}를 저장하고
 * 만료시각만 세팅하며, 검증은 저장된 코드 또는 {@value #MOCK_CODE}를 통과시킨다.
 * 실 발송을 붙일 때는 {@link #issueCode}에서 코드를 난수로 바꾸고 SMS 클라이언트를 부르면 되며,
 * {@link #matches} 안의 마스터 코드 분기를 <b>반드시 지워야 한다</b>.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 목 인증번호. 실 발송을 붙이는 순간 함께 제거한다. */
    private static final String MOCK_CODE = "371829";

    /** 인증번호 유효시간(초). 응답의 expiresIn으로도 나간다. */
    private static final int CODE_EXPIRES_IN_SECONDS = 180;

    /**
     * 휴대폰 인증을 마친 뒤 가입을 끝내야 하는 제한시간.
     *
     * <p>인증번호 자체의 수명(3분)보다 길다. 인증을 통과한 다음 이메일·비밀번호·약관을
     * 채우는 시간이 필요한데, 3분으로 잡으면 폼을 천천히 쓰는 사용자가 가입에 실패한다.
     * 반대로 제한을 아예 두지 않으면 한 번 인증한 번호로 언제까지나 가입할 수 있다.
     */
    private static final Duration SIGNUP_VERIFICATION_VALIDITY = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /** ⚠️ 시연용이다. 실제 신용평가사가 붙으면 이 의존과 {@link #signup} 안의 호출을 지운다. */
    private final CreditDemoProvisioner creditDemoProvisioner;

    // ── 휴대폰 인증 ──────────────────────────────────────────────────────────

    @Transactional
    public PhoneSendResponse sendPhoneCode(PhoneSendRequest request) {
        issueCode(request.getPhone(), request.getPurpose());
        return PhoneSendResponse.builder().expiresIn(CODE_EXPIRES_IN_SECONDS).build();
    }

    private void issueCode(String phone, VerificationPurpose purpose) {
        phoneVerificationRepository.save(PhoneVerification.builder()
                .phone(phone)
                .code(MOCK_CODE)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusSeconds(CODE_EXPIRES_IN_SECONDS))
                .build());

        log.info("인증번호 발송(목): phone={}, purpose={}", phone, purpose);
    }

    @Transactional
    public PhoneVerifyResponse verifyPhoneCode(PhoneVerifyRequest request) {
        PhoneVerification verification = phoneVerificationRepository
                .findTopByPhoneOrderByVerificationIdDesc(request.getPhone())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.VERIFICATION_NOT_FOUND));

        validate(verification, request.getCode());
        verification.markVerified(LocalDateTime.now());

        return PhoneVerifyResponse.builder().verified(true).build();
    }

    /** 만료·불일치를 각각 다른 코드로 던진다. 프론트가 "재발송" 버튼을 띄울지 정해야 하기 때문이다. */
    private void validate(PhoneVerification verification, String inputCode) {
        if (verification.isExpired(LocalDateTime.now())) {
            throw new BusinessException(AuthErrorCode.VERIFICATION_EXPIRED);
        }
        if (!matches(verification, inputCode)) {
            throw new BusinessException(AuthErrorCode.VERIFICATION_CODE_MISMATCH);
        }
    }

    /** ⚠️ 마스터 코드 분기는 목 전용이다. 실 SMS를 붙이면 지운다. */
    private boolean matches(PhoneVerification verification, String inputCode) {
        return MOCK_CODE.equals(inputCode) || verification.getCode().equals(inputCode);
    }

    // ── 이메일 중복 확인 ─────────────────────────────────────────────────────

    /** 중복이어도 에러가 아니다. 가입 폼에서 실시간으로 부르는 API라 200 + available:false로 답한다. */
    @Transactional(readOnly = true)
    public EmailCheckResponse checkEmail(EmailCheckRequest request) {
        boolean available = !userRepository.existsByEmail(request.getEmail());
        return EmailCheckResponse.builder().available(available).build();
    }

    // ── 가입 / 로그인 ────────────────────────────────────────────────────────

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (!request.isAgreedTerms() || !request.isAgreedPrivacy()) {
            throw new BusinessException(AuthErrorCode.TERMS_NOT_AGREED);
        }
        // 프론트가 /phone/verify를 불렀는지를 믿지 않고 서버가 다시 확인한다.
        // 이게 없으면 가입 API를 직접 때려 남의 번호로 계정을 만들 수 있다.
        requireVerifiedPhone(request.getPhone());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(AuthErrorCode.EMAIL_DUPLICATED);
        }

        User user = userRepository.save(User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .termsAgreedAt(LocalDateTime.now())
                .agreedLocation(request.isAgreedLocation())
                .build());

        /*
         * ⚠️ 시연용. 신용점수 이력과 비금융 납부 이력(통신요금·건강보험료·국민연금)을
         * 데모 계정에서 복제해 붙인다. 이 둘은 마이데이터 연동으로도 생기지 않아서
         * (연동은 계좌·카드만 가져온다) 가입 시점에 넣지 않으면 신용관리 화면이 빈 채로 뜬다.
         *
         * 마이데이터 연동을 건너뛴 회원도 화면을 볼 수 있어야 해서 연동이 아니라 가입에 건다.
         * 원본 시드가 없으면 조용히 건너뛰므로 가입 자체는 실패하지 않는다.
         */
        creditDemoProvisioner.provision(user.getUserId());

        return SignupResponse.builder()
                .userId(user.getUserId())
                .accessToken(jwtTokenProvider.createAccessToken(user.getUserId()))
                .refreshToken(jwtTokenProvider.createRefreshToken(user.getUserId()))
                .build();
    }

    /**
     * SIGNUP 목적으로 인증을 마친 기록이 {@link #SIGNUP_VERIFICATION_VALIDITY} 안에 있어야 한다.
     * PASSWORD_RESET 인증으로는 통과하지 못한다 — 남의 비밀번호 재설정 절차를 밟아
     * 그 번호로 계정을 만드는 걸 막는다.
     */
    private void requireVerifiedPhone(String phone) {
        boolean verified = phoneVerificationRepository
                .findTopByPhoneAndPurposeAndVerifiedTrueOrderByVerificationIdDesc(
                        phone, VerificationPurpose.SIGNUP)
                .filter(v -> v.isVerifiedWithin(SIGNUP_VERIFICATION_VALIDITY, LocalDateTime.now()))
                .isPresent();

        if (!verified) {
            throw new BusinessException(AuthErrorCode.PHONE_NOT_VERIFIED);
        }
    }

    /**
     * 이메일이 없을 때와 비밀번호가 틀렸을 때 <b>같은 예외</b>를 던진다.
     * 구분해 주면 어떤 이메일이 가입돼 있는지 알려주는 꼴이 된다.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndStatus(request.getEmail(), UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIAL));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIAL);
        }

        return LoginResponse.of(
                user,
                jwtTokenProvider.createAccessToken(user.getUserId()),
                jwtTokenProvider.createRefreshToken(user.getUserId()));
    }

    // ── 토큰 재발급 ──────────────────────────────────────────────────────────

    /**
     * 리프레시 토큰을 회전시키지 않는다 — 저장소에 토큰을 들고 있지 않아서 옛 토큰을 무효화할
     * 방법이 없기 때문이다. 로그아웃이 서버에서 무상태인 것도 같은 이유다.
     */
    @Transactional(readOnly = true)
    public TokenResponse reissue(TokenRefreshRequest request) {
        Long userId = jwtTokenProvider.parseUserIdFromRefreshToken(request.getRefreshToken());
        if (userId == null) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 토큰은 10년짜리다. 그 사이 탈퇴한 회원에게 새 액세스 토큰을 내주면 안 된다.
        userRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        return TokenResponse.builder()
                .accessToken(jwtTokenProvider.createAccessToken(userId))
                .build();
    }

    // ── 비밀번호 찾기 / 재설정 ───────────────────────────────────────────────

    /**
     * 가입되지 않은 값이면 {@link AuthErrorCode#USER_NOT_FOUND}를 던진다.
     * (로그인과 달리 여기서는 프론트가 "가입 이력이 없습니다"를 띄워야 해서 구분한다)
     */
    @Transactional
    public PhoneSendResponse findPassword(PasswordFindRequest request) {
        String input = request.getEmailOrPhone().trim();

        User user = (input.contains("@")
                ? userRepository.findByEmailAndStatus(input, UserStatus.ACTIVE)
                : userRepository.findByPhoneAndStatus(input, UserStatus.ACTIVE))
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        // 인증번호는 입력값이 아니라 가입 시 등록된 번호로 보낸다.
        issueCode(user.getPhone(), VerificationPurpose.PASSWORD_RESET);

        return PhoneSendResponse.builder().expiresIn(CODE_EXPIRES_IN_SECONDS).build();
    }

    /**
     * SIGNUP 목적으로 받은 코드로는 비밀번호를 바꿀 수 없다 — purpose를 걸어 조회한다.
     * 이게 없으면 가입 절차만 밟아도 남의 비밀번호를 바꿀 수 있다.
     */
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        PhoneVerification verification = phoneVerificationRepository
                .findTopByPhoneAndPurposeOrderByVerificationIdDesc(
                        request.getPhone(), VerificationPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.VERIFICATION_NOT_FOUND));

        validate(verification, request.getCode());

        User user = userRepository.findByPhoneAndStatus(request.getPhone(), UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        verification.markVerified(LocalDateTime.now());
    }
}

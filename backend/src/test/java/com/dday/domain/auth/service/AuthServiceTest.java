package com.dday.domain.auth.service;

import com.dday.domain.auth.dto.AuthErrorCode;
import com.dday.domain.auth.dto.request.SignupRequest;
import com.dday.domain.auth.entity.PhoneVerification;
import com.dday.domain.auth.entity.VerificationPurpose;
import com.dday.domain.auth.repository.PhoneVerificationRepository;
import com.dday.domain.credit.service.CreditDemoProvisioner;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.common.jwt.JwtTokenProvider;
import com.dday.global.exception.BusinessException;
import com.dday.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 가입 전 휴대폰 인증 강제에 집중한 테스트다.
 *
 * <p>이 검사가 없으면 {@code /api/auth/signup}을 직접 호출해 남의 번호로 계정을 만들 수 있다.
 * 프론트가 인증 화면을 먼저 띄운다는 건 보증이 아니다 — 서버만이 강제할 수 있다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    private static final String PHONE = "010-1234-5678";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PhoneVerificationRepository phoneVerificationRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private CreditDemoProvisioner creditDemoProvisioner;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, phoneVerificationRepository, passwordEncoder,
                jwtTokenProvider, creditDemoProvisioner);

        given(userRepository.existsByEmail(any())).willReturn(false);
        given(userRepository.save(any())).willAnswer(inv -> {
            User saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "userId", 1L);
            return saved;
        });
        given(jwtTokenProvider.createAccessToken(any())).willReturn("access");
        given(jwtTokenProvider.createRefreshToken(any())).willReturn("refresh");
    }

    /** 인증 기록이 있는 상태를 만든다. {@code verifiedAt}은 엔티티가 직접 채우므로 markVerified로 넣는다. */
    private void givenVerification(VerificationPurpose purpose, LocalDateTime verifiedAt) {
        PhoneVerification verification = PhoneVerification.builder()
                .phone(PHONE)
                .code("000000")
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .build();
        if (verifiedAt != null) {
            verification.markVerified(verifiedAt);
        }
        given(phoneVerificationRepository
                .findTopByPhoneAndPurposeAndVerifiedTrueOrderByVerificationIdDesc(PHONE, purpose))
                .willReturn(Optional.of(verification));
    }

    private SignupRequest signupRequest() {
        var request = new SignupRequest();
        ReflectionTestUtils.setField(request, "name", "김디데이");
        ReflectionTestUtils.setField(request, "phone", PHONE);
        ReflectionTestUtils.setField(request, "email", "test@dday.com");
        ReflectionTestUtils.setField(request, "password", "password123");
        ReflectionTestUtils.setField(request, "agreedTerms", true);
        ReflectionTestUtils.setField(request, "agreedPrivacy", true);
        return request;
    }

    private static ErrorCode errorCodeOf(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    @Test
    void 휴대폰_인증을_마쳤으면_가입된다() {
        givenVerification(VerificationPurpose.SIGNUP, LocalDateTime.now().minusMinutes(5));

        var response = authService.signup(signupRequest());

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getAccessToken()).isEqualTo("access");
    }

    /**
     * 신용관리 화면(신용점수 이력·납부 이력)은 가입 시점에 채워지지 않으면 빈 채로 뜬다.
     * 마이데이터 연동은 계좌·카드만 가져오므로 그쪽에 기댈 수 없다.
     */
    @Test
    void 가입하면_신용_데모_데이터가_붙는다() {
        givenVerification(VerificationPurpose.SIGNUP, LocalDateTime.now().minusMinutes(5));

        authService.signup(signupRequest());

        verify(creditDemoProvisioner).provision(1L);
    }

    @Test
    void 휴대폰_인증에_실패하면_신용_데모_데이터를_붙이지_않는다() {
        given(phoneVerificationRepository
                .findTopByPhoneAndPurposeAndVerifiedTrueOrderByVerificationIdDesc(any(), any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.signup(signupRequest()))
                .isInstanceOf(BusinessException.class);

        verify(creditDemoProvisioner, never()).provision(any());
    }

    @Test
    void 휴대폰_인증_없이는_가입할_수_없다() {
        // 발송·검증 기록이 아예 없는 상태 — signup API를 직접 호출한 경우다.
        given(phoneVerificationRepository
                .findTopByPhoneAndPurposeAndVerifiedTrueOrderByVerificationIdDesc(any(), any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.signup(signupRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(AuthErrorCode.PHONE_NOT_VERIFIED));

        verify(userRepository, never()).save(any());
    }

    @Test
    void 인증한지_30분이_지났으면_가입할_수_없다() {
        givenVerification(VerificationPurpose.SIGNUP, LocalDateTime.now().minusMinutes(31));

        assertThatThrownBy(() -> authService.signup(signupRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(AuthErrorCode.PHONE_NOT_VERIFIED));

        verify(userRepository, never()).save(any());
    }

    @Test
    void 비밀번호재설정용_인증으로는_가입할_수_없다() {
        // PASSWORD_RESET 인증만 있는 상태. SIGNUP으로 조회하면 비어 있어야 한다.
        givenVerification(VerificationPurpose.PASSWORD_RESET, LocalDateTime.now());
        given(phoneVerificationRepository
                .findTopByPhoneAndPurposeAndVerifiedTrueOrderByVerificationIdDesc(
                        PHONE, VerificationPurpose.SIGNUP))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.signup(signupRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(AuthErrorCode.PHONE_NOT_VERIFIED));
    }

    @Test
    void 약관_미동의는_휴대폰_인증보다_먼저_걸린다() {
        // 약관 화면으로 되돌려야 하는 흐름이라, 인증 여부를 보기 전에 판정되어야 한다.
        var request = signupRequest();
        ReflectionTestUtils.setField(request, "agreedTerms", false);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(AuthErrorCode.TERMS_NOT_AGREED));
    }

    @Test
    void 이미_가입된_이메일이면_중복으로_걸린다() {
        givenVerification(VerificationPurpose.SIGNUP, LocalDateTime.now());
        given(userRepository.existsByEmail(any())).willReturn(true);

        assertThatThrownBy(() -> authService.signup(signupRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(AuthErrorCode.EMAIL_DUPLICATED));
    }

    @Test
    void 가입_시_비밀번호는_BCrypt로_해시되어_저장된다() {
        givenVerification(VerificationPurpose.SIGNUP, LocalDateTime.now());

        authService.signup(signupRequest());

        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        String hash = captor.getValue().getPasswordHash();

        assertThat(hash).isNotEqualTo("password123").hasSize(60);
        assertThat(passwordEncoder.matches("password123", hash)).isTrue();
    }
}

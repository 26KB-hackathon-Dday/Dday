package com.dday.domain.user.service;

import com.dday.domain.user.dto.UserErrorCode;
import com.dday.domain.user.dto.request.PasswordChangeRequest;
import com.dday.domain.user.dto.request.UserUpdateRequest;
import com.dday.domain.user.dto.request.UserWithdrawRequest;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * {@link PasswordEncoder}는 목이 아니라 진짜 BCrypt를 쓴다. 해시 검증·재해시가 이 서비스의
 * 핵심인데 목으로 바꾸면 "matches가 true를 준다"만 확인하게 되어 테스트가 아무것도 못 막는다.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final String CURRENT_PASSWORD = "password123";

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);

        user = User.builder()
                .email("test@dday.com")
                .passwordHash(passwordEncoder.encode(CURRENT_PASSWORD))
                .name("김디데이")
                .phone("010-1234-5678")
                .termsAgreedAt(LocalDateTime.now())
                .agreedLocation(true)
                .build();
        // userId는 DB가 채우는 값이라 빌더에 없다. 응답 DTO 검증을 위해 직접 넣는다.
        ReflectionTestUtils.setField(user, "userId", USER_ID);
    }

    private void givenActiveUser() {
        given(userRepository.findByUserIdAndStatus(USER_ID, UserStatus.ACTIVE))
                .willReturn(Optional.of(user));
    }

    @Test
    void 회원정보를_조회하면_비밀번호는_응답에_담기지_않는다() {
        givenActiveUser();

        var response = userService.findMe(USER_ID);

        assertThat(response.getEmail()).isEqualTo("test@dday.com");
        assertThat(response.getName()).isEqualTo("김디데이");
        // passwordHash 필드 자체가 UserResponse에 없다 — 있으면 컴파일이 깨진다.
        assertThat(response).hasNoNullFieldsOrPropertiesExcept("protectionEndDate", "createdAt");
    }

    @Test
    void 탈퇴한_회원은_조회되지_않는다() {
        given(userRepository.findByUserIdAndStatus(USER_ID, UserStatus.ACTIVE))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findMe(USER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }

    @Test
    void 보내지_않은_필드는_수정되지_않는다() {
        givenActiveUser();

        // 이름만 보낸다. phone은 null이다.
        var request = new UserUpdateRequest();
        ReflectionTestUtils.setField(request, "name", "새이름");

        userService.updateMe(USER_ID, request);

        assertThat(user.getName()).isEqualTo("새이름");
        assertThat(user.getPhone()).isEqualTo("010-1234-5678");
    }

    @Test
    void 남이_쓰는_휴대폰_번호로는_수정할_수_없다() {
        givenActiveUser();
        given(userRepository.findByPhoneAndStatus(any(), any()))
                .willReturn(Optional.of(User.builder().phone("010-9999-9999").build()));

        var request = new UserUpdateRequest();
        ReflectionTestUtils.setField(request, "phone", "010-9999-9999");

        assertThatThrownBy(() -> userService.updateMe(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(UserErrorCode.PHONE_DUPLICATED));
    }

    @Test
    void 비밀번호를_바꾸면_새_비밀번호로_해시가_다시_만들어진다() {
        givenActiveUser();
        String oldHash = user.getPasswordHash();

        userService.changePassword(USER_ID, passwordChangeRequest(CURRENT_PASSWORD, "newPassword456"));

        assertThat(user.getPasswordHash()).isNotEqualTo(oldHash);
        assertThat(passwordEncoder.matches("newPassword456", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(CURRENT_PASSWORD, user.getPasswordHash())).isFalse();
    }

    @Test
    void 현재_비밀번호가_틀리면_비밀번호를_바꿀_수_없다() {
        givenActiveUser();
        String oldHash = user.getPasswordHash();

        assertThatThrownBy(() ->
                userService.changePassword(USER_ID, passwordChangeRequest("wrongPassword", "newPassword456")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(UserErrorCode.PASSWORD_MISMATCH));

        assertThat(user.getPasswordHash()).isEqualTo(oldHash);
    }

    @Test
    void 기존과_같은_비밀번호로는_바꿀_수_없다() {
        givenActiveUser();

        assertThatThrownBy(() ->
                userService.changePassword(USER_ID, passwordChangeRequest(CURRENT_PASSWORD, CURRENT_PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(UserErrorCode.SAME_AS_OLD_PASSWORD));
    }

    @Test
    void 탈퇴하면_행을_지우지_않고_상태와_사유만_남는다() {
        givenActiveUser();

        var request = new UserWithdrawRequest();
        ReflectionTestUtils.setField(request, "reason", "더 이상 필요하지 않아요");

        userService.withdraw(USER_ID, request);

        assertThat(user.isActive()).isFalse();
        assertThat(user.getWithdrawReason()).isEqualTo("더 이상 필요하지 않아요");
        assertThat(user.getWithdrawnAt()).isNotNull();
        // 이메일·이름은 남는다. 지우면 관련 데이터가 전부 고아가 된다.
        assertThat(user.getEmail()).isEqualTo("test@dday.com");
    }

    @Test
    void 사유_없이도_탈퇴할_수_있다() {
        givenActiveUser();

        userService.withdraw(USER_ID, null);

        assertThat(user.isActive()).isFalse();
        assertThat(user.getWithdrawReason()).isNull();
    }

    /** BusinessException의 message는 코드 이름이 아니라 사용자 문구다. 문구는 바뀌므로 코드로 단언한다. */
    private static com.dday.global.exception.ErrorCode errorCodeOf(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    private PasswordChangeRequest passwordChangeRequest(String current, String next) {
        var request = new PasswordChangeRequest();
        ReflectionTestUtils.setField(request, "currentPassword", current);
        ReflectionTestUtils.setField(request, "newPassword", next);
        return request;
    }
}

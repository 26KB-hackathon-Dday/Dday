package com.dday.domain.user.service;

import com.dday.domain.user.dto.UserErrorCode;
import com.dday.domain.user.dto.request.PasswordChangeRequest;
import com.dday.domain.user.dto.request.UserUpdateRequest;
import com.dday.domain.user.dto.request.UserWithdrawRequest;
import com.dday.domain.user.dto.response.UserResponse;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 마이페이지. 모든 메서드가 <b>토큰에서 꺼낸 {@code userId}</b>로만 회원을 찾는다 —
 * 요청 본문의 id를 믿으면 남의 정보를 고칠 수 있다.
 *
 * <p>조회 대상은 항상 {@link UserStatus#ACTIVE}다. 탈퇴한 회원의 토큰은 만료 전까지 서명이
 * 유효한 채로 남아 있어서, 상태를 걸지 않으면 탈퇴 후에도 마이페이지가 열린다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse findMe(Long userId) {
        return UserResponse.from(getActiveUser(userId));
    }

    /**
     * 기본정보(이름·휴대폰) 수정. 보내지 않은 필드는 그대로 둔다({@link User#updateProfile}).
     *
     * <p>휴대폰 번호는 비밀번호 찾기의 식별자로 쓰이므로 중복을 막는다. 중복을 허용하면
     * {@code findPassword}가 어느 계정을 집을지 알 수 없게 된다.
     */
    @Transactional
    public UserResponse updateMe(Long userId, UserUpdateRequest request) {
        User user = getActiveUser(userId);

        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            userRepository.findByPhoneAndStatus(request.getPhone(), UserStatus.ACTIVE)
                    .ifPresent(other -> {
                        throw new BusinessException(UserErrorCode.PHONE_DUPLICATED);
                    });
        }

        user.updateProfile(request.getName(), request.getPhone());
        return UserResponse.from(user);
    }

    /**
     * 현재 비밀번호를 확인한 뒤 새 비밀번호를 BCrypt로 다시 해시해 넣는다.
     *
     * <p>같은 비밀번호로 바꾸는 걸 막는 건, BCrypt는 salt가 매번 달라 해시만 보면
     * "안 바뀐 것"과 "바뀐 것"이 구분되지 않아 사용자가 바꿨다고 착각하기 때문이다.
     */
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = getActiveUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(UserErrorCode.PASSWORD_MISMATCH);
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException(UserErrorCode.SAME_AS_OLD_PASSWORD);
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        log.info("비밀번호 변경: userId={}", userId);
    }

    /**
     * 탈퇴. 행을 지우지 않고 상태만 {@link UserStatus#WITHDRAWN}으로 바꾼다 —
     * 지우면 포켓·거래내역이 전부 고아가 된다.
     *
     * <p>이미 발급된 토큰은 서명이 유효한 채로 남지만, 모든 조회가 ACTIVE를 걸고 있어
     * 다음 요청부터 404가 난다. 프론트는 그때 로그아웃시킨다.
     */
    @Transactional
    public void withdraw(Long userId, UserWithdrawRequest request) {
        User user = getActiveUser(userId);

        String reason = (request == null) ? null : request.getReason();
        user.withdraw(reason, LocalDateTime.now());

        log.info("회원 탈퇴: userId={}", userId);
    }

    private User getActiveUser(Long userId) {
        return userRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }
}

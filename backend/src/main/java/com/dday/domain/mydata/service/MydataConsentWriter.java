package com.dday.domain.mydata.service;

import com.dday.domain.user.dto.UserErrorCode;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 마이데이터 동의 기록만 담당한다.
 *
 * <p>{@link MydataConnectService}에서 분리한 이유는 <b>같은 클래스 안에서 부른
 * {@code @Transactional} 메서드는 프록시를 타지 않아 트랜잭션이 걸리지 않기</b> 때문이다.
 * 별도 빈으로 두어야 쓰기가 실제로 커밋된다.
 */
@Component
@RequiredArgsConstructor
public class MydataConsentWriter {

    private final UserRepository userRepository;

    /**
     * 동의를 남기고 회원 이름을 돌려준다.
     *
     * @return 목데이터에 쓸 회원 이름
     */
    @Transactional
    public String recordConsent(Long userId, LocalDateTime connectedAt, LocalDateTime expiresAt) {
        User user = userRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        user.connectMydata(connectedAt, expiresAt);
        return user.getName();
    }
}

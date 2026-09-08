package com.dday.domain.user.repository;

import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /** 탈퇴한 회원은 로그인·비밀번호 찾기에서 제외해야 하므로 상태를 함께 건다. */
    Optional<User> findByEmailAndStatus(String email, UserStatus status);

    Optional<User> findByPhoneAndStatus(String phone, UserStatus status);

    Optional<User> findByUserIdAndStatus(Long userId, UserStatus status);

    boolean existsByEmail(String email);
}

/**
 * Auth 도메인은 회원가입, 로그인/로그아웃, 휴대폰 본인인증, 비밀번호 찾기·재설정 등
 * 사용자 인증 흐름과 JWT 발급을 담당합니다.
 *
 * <p>인증 흐름과 휴대폰 인증 기록(PhoneVerification)은 이 모듈이 소유합니다. 회원 데이터
 * (User)는 user 모듈이 소유하므로, 계정 생성·조회 시 user 모듈의 공개 API(UserRepository,
 * UserService)를 통하며 users 테이블을 직접 다루지 않습니다. 토큰 생성·검증과 비밀번호
 * 해시는 global(JwtTokenProvider, PasswordEncoder)에 위임합니다.</p>
 */
package com.dday.domain.auth;
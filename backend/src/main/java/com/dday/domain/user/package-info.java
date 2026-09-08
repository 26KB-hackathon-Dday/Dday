/**
 * User 도메인은 회원 정보 조회·수정, 비밀번호 변경, 회원 탈퇴 등 회원 계정 관리와
 * 마이페이지 기본 기능을 담당합니다.
 *
 * <p>User 엔티티와 users 테이블 쓰기는 이 모듈이 소유합니다. 다른 모듈(auth, onboarding,
 * notification 등)은 users 테이블을 직접 수정하지 않고 이 모듈의 공개 서비스를 통해 회원
 * 정보를 조회·갱신합니다. 인증·토큰 발급은 auth 모듈이, 온보딩 프로필 입력은 onboarding
 * 모듈이 이 모듈의 공개 API로 요청합니다.</p>
 */
package com.dday.domain.user;
package com.dday.domain.user.controller;

import com.dday.domain.user.dto.UserSuccessCode;
import com.dday.domain.user.dto.request.PasswordChangeRequest;
import com.dday.domain.user.dto.request.UserUpdateRequest;
import com.dday.domain.user.dto.request.UserWithdrawRequest;
import com.dday.domain.user.dto.response.UserResponse;
import com.dday.domain.user.service.UserService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 마이페이지. 전부 인증이 필요하다({@code SecurityConfig}의 {@code anyRequest().authenticated()}).
 *
 * <p><b>경로에 회원 id를 두지 않고 {@code /me}로 고정한다.</b> {@code /api/users/{userId}}를
 * 열어두면 남의 id를 넣어보는 순간 권한 검사를 매 엔드포인트마다 직접 해야 한다.
 * {@code userId}는 {@code JwtAuthenticationFilter}가 토큰에서 꺼내 앉혀둔 값을 받는다.
 */
@Tag(name = "회원")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 회원정보 조회", description = """
            | HTTP | code | message |
            |---|---|---|
            | 401 | UNAUTHORIZED | 로그인이 필요합니다. |
            | 404 | USER_NOT_FOUND | 회원 정보를 찾을 수 없습니다. |
            """)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> findMe(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(UserSuccessCode.USER_FOUND, userService.findMe(userId));
    }

    @Operation(summary = "내 기본정보 수정", description = """
            PATCH다. **보내지 않은 필드는 바뀌지 않는다.** 이메일은 여기서 바꿀 수 없다.

            | HTTP | code | message |
            |---|---|---|
            | 409 | PHONE_DUPLICATED | 이미 사용 중인 휴대폰 번호입니다. |
            """)
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.of(UserSuccessCode.USER_UPDATED, userService.updateMe(userId, request));
    }

    @Operation(summary = "비밀번호 변경", description = """
            현재 비밀번호를 알아야 한다. 비밀번호를 잊었다면 `/api/auth/password/find`를 쓴다.

            | HTTP | code | message |
            |---|---|---|
            | 400 | PASSWORD_MISMATCH | 현재 비밀번호가 올바르지 않습니다. |
            | 400 | SAME_AS_OLD_PASSWORD | 새 비밀번호가 기존 비밀번호와 같습니다. |
            """)
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(userId, request);
        return ApiResponse.of(UserSuccessCode.PASSWORD_CHANGED);
    }

    @Operation(summary = "회원 탈퇴", description = """
            데이터를 지우지 않고 상태만 WITHDRAWN으로 바꾼다. 탈퇴 사유는 선택이라
            본문 없이 호출해도 된다(일부 HTTP 클라이언트는 DELETE 본문을 버린다).

            성공하면 프론트는 저장된 토큰을 지운다 — 서버는 토큰을 폐기하지 않지만
            다음 요청부터 USER_NOT_FOUND가 난다.
            """)
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody(required = false) UserWithdrawRequest request) {
        userService.withdraw(userId, request);
        return ApiResponse.of(UserSuccessCode.USER_WITHDRAWN);
    }
}

package com.dday.domain.auth.controller;

import com.dday.domain.auth.dto.AuthSuccessCode;
import com.dday.domain.auth.dto.request.*;
import com.dday.domain.auth.dto.response.*;
import com.dday.domain.auth.service.AuthService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "휴대폰 인증번호 발송", description = """
            실제 SMS는 보내지 않는다. 인증번호는 항상 `000000`이고 3분 뒤 만료된다.
            """)
    @PostMapping("/phone/send")
    public ResponseEntity<ApiResponse<PhoneSendResponse>> sendPhoneCode(
            @Valid @RequestBody PhoneSendRequest request) {
        return ApiResponse.of(AuthSuccessCode.VERIFICATION_CODE_SENT, authService.sendPhoneCode(request));
    }

    @Operation(summary = "휴대폰 인증번호 확인", description = """
            | HTTP | code | message |
            |---|---|---|
            | 400 | VERIFICATION_NOT_FOUND | 인증번호를 먼저 발송해주세요. |
            | 400 | VERIFICATION_EXPIRED | 인증번호가 만료되었습니다. 다시 발송해주세요. |
            | 400 | VERIFICATION_CODE_MISMATCH | 인증번호가 올바르지 않습니다. |
            """)
    @PostMapping("/phone/verify")
    public ResponseEntity<ApiResponse<PhoneVerifyResponse>> verifyPhoneCode(
            @Valid @RequestBody PhoneVerifyRequest request) {
        return ApiResponse.of(AuthSuccessCode.PHONE_VERIFIED, authService.verifyPhoneCode(request));
    }

    @Operation(summary = "이메일 중복 확인", description = """
            중복이어도 에러가 아니라 `data.available = false`로 내려온다.
            """)
    @PostMapping("/email/check")
    public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(
            @Valid @RequestBody EmailCheckRequest request) {
        return ApiResponse.of(AuthSuccessCode.EMAIL_CHECKED, authService.checkEmail(request));
    }

    @Operation(summary = "회원가입", description = """
            성공하면 바로 로그인된 상태가 되도록 토큰까지 함께 내려준다.

            **먼저 `/phone/send`(purpose=SIGNUP) → `/phone/verify`로 휴대폰 인증을 마쳐야 한다.**
            서버가 다시 확인하므로 이 API만 단독으로 부르면 실패한다.
            인증 후 30분이 지나면 다시 받아야 한다.

            | HTTP | code | message |
            |---|---|---|
            | 400 | TERMS_NOT_AGREED | 필수 약관에 동의해야 가입할 수 있습니다. |
            | 400 | PHONE_NOT_VERIFIED | 휴대폰 인증을 먼저 완료해주세요. |
            | 409 | EMAIL_DUPLICATED | 이미 가입된 이메일입니다. |
            """)
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request) {
        return ApiResponse.of(AuthSuccessCode.SIGNUP_COMPLETED, authService.signup(request));
    }

    @Operation(summary = "로그인", description = """
            이메일이 없을 때와 비밀번호가 틀렸을 때 응답이 같다(가입 여부 노출 방지).

            | HTTP | code | message |
            |---|---|---|
            | 401 | INVALID_CREDENTIAL | 이메일 또는 비밀번호가 올바르지 않습니다. |
            """)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ApiResponse.of(AuthSuccessCode.LOGIN_SUCCEEDED, authService.login(request));
    }

    @Operation(summary = "로그아웃", description = """
            JWT 무상태라 서버가 하는 일이 없다. 토큰 폐기는 클라이언트가 저장소에서 지우는 것으로 끝난다.
            """)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ApiResponse.of(AuthSuccessCode.LOGOUT_SUCCEEDED);
    }

    @Operation(summary = "액세스 토큰 재발급", description = """
            | HTTP | code | message |
            |---|---|---|
            | 401 | INVALID_REFRESH_TOKEN | 다시 로그인해주세요. |
            """)
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(
            @Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.of(AuthSuccessCode.TOKEN_REISSUED, authService.reissue(request));
    }

    @Operation(summary = "비밀번호 찾기 (인증번호 발송)", description = """
            이메일이나 휴대폰 번호로 회원을 찾아, 가입 시 등록된 번호로 인증번호를 보낸다.

            | HTTP | code | message |
            |---|---|---|
            | 404 | USER_NOT_FOUND | 가입된 회원 정보를 찾을 수 없습니다. |
            """)
    @PostMapping("/password/find")
    public ResponseEntity<ApiResponse<PhoneSendResponse>> findPassword(
            @Valid @RequestBody PasswordFindRequest request) {
        return ApiResponse.of(AuthSuccessCode.VERIFICATION_CODE_SENT, authService.findPassword(request));
    }

    @Operation(summary = "비밀번호 재설정", description = """
            `/password/find`로 받은 인증번호가 있어야 한다. 가입용(SIGNUP) 인증번호로는 통과하지 못한다.
            """)
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ApiResponse.of(AuthSuccessCode.PASSWORD_RESET);
    }
}

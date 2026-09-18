package com.back.domain.member.controller;

import com.back.domain.member.dto.LoginRequest;
import com.back.domain.member.dto.LoginResponse;
import com.back.domain.member.dto.RefreshTokenRequest;
import com.back.domain.member.service.AuthTokenService;
import com.back.domain.member.service.MemberService;
import com.back.global.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final AuthTokenService authTokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        // 아이디와 비밀번호를 검증
        Long memberId = memberService.authenticate(request);

        String accessToken = authTokenService.createAccessToken(memberId);

        String refreshToken = authTokenService.createRefreshToken(memberId);

        LoginResponse response = LoginResponse.of(
                accessToken,
                refreshToken
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        String refreshToken = request.refreshToken();

        if (!authTokenService.validateToken(refreshToken)
                || !authTokenService.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException(
                    "유효하지 않은 리프레시 토큰입니다."
            );
        }

        Long memberId =
                authTokenService.getMemberId(refreshToken);

        String accessToken =
                authTokenService.createAccessToken(memberId);

        LoginResponse response = LoginResponse.of(
                accessToken,
                refreshToken
        );

        return ResponseEntity.ok(response);
    }
}
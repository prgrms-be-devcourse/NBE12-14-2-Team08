package com.back.domain.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTokenServiceTest {

    private AuthTokenService authTokenService;

    @BeforeEach
    void setUp() {
        authTokenService = new AuthTokenService(
                "local-development-jwt-secret-key-1234567890",
                1_800_000L
        );
    }

    @Test
    @DisplayName("액세스 토큰 생성 및 검증 성공")
    void createAndValidateAccessToken() {
        // when
        String accessToken =
                authTokenService.createAccessToken(1L);

        // then
        assertThat(
                authTokenService.validateToken(accessToken)
        ).isTrue();

        assertThat(
                authTokenService.getMemberId(accessToken)
        ).isEqualTo(1L);

        assertThat(
                authTokenService.isAccessToken(accessToken)
        ).isTrue();
    }

    @Test
    @DisplayName("리프레시 토큰 생성 및 검증 성공")
    void createAndValidateRefreshToken() {
        // when
        String refreshToken =
                authTokenService.createRefreshToken(1L);

        // then
        assertThat(
                authTokenService.validateToken(refreshToken)
        ).isTrue();

        assertThat(
                authTokenService.getMemberId(refreshToken)
        ).isEqualTo(1L);

        assertThat(
                authTokenService.isRefreshToken(refreshToken)
        ).isTrue();
    }

    @Test
    @DisplayName("액세스 토큰과 리프레시 토큰 구분")
    void distinguishTokenTypes() {
        // given
        String accessToken =
                authTokenService.createAccessToken(1L);

        String refreshToken =
                authTokenService.createRefreshToken(1L);

        // then
        assertThat(
                authTokenService.isAccessToken(accessToken)
        ).isTrue();

        assertThat(
                authTokenService.isRefreshToken(accessToken)
        ).isFalse();

        assertThat(
                authTokenService.isRefreshToken(refreshToken)
        ).isTrue();

        assertThat(
                authTokenService.isAccessToken(refreshToken)
        ).isFalse();
    }

    @Test
    @DisplayName("잘못된 토큰 검증 실패")
    void invalidTokenFail() {
        assertThat(
                authTokenService.validateToken("invalid-token")
        ).isFalse();
    }
}
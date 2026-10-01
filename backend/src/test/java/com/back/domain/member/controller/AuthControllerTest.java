package com.back.domain.member.controller;

import com.back.domain.member.dto.LoginRequest;
import com.back.domain.member.service.AuthTokenService;
import com.back.domain.member.service.MemberService;
import com.back.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private MemberService memberService;

    @Mock
    private AuthTokenService authTokenService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    @DisplayName("로그인 API 성공")
    void loginSuccess() throws Exception {
        // given
        when(memberService.authenticate(
                any(LoginRequest.class)
        )).thenReturn(1L);

        when(authTokenService.createAccessToken(1L))
                .thenReturn("access-token");

        when(memberService.getRefreshToken(1L))
                .thenReturn("refresh-token");

        // when & then
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "username": "user@example.com",
                                          "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken")
                                .value("access-token")
                )
                .andExpect(
                        jsonPath("$.refreshToken")
                                .value("refresh-token")
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                );
    }

    @Test
    @DisplayName("리프레시 토큰으로 액세스 토큰 재발급 성공")
    void refreshSuccess() throws Exception {
        // given
        when(authTokenService.validateToken("refresh-token"))
                .thenReturn(true);

        when(authTokenService.isRefreshToken("refresh-token"))
                .thenReturn(true);

        when(authTokenService.getMemberId("refresh-token"))
                .thenReturn(1L);

        when(memberService.matchesRefreshToken(
                1L,
                "refresh-token"
        )).thenReturn(true);

        when(authTokenService.createAccessToken(1L))
                .thenReturn("new-access-token");

        // when & then
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "refreshToken": "refresh-token"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken")
                                .value("new-access-token")
                )
                .andExpect(
                        jsonPath("$.refreshToken")
                                .value("refresh-token")
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                );
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰 재발급 실패")
    void refreshFailWhenTokenInvalid() throws Exception {
        // given
        when(authTokenService.validateToken("invalid-token"))
                .thenReturn(false);

        // when & then
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "refreshToken": "invalid-token"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.status").value(401)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "유효하지 않은 리프레시 토큰입니다."
                                )
                );
    }

    @Test
    @DisplayName("DB 저장값과 다른 리프레시 토큰 재발급 실패")
    void refreshFailWhenTokenDoesNotMatchDatabase()
            throws Exception {
        // given
        when(authTokenService.validateToken("refresh-token"))
                .thenReturn(true);

        when(authTokenService.isRefreshToken("refresh-token"))
                .thenReturn(true);

        when(authTokenService.getMemberId("refresh-token"))
                .thenReturn(1L);

        when(memberService.matchesRefreshToken(
                1L,
                "refresh-token"
        )).thenReturn(false);

        // when & then
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "refreshToken": "refresh-token"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.status").value(401)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "저장된 리프레시 토큰과 일치하지 않습니다."
                                )
                );
    }
}
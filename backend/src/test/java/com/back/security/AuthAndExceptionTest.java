package com.back.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.global.exception.ForbiddenException;
import com.back.global.exception.GlobalExceptionHandler;
import com.back.global.exception.UnauthorizedException;
import com.back.global.webMvc.WebMvcConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = AuthAndExceptionTest.TestController.class)
@Import({WebMvcConfig.class, MockAuthInterceptor.class, LoginMemberArgumentResolver.class, GlobalExceptionHandler.class})
class AuthAndExceptionTest {

    @Autowired
    private MockMvc mockMvc;

    // --- 테스트 전용 Dummy Controller & DTO ---
    @RestController
    static class TestController {

        // 래퍼 타입 주입 검증
        @GetMapping("/api/test/auth-wrapper")
        public String checkAuthWrapper(@LoginMemberId Long memberId) {
            return "memberId: " + memberId;
        }

        // 기본형 타입 주입 검증
        @GetMapping("/api/test/auth-primitive")
        public String checkAuthPrimitive(@LoginMemberId long memberId) {
            return "memberId: " + memberId;
        }

        @PostMapping("/api/test/validation")
        public String checkValidation(@RequestBody @Valid DummyDto dto) {
            return dto.name();
        }

        @GetMapping("/api/test/401")
        public void throw401() {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        @GetMapping("/api/test/403")
        public void throw403() {
            throw new ForbiddenException("접근 권한이 없습니다.");
        }

        @GetMapping("/api/test/404")
        public void throw404() {
            throw new NoSuchElementException("데이터를 찾을 수 없습니다.");
        }

        @GetMapping("/api/test/409")
        public void throw409() {
            throw new IllegalStateException("이미 진행 중인 상태입니다.");
        }
    }

    record DummyDto(
        @NotBlank(message = "이름은 필수입니다.")
        String name
    ) {}

    // --- 1. 인증 미들웨어 (Mock Auth & Resolver) 검증 ---
    @Nested
    @DisplayName("임시 인증 미들웨어 테스트")
    class MockAuthTests {

        @Test
        @DisplayName("X-User-Id 헤더가 있으면 해당 ID가 Long 파라미터에 주입된다")
        void headerProvided_Long() throws Exception {
            mockMvc.perform(get("/api/test/auth-wrapper")
                    .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(content().string("memberId: 42"));
        }

        @Test
        @DisplayName("X-User-Id 헤더가 없으면 기본값 1L이 주입된다")
        void headerMissing_fallbackDefault() throws Exception {
            mockMvc.perform(get("/api/test/auth-wrapper"))
                .andExpect(status().isOk())
                .andExpect(content().string("memberId: 1"));
        }

        @Test
        @DisplayName("기본형 long 파라미터에도 정상적으로 ID가 주입된다")
        void primitiveLongSupported() throws Exception {
            mockMvc.perform(get("/api/test/auth-primitive")
                    .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string("memberId: 7"));
        }
    }

    // --- 2. GlobalExceptionHandler 공통 예외 응답 규격 검증 ---
    @Nested
    @DisplayName("GlobalExceptionHandler 예외 매핑 테스트")
    class ExceptionHandlerTests {

        @Test
        @DisplayName("401 UnauthorizedException 처리")
        void unauthorized() throws Exception {
            mockMvc.perform(get("/api/test/401"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
        }

        @Test
        @DisplayName("403 ForbiddenException 처리")
        void forbidden() throws Exception {
            mockMvc.perform(get("/api/test/403"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
        }

        @Test
        @DisplayName("404 NoSuchElementException 처리")
        void notFound() throws Exception {
            mockMvc.perform(get("/api/test/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("데이터를 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("409 IllegalStateException 처리")
        void conflict() throws Exception {
            mockMvc.perform(get("/api/test/409"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("이미 진행 중인 상태입니다."));
        }

        @Test
        @DisplayName("400 @Valid 유효성 검증 실패 시 errors 리스트 반환")
        void validationError() throws Exception {
            mockMvc.perform(post("/api/test/validation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다."))
                .andExpect(jsonPath("$.errors[0]").value("이름은 필수입니다."));
        }

        @Test
        @DisplayName("400 깨진 JSON 본문 전달 시 HttpMessageNotReadableException 처리")
        void notReadableJson() throws Exception {
            mockMvc.perform(post("/api/test/validation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ invalid: json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("요청 본문(JSON) 형식이 올바르지 않습니다."));
        }
    }
}
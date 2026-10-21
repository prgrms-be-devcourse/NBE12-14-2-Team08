package com.back.global.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.service.AuthTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// 실제 보안 필터 체인을 거쳐서, 인증 실패는 401 / 권한 부족은 403으로 구분되는지 확인한다.
// 프론트의 토큰 자동 갱신은 401일 때만 동작한다. (메모리 DB, 테스트 종료 시 롤백)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationFailureStatusTest {

    private static final String PROTECTED_URL = "/api/members/me";

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private GroupRepository groupRepository;
    @Value("${jwt.secret}") private String jwtSecret;

    private Member member;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        member = memberRepository.save(Member.create("sec-" + suffix, "sec-" + suffix, "pw1234!"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("유효한 액세스 토큰이면 보호된 API에 접근할 수 있다 (대조군)")
    void 유효한_토큰은_통과한다() throws Exception {
        mockMvc.perform(get(PROTECTED_URL)
                        .header("Authorization", bearer(authTokenService.createAccessToken(member.getId()))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("토큰이 없으면 401")
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(get(PROTECTED_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("형식이 잘못된 토큰이면 401")
    void 잘못된_토큰이면_401() throws Exception {
        mockMvc.perform(get(PROTECTED_URL).header("Authorization", bearer("abc.def.ghi")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("서명이 다른(위조된) 토큰이면 401")
    void 위조된_토큰이면_401() throws Exception {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "another-secret-key-another-secret-key-1234567890".getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder()
                .subject(String.valueOf(member.getId()))
                .claim("tokenType", "access")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료된 액세스 토큰이면 401 (프론트가 이 응답으로 토큰을 갱신한다)")
    void 만료된_토큰이면_401() throws Exception {
        String expired = Jwts.builder()
                .subject(String.valueOf(member.getId()))
                .claim("tokenType", "access")
                .issuedAt(new Date(System.currentTimeMillis() - 3_600_000))
                .expiration(new Date(System.currentTimeMillis() - 1_800_000))
                .signWith(secretKey())
                .compact();

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리프레시 토큰을 액세스 토큰 자리에 쓰면 401")
    void 리프레시_토큰으로는_접근할_수_없다() throws Exception {
        String refreshToken = authTokenService.createRefreshToken(member.getId());

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", bearer(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("탈퇴한 회원의 토큰이면 401")
    void 탈퇴한_회원의_토큰이면_401() throws Exception {
        String token = authTokenService.createAccessToken(member.getId());
        member.withdraw();
        memberRepository.saveAndFlush(member);

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인은 했지만 권한이 없으면(다른 그룹 조회) 그대로 403")
    void 권한이_없으면_403() throws Exception {
        Group group = groupRepository.save(Group.builder()
                .title("권한 테스트 그룹")
                .penalty("커피 사기")
                .password("1234")
                .inviteCode("S" + UUID.randomUUID().toString().substring(0, 8))
                .memberLimit(5)
                .deadline(LocalDate.now().plusDays(7))
                .build());

        mockMvc.perform(get("/api/groups/{id}", group.getId())
                        .header("Authorization", bearer(authTokenService.createAccessToken(member.getId()))))
                .andExpect(status().isForbidden());
    }
}

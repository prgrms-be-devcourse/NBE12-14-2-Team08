package com.back.domain.group.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.service.AuthTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthTokenService authTokenService;

    @Nested
    @DisplayName("그룹 생성")
    class CreateGroup {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("유효한 토큰으로 요청하면 그룹이 생성된다")
            void createGroup_WithValidToken() throws Exception {
                Member member = memberRepository.save(
                        Member.create(
                                "테스트 회원",
                                "test-group-controller",
                                "encoded-password"
                        )
                );

                String accessToken =
                        authTokenService.createAccessToken(member.getId());

                String requestBody = """
                        {
                            "title": "테스트 그룹",
                            "description": "테스트 설명",
                            "deadline": "2026-12-31",
                            "penalty": "벌칙",
                            "password": "pass123",
                            "memberLimit": 5
                        }
                        """;

                mockMvc.perform(post("/api/groups")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isCreated());
            }
        }

        @Nested
        @DisplayName("예외")
        class Failure {

            @Test
            @DisplayName("인증 토큰이 없으면 그룹 생성에 실패한다")
            void createGroup_WithoutToken() throws Exception {
                String requestBody = """
                        {
                            "title": "테스트 그룹",
                            "description": "테스트 설명",
                            "deadline": "2026-12-31",
                            "penalty": "벌칙",
                            "password": "pass123",
                            "memberLimit": 5
                        }
                        """;

                mockMvc.perform(post("/api/groups")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("그룹 조회")
    class GetGroup {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("인증된 회원의 ACTIVE 그룹 목록을 반환한다")
            void getGroupList_WithValidToken() throws Exception {
                Member member = memberRepository.save(
                        Member.create(
                                "테스트 회원",
                                "test-group-list",
                                "encoded-password"
                        )
                );

                String accessToken =
                        authTokenService.createAccessToken(member.getId());

                Long groupId = createGroup(accessToken);

                mockMvc.perform(get("/api/groups")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("status", "ACTIVE"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].id").value(groupId.intValue()))
                        .andExpect(jsonPath("$[0].title").value("테스트 그룹"));
            }

            @Test
            @DisplayName("가입한 그룹의 상세 정보를 조회할 수 있다")
            void getGroupDetail_WithValidToken() throws Exception {
                Member member = memberRepository.save(
                        Member.create(
                                "테스트 회원",
                                "test-group-detail",
                                "encoded-password"
                        )
                );

                String accessToken =
                        authTokenService.createAccessToken(member.getId());

                Long groupId = createGroup(accessToken);

                mockMvc.perform(get("/api/groups/{groupId}", groupId)
                                .header("Authorization", "Bearer " + accessToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(groupId.intValue()))
                        .andExpect(jsonPath("$.title").value("테스트 그룹"))
                        .andExpect(jsonPath("$.isJoined").value(true));
            }
        }
    }

    @Nested
    @DisplayName("그룹 수정")
    class UpdateGroup {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("방장은 그룹 정보를 수정할 수 있다")
            void updateGroup_WithOwnerToken() throws Exception {
                String accessToken =
                        createMemberAndGetToken("test-group-update");

                Long groupId = createGroup(accessToken);

                String requestBody = """
                        {
                            "title": "수정된 그룹",
                            "description": "수정된 설명",
                            "deadline": "2026-12-31",
                            "penalty": "수정된 벌칙",
                            "password": "newpass123",
                            "memberLimit": 10
                        }
                        """;

                mockMvc.perform(put("/api/groups/{groupId}", groupId)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isOk());

                mockMvc.perform(get("/api/groups/{groupId}", groupId)
                                .header("Authorization", "Bearer " + accessToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.title").value("수정된 그룹"))
                        .andExpect(jsonPath("$.description").value("수정된 설명"));
            }
        }

        @Nested
        @DisplayName("예외")
        class Failure {

            @Test
            @DisplayName("방장이 아닌 회원은 그룹을 수정할 수 없다")
            void updateGroup_WithNonOwnerToken() throws Exception {
                String ownerToken =
                        createMemberAndGetToken("test-group-owner");

                Long groupId = createGroup(ownerToken);

                String otherMemberToken =
                        createMemberAndGetToken("test-group-non-owner");

                String requestBody = """
                        {
                            "title": "권한 없는 수정",
                            "description": "수정 시도",
                            "deadline": "2026-12-31",
                            "penalty": "벌칙",
                            "password": "pass123",
                            "memberLimit": 5
                        }
                        """;

                mockMvc.perform(put("/api/groups/{groupId}", groupId)
                                .header("Authorization", "Bearer " + otherMemberToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isForbidden());

                mockMvc.perform(get("/api/groups/{groupId}", groupId)
                                .header("Authorization", "Bearer " + ownerToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.title").value("테스트 그룹"));
            }

            @Test
            @DisplayName("인증 토큰이 없으면 그룹 수정에 실패한다")
            void updateGroup_WithoutToken() throws Exception {
                String accessToken =
                        createMemberAndGetToken("test-update-no-token");

                Long groupId = createGroup(accessToken);

                String requestBody = """
                        {
                            "title": "수정된 그룹",
                            "description": "수정된 설명",
                            "deadline": "2026-12-31",
                            "penalty": "벌칙",
                            "password": "pass123",
                            "memberLimit": 5
                        }
                        """;

                mockMvc.perform(put("/api/groups/{groupId}", groupId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("그룹 삭제")
    class DeleteGroup {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("방장은 그룹을 삭제할 수 있다")
            void deleteGroup_WithOwnerToken() throws Exception {
                String accessToken =
                        createMemberAndGetToken("test-group-delete");

                Long groupId = createGroup(accessToken);

                mockMvc.perform(delete("/api/groups/{groupId}", groupId)
                                .header("Authorization", "Bearer " + accessToken))
                        .andExpect(status().isNoContent());

                mockMvc.perform(get("/api/groups/{groupId}", groupId)
                                .header("Authorization", "Bearer " + accessToken))
                        .andExpect(status().isNotFound());
            }
        }

        @Nested
        @DisplayName("예외")
        class Failure {

            @Test
            @DisplayName("방장이 아닌 회원은 그룹을 삭제할 수 없다")
            void deleteGroup_WithNonOwnerToken() throws Exception {
                String ownerToken =
                        createMemberAndGetToken("test-group-delete-owner");

                Long groupId = createGroup(ownerToken);

                String otherMemberToken =
                        createMemberAndGetToken("test-group-delete-other");

                mockMvc.perform(delete("/api/groups/{groupId}", groupId)
                                .header("Authorization", "Bearer " + otherMemberToken))
                        .andExpect(status().isForbidden());

                mockMvc.perform(get("/api/groups/{groupId}", groupId)
                                .header("Authorization", "Bearer " + ownerToken))
                        .andExpect(status().isOk());
            }

            @Test
            @DisplayName("인증 토큰이 없으면 그룹 삭제에 실패한다")
            void deleteGroup_WithoutToken() throws Exception {
                String accessToken =
                        createMemberAndGetToken("test-delete-no-token");

                Long groupId = createGroup(accessToken);

                mockMvc.perform(delete("/api/groups/{groupId}", groupId))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("존재하지 않는 그룹을 삭제하면 실패한다")
            void deleteGroup_WithNonExistentGroup() throws Exception {
                String accessToken =
                        createMemberAndGetToken("test-delete-not-found");

                Long nonExistentGroupId = 999999999L;

                mockMvc.perform(delete(
                                "/api/groups/{groupId}",
                                nonExistentGroupId
                        )
                                .header("Authorization", "Bearer " + accessToken))
                        .andExpect(status().isNotFound());
            }
        }
    }

    private Long createGroup(String accessToken) throws Exception {
        String requestBody = """
                {
                    "title": "테스트 그룹",
                    "description": "테스트 설명",
                    "deadline": "2026-12-31",
                    "penalty": "벌칙",
                    "password": "pass123",
                    "memberLimit": 5
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        Object groupId = new JsonPathExpectationsHelper("$.id")
                .evaluateJsonPath(responseBody);

        return Long.valueOf(groupId.toString());
    }

    private String createMemberAndGetToken(String username) {
        Member member = memberRepository.save(
                Member.create(username, username, "encoded-password")
        );

        return authTokenService.createAccessToken(member.getId());
    }
}
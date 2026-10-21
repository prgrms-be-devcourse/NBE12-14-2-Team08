
package com.back.domain.groupMember.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class GroupMemberControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthTokenService authTokenService;

    @Nested
    @DisplayName("초대 코드로 그룹 조회")
    class GetGroupByInviteCode {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("유효한 초대 코드로 그룹 정보를 조회할 수 있다")
            void getGroupByInviteCode_WithValidToken() throws Exception {
                String accessToken =
                        createMemberAndGetToken("test-invite-lookup");

                Long groupId = createGroup(accessToken);
                String inviteCode = getInviteCode(groupId, accessToken);

                mockMvc.perform(get("/api/groups/invite/{inviteCode}", inviteCode)
                                .header("Authorization", "Bearer " + accessToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(groupId.intValue()))
                        .andExpect(jsonPath("$.inviteCode").value(inviteCode));
            }
        }

        @Nested
        @DisplayName("예외")
        class Failure {

            @Test
            @DisplayName("인증 토큰이 없으면 초대 코드로 그룹을 조회할 수 없다")
            void getGroupByInviteCode_WithoutToken() throws Exception {
                mockMvc.perform(get(
                                "/api/groups/invite/{inviteCode}",
                                "invalid-code"
                        ))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("그룹 가입")
    class JoinGroup {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("유효한 초대 코드와 비밀번호로 그룹에 가입할 수 있다")
            void joinGroup_WithValidToken() throws Exception {
                String ownerToken =
                        createMemberAndGetToken("test-join-owner");

                Long groupId = createGroup(ownerToken);
                String inviteCode = getInviteCode(groupId, ownerToken);

                String memberToken =
                        createMemberAndGetToken("test-join-member");

                String requestBody = """
                        {
                            "password": "pass123"
                        }
                        """;

                mockMvc.perform(post("/api/groups/join/{inviteCode}", inviteCode)
                                .header("Authorization", "Bearer " + memberToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.groupId").value(groupId.intValue()));

                mockMvc.perform(get("/api/groups/{groupId}/members", groupId)
                                .header("Authorization", "Bearer " + memberToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(2));
            }
        }

        @Nested
        @DisplayName("예외")
        class Failure {

            @Test
            @DisplayName("인증 토큰이 없으면 그룹 가입에 실패한다")
            void joinGroup_WithoutToken() throws Exception {
                String requestBody = """
                        {
                            "password": "pass123"
                        }
                        """;

                mockMvc.perform(post(
                                "/api/groups/join/{inviteCode}",
                                "invalid-code"
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("그룹 멤버 목록 조회")
    class GetGroupMembers {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("인증된 회원은 가입한 그룹의 멤버 목록을 조회할 수 있다")
            void getGroupMembers_WithValidToken() throws Exception {
                String accessToken =
                        createMemberAndGetToken("test-group-member-list");

                Long groupId = createGroup(accessToken);

                mockMvc.perform(get("/api/groups/{groupId}/members", groupId)
                                .header("Authorization", "Bearer " + accessToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").isArray());
            }
        }

        @Nested
        @DisplayName("예외")
        class Failure {

            @Test
            @DisplayName("인증 토큰이 없으면 그룹 멤버 목록 조회에 실패한다")
            void getGroupMembers_WithoutToken() throws Exception {
                mockMvc.perform(get("/api/groups/{groupId}/members", 1L))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("그룹 멤버 상세 조회")
    class GetGroupMemberDetail {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("그룹 멤버의 상세 정보를 조회할 수 있다")
            void getGroupMemberDetail_WithValidToken() throws Exception {
                String accessToken =
                        createMemberAndGetToken("test-member-detail");

                Long groupId = createGroup(accessToken);
                Long groupMemberId = getGroupMemberId(groupId, accessToken, 0);

                mockMvc.perform(get(
                                "/api/groups/{groupId}/members/{groupMemberId}",
                                groupId,
                                groupMemberId
                        )
                                .header("Authorization", "Bearer " + accessToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.groupMemberId")
                                .value(groupMemberId.intValue()));
            }
        }

        @Nested
        @DisplayName("예외")
        class Failure {

            @Test
            @DisplayName("인증 토큰이 없으면 멤버 상세 조회에 실패한다")
            void getGroupMemberDetail_WithoutToken() throws Exception {
                mockMvc.perform(get(
                                "/api/groups/{groupId}/members/{groupMemberId}",
                                1L,
                                1L
                        ))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("그룹 탈퇴")
    class LeaveGroup {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("일반 멤버는 가입한 그룹에서 탈퇴할 수 있다")
            void leaveGroup_WithValidToken() throws Exception {
                String ownerToken =
                        createMemberAndGetToken("test-leave-owner");

                Long groupId = createGroup(ownerToken);
                String inviteCode = getInviteCode(groupId, ownerToken);

                String memberToken =
                        createMemberAndGetToken("test-leave-member");

                joinGroup(inviteCode, memberToken);

                mockMvc.perform(delete("/api/groups/{groupId}/leave", groupId)
                                .header("Authorization", "Bearer " + memberToken))
                        .andExpect(status().isOk());

                mockMvc.perform(get("/api/groups/{groupId}/members", groupId)
                                .header("Authorization", "Bearer " + ownerToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(1));
            }
        }

        @Nested
        @DisplayName("예외")
        class Failure {

            @Test
            @DisplayName("인증 토큰이 없으면 그룹 탈퇴에 실패한다")
            void leaveGroup_WithoutToken() throws Exception {
                mockMvc.perform(delete("/api/groups/{groupId}/leave", 1L))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("그룹 멤버 강퇴")
    class KickMember {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("방장은 그룹의 일반 멤버를 강퇴할 수 있다")
            void kickMember_WithOwnerToken() throws Exception {
                String ownerToken =
                        createMemberAndGetToken("test-kick-owner");

                Long groupId = createGroup(ownerToken);
                String inviteCode = getInviteCode(groupId, ownerToken);

                String memberToken =
                        createMemberAndGetToken("test-kick-member");

                joinGroup(inviteCode, memberToken);

                Long targetGroupMemberId =
                        getGroupMemberId(groupId, ownerToken, 1);

                mockMvc.perform(delete(
                                "/api/groups/{groupId}/members/{groupMemberId}/kick",
                                groupId,
                                targetGroupMemberId
                        )
                                .header("Authorization", "Bearer " + ownerToken))
                        .andExpect(status().isNoContent());

                mockMvc.perform(get("/api/groups/{groupId}/members", groupId)
                                .header("Authorization", "Bearer " + ownerToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(1));
            }
        }

        @Nested
        @DisplayName("예외")
        class Failure {

            @Test
            @DisplayName("인증 토큰이 없으면 멤버 강퇴에 실패한다")
            void kickMember_WithoutToken() throws Exception {
                mockMvc.perform(delete(
                                "/api/groups/{groupId}/members/{groupMemberId}/kick",
                                1L,
                                2L
                        ))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("방장 권한 위임")
    class TransferOwner {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("방장은 다른 멤버에게 방장 권한을 위임할 수 있다")
            void transferOwner_WithOwnerToken() throws Exception {
                String ownerToken =
                        createMemberAndGetToken("test-delegate-owner");

                Long groupId = createGroup(ownerToken);
                String inviteCode = getInviteCode(groupId, ownerToken);

                String memberToken =
                        createMemberAndGetToken("test-delegate-member");

                joinGroup(inviteCode, memberToken);

                Long targetGroupMemberId =
                        getGroupMemberId(groupId, ownerToken, 1);

                mockMvc.perform(patch(
                                "/api/groups/{groupId}/members/{groupMemberId}/delegate",
                                groupId,
                                targetGroupMemberId
                        )
                                .header("Authorization", "Bearer " + ownerToken))
                        .andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("예외")
        class Failure {

            @Test
            @DisplayName("인증 토큰이 없으면 방장 권한 위임에 실패한다")
            void transferOwner_WithoutToken() throws Exception {
                mockMvc.perform(patch(
                                "/api/groups/{groupId}/members/{groupMemberId}/delegate",
                                1L,
                                2L
                        ))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    private String createMemberAndGetToken(String username) {
        Member member = memberRepository.save(
                Member.create(username, username, "encoded-password")
        );

        return authTokenService.createAccessToken(member.getId());
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

    private String getInviteCode(Long groupId, String accessToken)
            throws Exception {

        MvcResult result = mockMvc.perform(
                        get("/api/groups/{groupId}", groupId)
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        Object inviteCode = new JsonPathExpectationsHelper("$.inviteCode")
                .evaluateJsonPath(responseBody);

        return inviteCode.toString();
    }

    private Long getGroupMemberId(
            Long groupId,
            String accessToken,
            int index
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        get("/api/groups/{groupId}/members", groupId)
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        Object groupMemberId = new JsonPathExpectationsHelper(
                "$[" + index + "].groupMemberId"
        ).evaluateJsonPath(responseBody);

        return Long.valueOf(groupMemberId.toString());
    }

    private void joinGroup(String inviteCode, String accessToken)
            throws Exception {

        String requestBody = """
                {
                    "password": "pass123"
                }
                """;

        mockMvc.perform(post("/api/groups/join/{inviteCode}", inviteCode)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }
}
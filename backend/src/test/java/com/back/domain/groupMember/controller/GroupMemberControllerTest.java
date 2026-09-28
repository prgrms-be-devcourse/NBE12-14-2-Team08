package com.back.domain.groupMember.controller;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.entity.GroupStatus;
import com.back.domain.group.service.GroupService;
import com.back.domain.groupMember.dto.GroupMemberResponse;
import com.back.domain.groupMember.service.GroupMemberService;
import com.back.global.security.LoginMemberId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GroupMemberControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private GroupMemberController groupMemberController;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Long testMemberId = 1L;
    private final Long testGroupId = 100L;
    private final String testInviteCode = "abcdefgh";

    @BeforeEach
    void setUp() {
        HandlerMethodArgumentResolver loginMemberIdArgumentResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(LoginMemberId.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return testMemberId;
            }
        };

        this.mockMvc = MockMvcBuilders.standaloneSetup(groupMemberController)
                .setCustomArgumentResolvers(loginMemberIdArgumentResolver)
                .build();
    }

    @Test
    @DisplayName("초대 코드로 그룹 상세 정보 조회 - 성공 (200 OK)")
    void getGroupByInviteCode_Success() throws Exception {
        // given
        GroupResponse.Detail response = new GroupResponse.Detail(
                testGroupId, "테스트 그룹", "설명", LocalDate.now(), LocalDate.now().plusDays(7),
                "벌칙", testInviteCode, 5, LocalDateTime.now(), "http://base.url" + testInviteCode,
                GroupStatus.ACTIVE, true
        );
        given(groupService.getGroupByInviteCode(testInviteCode, testMemberId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/groups/join/{inviteCode}", testInviteCode)
                        .header("Authorization", "Bearer mock-jwt-token"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testGroupId))
                .andExpect(jsonPath("$.inviteCode").value(testInviteCode))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.isJoined").value(true));
    }

    @Test
    @DisplayName("그룹 가입 요청 - 성공 (200 OK)")
    void joinGroup_Success() throws Exception {
        // given
        GroupRequest.Join request = new GroupRequest.Join("password123");
        given(groupMemberService.joinGroup(eq(testInviteCode), eq(testMemberId), any(GroupRequest.Join.class)))
                .willReturn(testGroupId);

        // when & then
        mockMvc.perform(post("/api/groups/join/{inviteCode}", testInviteCode)
                        .header("Authorization", "Bearer mock-jwt-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(testGroupId));
    }

    @Test
    @DisplayName("그룹 내 전체 가입 멤버 조회 - 성공 (200 OK)")
    void getGroupMembers_Success() throws Exception {
        // given
        given(groupMemberService.getGroupMembers(testGroupId, testMemberId)).willReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/api/groups/{groupId}/members", testGroupId)
                        .header("Authorization", "Bearer mock-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("특정 그룹 멤버 상세 조회 - 성공 (200 OK)")
    void getGroupMemberDetail_Success() throws Exception {
        // given
        Long targetGroupMemberId = 50L;
        GroupMemberResponse.Detail response = new GroupMemberResponse.Detail(targetGroupMemberId, testMemberId, "유저이름", "아이디", "MEMBER", 0);
        given(groupMemberService.getGroupMemberDetail(testGroupId, targetGroupMemberId, testMemberId))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/groups/{groupId}/members/{groupMemberId}", testGroupId, targetGroupMemberId)
                        .header("Authorization", "Bearer mock-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupMemberId").value(targetGroupMemberId))
                .andExpect(jsonPath("$.memberId").value(testMemberId));
    }

    @Test
    @DisplayName("그룹 자발적 탈퇴 - 성공 (200 OK)")
    void leaveGroup_Success() throws Exception {
        // given
        doNothing().when(groupMemberService).leaveGroup(testGroupId, testMemberId);

        // when & then
        mockMvc.perform(delete("/api/groups/{groupId}/leave", testGroupId)
                        .header("Authorization", "Bearer mock-jwt-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("팀원에 대한 방장의 강제 추방 - 성공 (244 NO CONTENT)")
    void kickMember_Success() throws Exception {
        // given
        Long targetGroupMemberId = 50L;
        doNothing().when(groupMemberService).kickMember(testGroupId, targetGroupMemberId, testMemberId);

        // when & then
        mockMvc.perform(delete("/api/groups/{groupId}/members/{groupMemberId}/kick", testGroupId, targetGroupMemberId)
                        .header("Authorization", "Bearer mock-jwt-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("방장 권한 다음 멤버에게 위임 - 성공 (200 OK)")
    void transferOwner_Success() throws Exception {
        // given
        Long targetGroupMemberId = 50L;
        doNothing().when(groupMemberService).transferOwner(testGroupId, targetGroupMemberId, testMemberId);

        // when & then
        mockMvc.perform(patch("/api/groups/{groupId}/members/{groupMemberId}/delegate", testGroupId, targetGroupMemberId)
                        .header("Authorization", "Bearer mock-jwt-token"))
                .andExpect(status().isOk());
    }
}

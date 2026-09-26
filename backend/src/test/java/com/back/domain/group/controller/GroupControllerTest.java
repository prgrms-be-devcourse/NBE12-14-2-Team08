package com.back.domain.group.controller;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.entity.GroupStatus;
import com.back.domain.group.service.GroupService;
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
class GroupControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private GroupController groupController;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Long testMemberId = 1L;

    @BeforeEach
    void setUp() {
        // @LoginMemberId 가짜 주입용 Resolver 세팅 (JWT 필터 대체)
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

        this.mockMvc = MockMvcBuilders.standaloneSetup(groupController)
                .setCustomArgumentResolvers(loginMemberIdArgumentResolver)
                .build();
    }

    @Test
    @DisplayName("그룹 생성 - 성공 (201 CREATED)")
    void createGroup_Success() throws Exception {
        GroupRequest.Create request = new GroupRequest.Create(
                "테스트 그룹", "설명", LocalDate.now().plusDays(7), "벌칙", "password123", 5
        );
        GroupResponse.Detail response = new GroupResponse.Detail(
                1L, "테스트 그룹", "설명", LocalDate.now(), LocalDate.now().plusDays(7),
                "벌칙", "INVITE12", 5, LocalDateTime.now(), "http://base.url"
        );

        given(groupService.createGroup(eq(testMemberId), any(GroupRequest.Create.class))).willReturn(response);

        mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer mock-jwt-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("테스트 그룹"));
    }

    @Test
    @DisplayName("그룹 생성 - 유효성 검증 실패 (400 BAD REQUEST)")
    void createGroup_ValidationFailed() throws Exception {
        GroupRequest.Create invalidRequest = new GroupRequest.Create("", "설명", null, "벌칙", "", 0);

        mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer mock-jwt-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("그룹 목록 조회 - 성공 (200 OK)")
    void getGroupSimpleList_Success() throws Exception {
        given(groupService.getGroupSimpleList(testMemberId, GroupStatus.ACTIVE)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/groups")
                        .header("Authorization", "Bearer mock-jwt-token")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("그룹 상세 조회 - 성공 (200 OK)")
    void getGroupDetail_Success() throws Exception {
        Long groupId = 1L;
        GroupResponse.Detail response = new GroupResponse.Detail(
                groupId, "테스트 그룹", "설명", LocalDate.now(), LocalDate.now().plusDays(7),
                "벌칙", "INVITE12", 5, LocalDateTime.now(), "http://base.url"
        );

        given(groupService.getGroupDetail(groupId, testMemberId)).willReturn(response);

        mockMvc.perform(get("/api/groups/{groupId}", groupId)
                        .header("Authorization", "Bearer mock-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(groupId));
    }

    @Test
    @DisplayName("그룹 수정 - 성공 (200 OK)")
    void updateGroup_Success() throws Exception {
        Long groupId = 1L;
        GroupRequest.Update request = new GroupRequest.Update("수정", "설명", null, "벌칙", "pass", 10);

        doNothing().when(groupService).updateGroup(eq(groupId), eq(testMemberId), any(GroupRequest.Update.class));

        mockMvc.perform(put("/api/groups/{groupId}", groupId)
                        .header("Authorization", "Bearer mock-jwt-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("그룹 삭제 - 성공 (244 NO CONTENT)")
    void deleteGroup_Success() throws Exception {
        Long groupId = 1L;
        doNothing().when(groupService).deleteGroup(groupId, testMemberId);

        mockMvc.perform(delete("/api/groups/{groupId}", groupId)
                        .header("Authorization", "Bearer mock-jwt-token"))
                .andExpect(status().isNoContent());
    }
}

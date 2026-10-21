package com.back.domain.habit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.dto.CreateHabitRequest;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.entity.HabitStatus;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.member.dto.CreateMemberRequest;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.service.AuthTokenService;
import com.back.domain.penaltyverify.entity.PenaltyVerify;
import com.back.domain.penaltyverify.entity.PenaltyVerifyStatus;
import com.back.domain.penaltyverify.repository.PenaltyVerifyRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// 습관 생성·포기·조회 API 통합 테스트. (주간 판정은 HabitWeeklyCheckControllerTest, 상세 조회는 HabitDetailControllerTest)
// 메모리 DB(test 프로필)를 쓰고 테스트 종료 시 롤백한다.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HabitControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;
    @Autowired private HabitRepository habitRepository;
    @Autowired private PenaltyVerifyRepository penaltyVerifyRepository;

    private Long groupId;
    private String ownerToken;     // 방장 (습관 없음)
    private Long memberId;
    private String memberToken;    // 일반 멤버 (활성 습관 "물 마시기" 1개 보유)
    private Long habitId;

    @BeforeEach
    void setUp() throws Exception {

        // 1. 방장 멤버 생성 -> 로그인 토큰 발급
        Long ownerId = createMember("owner01", "방장");
        ownerToken = authTokenService.createAccessToken(ownerId);

        // 2. 방장이 그룹 생성 (그룹 생성 시 방장은 자동으로 OWNER GroupMember가 됨)
        // deadline은 null이면 가입 시 서비스에서 NPE가 나므로 미래 날짜로 지정한다.
        String createGroupBody = jsonMapper.writeValueAsString(
                new GroupRequest.Create(
                        "테스트 그룹", "설명", LocalDate.now().plusMonths(1), "커피 사기",
                        "1234", 5
                )
        );

        String groupResponse = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createGroupBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode groupNode = jsonMapper.readTree(groupResponse);
        groupId = groupNode.get("id").asLong();
        String inviteCode = groupNode.get("inviteCode").asText();

        // 3. 일반 멤버 생성 -> 로그인 토큰 발급
        memberId = createMember("member01", "멤버1");
        memberToken = authTokenService.createAccessToken(memberId);

        // 4. 일반 멤버가 초대 코드 + 그룹 비밀번호로 그룹 가입
        String joinBody = jsonMapper.writeValueAsString(
                new GroupRequest.Join("1234")
        );

        mockMvc.perform(post("/api/groups/join/{inviteCode}", inviteCode)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody))
                .andExpect(status().isOk());

        // 5. 가입한 멤버가 습관 생성 (주 3일 인증 필요)
        String createHabitBody = jsonMapper.writeValueAsString(
                new CreateHabitRequest("물 마시기", "하루 2L 마시기", 3)
        );

        String habitResponse = mockMvc.perform(post("/api/groups/{groupId}/habits", groupId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHabitBody))
                .andReturn().getResponse().getContentAsString();

        habitId = jsonMapper.readTree(habitResponse).get("id").asLong();
    }

    private Long createMember(String username, String nickname) throws Exception {
        String body = jsonMapper.writeValueAsString(
                new CreateMemberRequest(nickname, username, "password1234")
        );

        String response = mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return jsonMapper.readTree(response).get("memberId").asLong();
    }

    private ResultActions createHabit(String token, Object request) throws Exception {
        return mockMvc.perform(post("/api/groups/{groupId}/habits", groupId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));
    }

    private ResultActions failHabit(String token, Long targetHabitId) throws Exception {
        return mockMvc.perform(post("/api/habits/fail/{habitId}", targetHabitId)
                .header("Authorization", "Bearer " + token));
    }

    private void finishGroup() {
        Group group = groupRepository.findById(groupId).orElseThrow();
        group.finish();
        groupRepository.saveAndFlush(group);
    }
}

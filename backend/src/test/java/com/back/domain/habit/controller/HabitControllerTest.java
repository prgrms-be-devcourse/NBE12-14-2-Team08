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

    @Nested
    @DisplayName("습관 생성 POST /api/groups/{groupId}/habits")
    class CreateHabitApi {

        static Stream<Arguments> invalidRequests() {
            return Stream.of(
                    Arguments.of("제목이 빈 문자열", new CreateHabitRequest("", "설명", 3)),
                    Arguments.of("제목이 없음", new CreateHabitRequest(null, "설명", 3)),
                    Arguments.of("실천 일수가 0", new CreateHabitRequest("운동", "설명", 0)),
                    Arguments.of("실천 일수가 8", new CreateHabitRequest("운동", "설명", 8))
            );
        }

        @Test
        @DisplayName("활성 습관이 없는 멤버가 생성하면 ACTIVE 습관이 만들어진다")
        void 생성_성공() throws Exception {
            createHabit(ownerToken, new CreateHabitRequest("독서 30분", "책 읽기", 5))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("독서 30분"))
                    .andExpect(jsonPath("$.description").value("책 읽기"))
                    .andExpect(jsonPath("$.days").value(5))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            // 방장 1개 + setUp에서 만든 멤버 1개
            assertThat(habitRepository.findAllByGroupMember_Group_IdAndStatus(groupId, HabitStatus.ACTIVE))
                    .hasSize(2);
        }

        @Test
        @DisplayName("이미 활성 습관이 있으면 409를 반환하고 습관이 더 생기지 않는다")
        void 활성_습관이_이미_있으면_409() throws Exception {
            createHabit(memberToken, new CreateHabitRequest("독서 30분", "책 읽기", 5))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("이미 활성화된 습관이 존재합니다."));

            assertThat(habitRepository.findAllByGroupMember_Group_IdAndStatus(groupId, HabitStatus.ACTIVE))
                    .hasSize(1);
        }

        @ParameterizedTest(name = "{0}이면 400")
        @MethodSource("invalidRequests")
        @DisplayName("요청 값이 올바르지 않으면 400을 반환하고 습관을 만들지 않는다")
        void 잘못된_요청은_400(String ignoredCase, CreateHabitRequest request) throws Exception {
            createHabit(ownerToken, request)
                    .andExpect(status().isBadRequest());

            assertThat(habitRepository.findAllByGroupMember_Group_IdAndStatus(groupId, HabitStatus.ACTIVE))
                    .hasSize(1);
        }

        @Test
        @DisplayName("그룹 멤버가 아니면 400을 반환한다")
        void 그룹_멤버가_아니면_400() throws Exception {
            String otherToken = authTokenService.createAccessToken(createMember("other01", "다른멤버"));

            createHabit(otherToken, new CreateHabitRequest("독서 30분", "책 읽기", 5))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("해당 그룹의 멤버를 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("종료된 그룹에서는 400을 반환하고 습관을 만들지 않는다")
        void 종료된_그룹이면_400() throws Exception {
            finishGroup();

            createHabit(ownerToken, new CreateHabitRequest("독서 30분", "책 읽기", 5))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("이미 종료된 그룹에서는 습관을 추가할 수 없습니다."));

            assertThat(habitRepository.findAllByGroupMember_Group_IdAndStatus(groupId, HabitStatus.ACTIVE))
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("습관 포기 POST /api/habits/fail/{habitId}")
    class FailHabitApi {

        @Test
        @DisplayName("본인 습관을 포기하면 FAILED가 되고 REQUIRED 벌칙이 그룹의 벌칙 문구로 생성된다")
        void 포기_성공() throws Exception {
            failHabit(memberToken, habitId).andExpect(status().isOk());

            Habit habit = habitRepository.findById(habitId).orElseThrow();
            assertThat(habit.getStatus()).isEqualTo(HabitStatus.FAILED);

            PenaltyVerify penalty = penaltyVerifyRepository.findByHabitId(habitId).orElseThrow();
            assertThat(penalty.getStatus()).isEqualTo(PenaltyVerifyStatus.REQUIRED);
            assertThat(penalty.getPenaltyText()).isEqualTo("커피 사기");
            assertThat(penalty.getHabitTitle()).isEqualTo("물 마시기");
        }

        @Test
        @DisplayName("포기하면 활성 습관이 없어지고, 벌칙과 무관하게 새 습관을 바로 만들 수 있다")
        void 포기_후_새_습관_생성_가능() throws Exception {
            failHabit(memberToken, habitId).andExpect(status().isOk());

            mockMvc.perform(get("/api/groups/{groupId}/habits/active", groupId)
                            .header("Authorization", "Bearer " + memberToken))
                    .andExpect(status().isBadRequest());

            createHabit(memberToken, new CreateHabitRequest("독서 30분", "책 읽기", 5))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("다른 사람의 습관은 포기시킬 수 없다 (400, 상태와 벌칙은 그대로)")
        void 타인의_습관이면_400() throws Exception {
            failHabit(ownerToken, habitId).andExpect(status().isBadRequest());

            assertThat(habitRepository.findById(habitId).orElseThrow().getStatus())
                    .isEqualTo(HabitStatus.ACTIVE);
            assertThat(penaltyVerifyRepository.findByHabitId(habitId)).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 습관이면 400을 반환한다")
        void 없는_습관이면_400() throws Exception {
            failHabit(memberToken, 999_999L).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("이미 포기한 습관을 다시 포기하면 409를 반환하고 벌칙이 더 생기지 않는다")
        void 이미_포기한_습관이면_409() throws Exception {
            failHabit(memberToken, habitId).andExpect(status().isOk());
            long penaltyCountAfterFirstFail = penaltyVerifyRepository.count();

            failHabit(memberToken, habitId).andExpect(status().isConflict());

            assertThat(penaltyVerifyRepository.count()).isEqualTo(penaltyCountAfterFirstFail);
        }

        @Test
        @DisplayName("종료된 그룹에서는 포기할 수 없다 (400, 습관은 ACTIVE 그대로)")
        void 종료된_그룹이면_400() throws Exception {
            finishGroup();

            failHabit(memberToken, habitId)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("이미 종료된 그룹의 습관은 포기할 수 없습니다."));

            assertThat(habitRepository.findById(habitId).orElseThrow().getStatus())
                    .isEqualTo(HabitStatus.ACTIVE);
            assertThat(penaltyVerifyRepository.findByHabitId(habitId)).isEmpty();
        }
    }
}

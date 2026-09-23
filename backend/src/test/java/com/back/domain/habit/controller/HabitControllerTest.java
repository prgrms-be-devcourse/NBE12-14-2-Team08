package com.back.domain.habit.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.habit.dto.CreateHabitRequest;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.habitVerify.entity.HabitVerify;
import com.back.domain.habitVerify.entity.HabitVerifyStatus;
import com.back.domain.habitVerify.repository.HabitVerifyRepository;
import com.back.domain.member.dto.CreateMemberRequest;
import com.back.domain.member.service.AuthTokenService;
import com.back.domain.penaltyverify.repository.PenaltyVerifyRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HabitControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;
    @Autowired private HabitRepository habitRepository;
    @Autowired private HabitVerifyRepository habitVerifyRepository;
    @Autowired private PenaltyVerifyRepository penaltyVerifyRepository;

    private Long groupId;
    private String ownerToken;
    private Long memberId;
    private String memberToken;
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

    // 그룹 생성일 요일을 오늘로, 멤버 가입일을 10일 전으로 되돌려
    // "가입 1주 경과 + 이번 주 마감 지남" 상태를 만든다. (습관 생성일은 건드리지 않음)
    private void backdateGroupAndMembership() {
        LocalDateTime now = LocalDateTime.now();

        Group group = groupRepository.findById(groupId).orElseThrow();
        ReflectionTestUtils.setField(group, "createDate", now.minusDays(14));
        groupRepository.saveAndFlush(group);

        GroupMember groupMember = groupMemberRepository
                .findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow();
        ReflectionTestUtils.setField(groupMember, "createDate", now.minusDays(10));
        groupMemberRepository.saveAndFlush(groupMember);
    }

    // 위 조건에 더해, 습관 생성일도 10일 전으로 되돌려 "습관이 이번 판정 구간보다 먼저부터
    // 존재했음"까지 만족시킨다 - PASSED/PENDING_REVIEW/FAILED 판정에 도달하기 위한 전체 조건.
    private void backdateForPastDeadline() {
        backdateGroupAndMembership();

        Habit habit = habitRepository.findById(habitId).orElseThrow();
        ReflectionTestUtils.setField(habit, "createDate", LocalDateTime.now().minusDays(10));
        habitRepository.saveAndFlush(habit);
    }

    // 이번 주 판정 구간(오늘 00시 이전) 안에 들어오도록 제출 시각을 3일 전으로 되돌린 인증 기록 생성
    private void createBackdatedVerify(LocalDate verifyDate, HabitVerifyStatus status) {
        Habit habit = habitRepository.findById(habitId).orElseThrow();

        // HabitVerify.create는 항상 PENDING으로 생성하므로, APPROVED가 필요하면 승인 처리까지 이어서 한다.
        HabitVerify habitVerify = HabitVerify.create(
                habit, verifyDate, "테스트 인증", "https://x.com/a.jpg"
        );
        if (status == HabitVerifyStatus.APPROVED) {
            habitVerify.approve();
        }
        habitVerifyRepository.save(habitVerify);

        ReflectionTestUtils.setField(habitVerify, "createDate", LocalDateTime.now().minusDays(3));
        habitVerifyRepository.saveAndFlush(habitVerify);
    }

    @Test
    @DisplayName("가입한 지 1주가 안 되면 TOO_EARLY를 반환하고 습관은 그대로 유지된다")
    void 가입_1주_미만이면_TOO_EARLY() throws Exception {
        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].habitId").value(habitId))
                .andExpect(jsonPath("$[0].result").value("TOO_EARLY"))
                .andExpect(jsonPath("$[0].habitStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("이미 실패 처리된 습관은 그룹 판정 대상(활성 습관)에서 제외된다")
    void 이미_실패한_습관은_판정_대상에서_제외() throws Exception {
        Habit habit = habitRepository.findById(habitId).orElseThrow();
        habit.fail();
        habitRepository.saveAndFlush(habit);

        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("가입 1주는 지났지만 습관이 이번 판정 구간 마감 이후에 생성됐다면 BEFORE_DEADLINE을 반환한다")
    void 습관_생성이_마감_이후면_BEFORE_DEADLINE() throws Exception {
        // 그룹/가입일만 되돌리고, 습관은 방금(@BeforeEach) 생성된 상태 그대로 둔다
        backdateGroupAndMembership();

        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("BEFORE_DEADLINE"))
                .andExpect(jsonPath("$[0].habitStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("승인된 인증 횟수가 days 이상이면 PASSED를 반환하고 습관은 ACTIVE로 유지된다")
    void 승인_횟수가_충분하면_PASSED() throws Exception {
        backdateForPastDeadline();

        createBackdatedVerify(LocalDate.now().minusDays(1), HabitVerifyStatus.APPROVED);
        createBackdatedVerify(LocalDate.now().minusDays(2), HabitVerifyStatus.APPROVED);
        createBackdatedVerify(LocalDate.now().minusDays(3), HabitVerifyStatus.APPROVED);

        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("PASSED"))
                .andExpect(jsonPath("$[0].habitStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("미검토(PENDING) 건이 남아있으면 승인 횟수와 무관하게 PENDING_REVIEW로 보류된다")
    void 미검토_건이_있으면_PENDING_REVIEW() throws Exception {
        backdateForPastDeadline();

        createBackdatedVerify(LocalDate.now().minusDays(1), HabitVerifyStatus.APPROVED);
        createBackdatedVerify(LocalDate.now().minusDays(2), HabitVerifyStatus.APPROVED);
        createBackdatedVerify(LocalDate.now().minusDays(3), HabitVerifyStatus.APPROVED);
        createBackdatedVerify(LocalDate.now().minusDays(4), HabitVerifyStatus.PENDING);

        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$[0].habitStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("검토가 끝났는데도 days 미달이면 실패 처리되고 벌칙이 확정된다")
    void 승인_횟수가_부족하면_실패_및_벌칙_확정() throws Exception {
        backdateForPastDeadline();

        createBackdatedVerify(LocalDate.now().minusDays(1), HabitVerifyStatus.APPROVED);

        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("FAILED_INSUFFICIENT_SUBMISSION"))
                .andExpect(jsonPath("$[0].habitStatus").value("FAILED"));

        boolean penaltyCreated = penaltyVerifyRepository.findByHabitId(habitId).isPresent();
        Assertions.assertTrue(penaltyCreated);
    }

    @Test
    @DisplayName("방장이 아닌 일반 멤버가 호출하면 403을 반환한다")
    void 방장이_아니면_403() throws Exception {
        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("그룹 멤버가 아닌 사람이 호출하면 400을 반환한다")
    void 그룹_멤버가_아니면_400() throws Exception {
        Long otherId = createMember("other01", "다른멤버");
        String otherToken = authTokenService.createAccessToken(otherId);

        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isBadRequest());
    }
}

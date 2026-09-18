package com.back.domain.penaltyverify.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.penaltyverify.entity.PenaltyVerify;
import com.back.domain.penaltyverify.repository.PenaltyVerifyRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PenaltyVerifyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;
    @Autowired private HabitRepository habitRepository;
    @Autowired private PenaltyVerifyRepository penaltyVerifyRepository;

    private Long memberId;
    private Long groupId;
    private Long groupMemberId;
    private Long habitId;
    private Long submittedPenaltyVerifyId;

    @BeforeEach
    void setUp() {
        Group group = groupRepository.save(
            Group.builder()
                .title("테스트 그룹")
                .penalty("커피사기")
                .password("1234")
                .inviteCode("TESTCODE")
                .memberLimit(5)
                .build()
        );
        groupId = group.getId();

        // 1. 유저 A: 습관 1개 생성 -> REQUIRED 벌칙 발급 (제출 테스트 & 본인 목록 조회 테스트용)
        Member memberA = memberRepository.save(
            Member.create("유저A", "usera", "password")
        );
        memberId = memberA.getId();

        GroupMember groupMemberA = groupMemberRepository.save(
            GroupMember.create(group, memberA, GroupMemberRole.OWNER)
        );
        groupMemberId = groupMemberA.getId();

        Habit habitA = habitRepository.save(
            Habit.create(groupMemberA, "기상 후 운동", 30)
        );
        habitId = habitA.getId();

        penaltyVerifyRepository.save(PenaltyVerify.create(groupMemberA, habitA));

        // 2. 유저 B: 별도 멤버로 습관 1개 생성 -> PENDING 벌칙 등록 (상세 조회 테스트용, Habit 1:1 충돌 방지)
        Member memberB = memberRepository.save(
            Member.create("유저B", "userb", "password")
        );

        GroupMember groupMemberB = groupMemberRepository.save(
            GroupMember.create(group, memberB, GroupMemberRole.MEMBER)
        );

        Habit habitB = habitRepository.save(
            Habit.create(groupMemberB, "물 마시기", 30)
        );

        PenaltyVerify submittedPenalty = PenaltyVerify.create(groupMemberB, habitB);
        submittedPenalty.submit(LocalDate.now(), "미리 만들어둔 벌칙 인증", "https://x.com/existing.jpg");
        submittedPenaltyVerifyId = penaltyVerifyRepository.save(submittedPenalty).getId();
    }

    @Test
    @DisplayName("사전 발급된 REQUIRED 벌칙에 대해 X-User-Id 헤더로 인증 제출 성공")
    void X_User_Id_헤더로_벌칙_제출() throws Exception {
        String body = jsonMapper.writeValueAsString(
            new SubmitPenaltyVerifyRequestTest("운동 완료했습니다", "https://x.com/a.jpg")
        );

        mockMvc.perform(post("/api/habits/{habitId}/penalties", habitId)
                .header("X-User-Id", memberId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.description").value("운동 완료했습니다"))
            .andExpect(jsonPath("$.verifyDate").isNotEmpty());
    }

    @Test
    @DisplayName("존재하는 벌칙 인증 상세 조회 성공")
    void 존재하는_벌칙_인증_상세_조회_성공() throws Exception {
        mockMvc.perform(get("/api/penalties/{id}", submittedPenaltyVerifyId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.description").value("미리 만들어둔 벌칙 인증"));
    }

    @Test
    @DisplayName("특정 그룹 멤버의 전체 벌칙 기록 목록 최신순 조회 성공")
    void 그룹_멤버별_벌칙_목록_조회_성공() throws Exception {
        mockMvc.perform(get("/api/groups/{groupId}/members/{groupMemberId}/penalties", groupId, groupMemberId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].habitTitle").value("기상 후 운동"))
            .andExpect(jsonPath("$[0].status").value("REQUIRED"));
    }

    record SubmitPenaltyVerifyRequestTest(String description, String imageUrl) {}
}
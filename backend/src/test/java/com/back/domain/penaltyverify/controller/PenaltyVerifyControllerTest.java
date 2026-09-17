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
    private Long habitId;
    private Long penaltyVerifyId;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(
            Member.create( "테스트유저", "testuser","password")
        );
        memberId = member.getId();

        Group group = groupRepository.save(
            Group.builder()
                .title("테스트 그룹")
                .penalty("커피사기")
                .password("1234")
                .inviteCode("TESTCODE")
                .memberLimit(5)
                .build()
        );

        GroupMember groupMember = groupMemberRepository.save(
            GroupMember.create(group, member, GroupMemberRole.OWNER)
        );

        Habit habit = habitRepository.save(
            Habit.create(groupMember, "기상 후 운동", 30)
        );
        habitId = habit.getId();

        // 상세 조회 테스트용 PenaltyVerify 미리 하나 생성
        PenaltyVerify penaltyVerify = penaltyVerifyRepository.save(
            PenaltyVerify.submit(
                groupMember, habit, LocalDate.now(),
                habit.getTitle(), group.getPenalty(),
                "미리 만들어둔 벌칙 인증", "https://x.com/existing.jpg"
            )
        );
        penaltyVerifyId = penaltyVerify.getId();
    }

    @Test
    void X_User_Id_헤더로_벌칙_제출() throws Exception {
        String body = jsonMapper.writeValueAsString(
            new SubmitPenaltyVerifyRequestTest("2026-09-18", "운동함", "https://x.com/a.jpg")
        );

        mockMvc.perform(post("/api/habits/{habitId}/penalties", habitId)
                .header("X-User-Id", memberId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 존재하는_벌칙_인증_상세_조회_성공() throws Exception {
        mockMvc.perform(get("/api/penalties/{id}", penaltyVerifyId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.description").value("미리 만들어둔 벌칙 인증"));
    }

    record SubmitPenaltyVerifyRequestTest(String verifyDate, String description, String imageUrl) {}
}
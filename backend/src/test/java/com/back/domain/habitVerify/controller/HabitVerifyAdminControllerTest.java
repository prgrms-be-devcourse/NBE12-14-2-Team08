package com.back.domain.habitVerify.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.habitVerify.entity.HabitVerify;
import com.back.domain.habitVerify.entity.HabitVerifyStatus;
import com.back.domain.habitVerify.repository.HabitVerifyRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.service.AuthTokenService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// 일괄 승인의 "하나라도 실패하면 전부 취소"를 실제 트랜잭션으로 확인한다.
// 테스트 클래스에 @Transactional을 붙이면 서비스가 테스트 트랜잭션에 합류해 롤백 여부를 볼 수 없으므로 붙이지 않고,
// 대신 만든 데이터를 @AfterEach에서 직접 지운다. (메모리 DB, test 프로필)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HabitVerifyAdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;
    @Autowired private HabitRepository habitRepository;
    @Autowired private HabitVerifyRepository habitVerifyRepository;

    private Member owner;
    private Member member;
    private Group group;
    private GroupMember ownerGroupMember;
    private GroupMember memberGroupMember;
    private Habit habit;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        owner = memberRepository.save(Member.create("방장", uniqueName("owner"), "pass1234"));
        member = memberRepository.save(Member.create("일반회원", uniqueName("member"), "pass1234"));
        group = groupRepository.save(Group.builder()
            .title("아침 기상 챌린지")
            .penalty("커피 1잔 쏘기")
            .password("1234")
            .inviteCode(uniqueName("code"))
            .memberLimit(10)
            .deadline(LocalDate.now().plusMonths(1))
            .build());
        ownerGroupMember = groupMemberRepository.save(GroupMember.create(group, owner, GroupMemberRole.OWNER));
        memberGroupMember = groupMemberRepository.save(GroupMember.create(group, member, GroupMemberRole.MEMBER));
        habit = habitRepository.save(Habit.create(memberGroupMember, "기상 후 스트레칭", "10분 스트레칭", 3));
        ownerToken = authTokenService.createAccessToken(owner.getId());
    }

    @AfterEach
    void tearDown() {
        habitVerifyRepository.deleteAll(habitVerifyRepository.findAllByHabitIdOrderByVerifyDateDesc(habit.getId()));
        habitRepository.delete(habit);
        groupMemberRepository.deleteAll(List.of(memberGroupMember, ownerGroupMember));
        groupRepository.delete(group);
        memberRepository.deleteAll(List.of(member, owner));
    }

    private String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private HabitVerify saveVerify(LocalDate verifyDate, HabitVerifyStatus status) {
        HabitVerify habitVerify = HabitVerify.create(habit, verifyDate, "인증", "https://example.com/a.png");
        if (status == HabitVerifyStatus.APPROVED) {
            habitVerify.approve();
        }
        return habitVerifyRepository.save(habitVerify);
    }

    private HabitVerifyStatus statusInDb(HabitVerify habitVerify) {
        return habitVerifyRepository.findById(habitVerify.getId()).orElseThrow().getStatus();
    }

    @Test
    @DisplayName("일괄 승인: 모두 검토 대기면 200이고 DB에 모두 APPROVED로 저장된다")
    void 모두_대기면_전부_승인() throws Exception {
        HabitVerify first = saveVerify(LocalDate.of(2026, 10, 20), HabitVerifyStatus.PENDING);
        HabitVerify second = saveVerify(LocalDate.of(2026, 10, 21), HabitVerifyStatus.PENDING);

        mockMvc.perform(patch("/api/groups/{groupId}/habits/verifications/bulk-approve", group.getId())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\": [" + first.getId() + ", " + second.getId() + "]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        assertThat(statusInDb(first)).isEqualTo(HabitVerifyStatus.APPROVED);
        assertThat(statusInDb(second)).isEqualTo(HabitVerifyStatus.APPROVED);
    }

    @Test
    @DisplayName("일괄 승인: 하나라도 이미 처리된 인증이면 409이고, 앞서 승인하던 인증도 DB에서 PENDING 그대로다 (전부 취소)")
    void 하나라도_처리됐으면_전부_취소() throws Exception {
        HabitVerify pending = saveVerify(LocalDate.of(2026, 10, 20), HabitVerifyStatus.PENDING);
        HabitVerify approved = saveVerify(LocalDate.of(2026, 10, 21), HabitVerifyStatus.APPROVED);

        mockMvc.perform(patch("/api/groups/{groupId}/habits/verifications/bulk-approve", group.getId())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\": [" + pending.getId() + ", " + approved.getId() + "]}"))
            .andExpect(status().isConflict());

        assertThat(statusInDb(pending)).isEqualTo(HabitVerifyStatus.PENDING);
        assertThat(statusInDb(approved)).isEqualTo(HabitVerifyStatus.APPROVED);
    }
}

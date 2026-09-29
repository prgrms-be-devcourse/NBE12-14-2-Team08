package com.back.domain.penaltyverify.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.service.AuthTokenService;
import com.back.domain.penaltyverify.dto.BulkActionRequest;
import com.back.domain.penaltyverify.dto.SubmitPenaltyVerifyRequest;
import com.back.domain.penaltyverify.entity.PenaltyVerify;
import com.back.domain.penaltyverify.entity.PenaltyVerifyStatus;
import com.back.domain.penaltyverify.repository.PenaltyVerifyRepository;
import java.time.LocalDate;
import java.util.List;
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
    @Autowired private AuthTokenService authTokenService;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;
    @Autowired private HabitRepository habitRepository;
    @Autowired private PenaltyVerifyRepository penaltyVerifyRepository;

    private Long ownerId;
    private String ownerToken;
    private Long memberId;
    private String memberToken;

    private Long groupId;
    private Long memberGroupMemberId;
    private Long memberHabitId;
    private Long pendingPenaltyId;
    private Long rejectedPenaltyId;

    @BeforeEach
    void setUp() {
        Group group = groupRepository.save(
            Group.builder()
                .title("테스트 그룹")
                .penalty("커피 사기")
                .password("1234")
                .inviteCode("TESTCODE")
                .memberLimit(5)
                .build()
        );
        groupId = group.getId();

        Member owner = memberRepository.save(Member.create("방장", "owner", "password"));
        ownerId = owner.getId();
        ownerToken = authTokenService.createAccessToken(ownerId);
        groupMemberRepository.save(GroupMember.create(group, owner, GroupMemberRole.OWNER));

        Member member = memberRepository.save(Member.create("일반회원", "member", "password"));
        memberId = member.getId();
        memberToken = authTokenService.createAccessToken(memberId);
        GroupMember normalGroupMember = groupMemberRepository.save(
            GroupMember.create(group, member, GroupMemberRole.MEMBER)
        );
        memberGroupMemberId = normalGroupMember.getId();

        Habit habit = habitRepository.save(
            Habit.create(normalGroupMember, "아침 운동", "30분 러닝", 30)
        );
        memberHabitId = habit.getId();
        penaltyVerifyRepository.save(PenaltyVerify.create(normalGroupMember, habit));

        Habit habitForPending = habitRepository.save(
            Habit.create(normalGroupMember, "책 읽기", "하루 10페이지", 30)
        );
        PenaltyVerify pendingPv = PenaltyVerify.create(normalGroupMember, habitForPending);
        pendingPv.submit(LocalDate.now(), "책 읽었습니다.", "https://example.com/book.jpg");
        pendingPenaltyId = penaltyVerifyRepository.save(pendingPv).getId();

        Habit habitForRejected = habitRepository.save(
            Habit.create(normalGroupMember, "물 마시기", "하루 2L", 30)
        );
        PenaltyVerify rejectedPv = PenaltyVerify.create(normalGroupMember, habitForRejected);
        rejectedPv.submit(LocalDate.now(), "물 인증", "https://example.com/water.jpg");
        rejectedPv.reject();
        rejectedPenaltyId = penaltyVerifyRepository.save(rejectedPv).getId();
    }

    @Test
    @DisplayName("벌칙 최초 제출 성공 시 201 Created 및 PENDING 반환")
    void submit_성공() throws Exception {
        SubmitPenaltyVerifyRequest request = new SubmitPenaltyVerifyRequest(
            "오늘 러닝 완료", "https://example.com/run.jpg"
        );

        mockMvc.perform(post("/api/habits/{habitId}/penalties", memberHabitId)
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.description").value("오늘 러닝 완료"))
            .andExpect(jsonPath("$.imageUrl").value("https://example.com/run.jpg"));
    }

    @Test
    @DisplayName("반려된 벌칙 재제출 성공 시 PENDING 상태로 갱신")
    void resubmit_성공() throws Exception {
        SubmitPenaltyVerifyRequest request = new SubmitPenaltyVerifyRequest(
            "물 다시 마시고 재인증합니다.", "https://example.com/water2.jpg"
        );

        mockMvc.perform(patch("/api/penalties/{id}/resubmit", rejectedPenaltyId)
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.description").value("물 다시 마시고 재인증합니다."));
    }

    @Test
    @DisplayName("벌칙 단건 상세 조회 성공")
    void getDetail_성공() throws Exception {
        mockMvc.perform(get("/api/penalties/{id}", pendingPenaltyId)
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(pendingPenaltyId))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("그룹 멤버별 벌칙 목록 최신순 조회 성공")
    void getPenaltyList_성공() throws Exception {
        mockMvc.perform(get("/api/groups/{groupId}/members/{groupMemberId}/penalties", groupId, memberGroupMemberId)
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("방장의 벌칙 단건 승인 성공 시 APPROVED 반환")
    void approve_성공() throws Exception {
        mockMvc.perform(patch("/api/penalties/{id}/approve", pendingPenaltyId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("방장의 벌칙 단건 거절 성공 시 REJECTED 반환")
    void reject_성공() throws Exception {
        mockMvc.perform(patch("/api/penalties/{id}/reject", pendingPenaltyId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("방장의 그룹 내 벌칙 승인 대기 목록 조회 성공")
    void getPending_성공() throws Exception {
        mockMvc.perform(get("/api/groups/{groupId}/penalties/pending", groupId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(pendingPenaltyId));
    }

    @Test
    @DisplayName("방장의 벌칙 일괄 승인 성공")
    void bulkApprove_성공() throws Exception {
        BulkActionRequest request = new BulkActionRequest(List.of(pendingPenaltyId));

        mockMvc.perform(patch("/api/groups/{groupId}/penalties/bulk-approve", groupId)
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    @DisplayName("벌칙 작성자의 벌칙 삭제 성공 시 204 No Content 반환")
    void delete_성공() throws Exception {
        mockMvc.perform(delete("/api/penalties/{id}", pendingPenaltyId)
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isNoContent());
    }
}
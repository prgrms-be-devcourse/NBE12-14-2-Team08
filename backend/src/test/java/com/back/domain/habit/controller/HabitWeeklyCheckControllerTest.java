package com.back.domain.habit.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.entity.HabitStatus;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.habit.service.HabitService;
import com.back.domain.habitVerify.entity.HabitVerify;
import com.back.domain.habitVerify.entity.HabitVerifyStatus;
import com.back.domain.habitVerify.repository.HabitVerifyRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link HabitControllerTest}와 달리 회원가입/그룹생성/그룹가입을 실제 API로 거치지 않고,
 * weekly-check 판정에 필요한 최소한의 데이터만 리포지토리로 직접 insert해서 검증한다.
 * API 호출 왕복이 없어 더 빠르고, 각 시나리오에 필요한 createDate를 바로 세팅할 수 있다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HabitWeeklyCheckControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private HabitService habitService;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;
    @Autowired private HabitRepository habitRepository;
    @Autowired private HabitVerifyRepository habitVerifyRepository;
    @Autowired private PenaltyVerifyRepository penaltyVerifyRepository;

    private Long groupId;
    private Long memberId;
    private String ownerToken;
    private Long habitId;

    // 기본값: 그룹 생성일 14일 전(오늘과 같은 요일) / 가입일·습관 생성일 10일 전
    // -> 별도 조작 없이 바로 "가입 1주 경과 + 이번 주 마감 지남" 상태에서 시작한다.
    // 방장(OWNER)이 습관을 갖고 있고, 주간 판정도 방장 권한으로 호출한다.
    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        Member owner = memberRepository.save(
                Member.create("방장", "weekly-owner01", "password1234")
        );
        memberId = owner.getId();
        ownerToken = authTokenService.createAccessToken(memberId);

        Group group = groupRepository.save(
                Group.builder()
                        .title("테스트 그룹")
                        .penalty("팔굽혀펴기 30개")
                        .password("1234")
                        .inviteCode("WEEKLYCHECK01")
                        .memberLimit(5)
                        .build()
        );
        ReflectionTestUtils.setField(group, "createDate", now.minusDays(14));
        groupRepository.saveAndFlush(group);
        groupId = group.getId();

        GroupMember groupMember = groupMemberRepository.save(
                GroupMember.create(group, owner, GroupMemberRole.OWNER)
        );
        ReflectionTestUtils.setField(groupMember, "createDate", now.minusDays(10));
        groupMemberRepository.saveAndFlush(groupMember);

        Habit habit = habitRepository.save(
                Habit.create(groupMember, "물 마시기", "하루 2L 마시기", 3)
        );
        ReflectionTestUtils.setField(habit, "createDate", now.minusDays(10));
        habitRepository.saveAndFlush(habit);
        habitId = habit.getId();
    }

    // 이번 주 판정 구간(오늘 00시 이전) 안에 들어오도록 제출 시각을 3일 전으로 되돌린 인증 기록 생성
    private Long createVerify(LocalDate verifyDate, HabitVerifyStatus status) {
        Habit habit = habitRepository.findById(habitId).orElseThrow();

        HabitVerify verify = HabitVerify.create(
                habit, verifyDate, status, "테스트 인증", "https://x.com/a.jpg"
        );
        habitVerifyRepository.save(verify);

        ReflectionTestUtils.setField(verify, "createDate", LocalDateTime.now().minusDays(3));
        habitVerifyRepository.saveAndFlush(verify);

        return verify.getId();
    }

    @Test
    @DisplayName("가입한 지 1주가 안 되면 TOO_EARLY를 반환한다")
    void 가입_1주_미만이면_TOO_EARLY() throws Exception {
        GroupMember groupMember = groupMemberRepository
                .findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow();
        ReflectionTestUtils.setField(groupMember, "createDate", LocalDateTime.now());
        groupMemberRepository.saveAndFlush(groupMember);

        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].habitId").value(habitId))
                .andExpect(jsonPath("$[0].result").value("TOO_EARLY"))
                .andExpect(jsonPath("$[0].habitStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("가입 1주는 지났지만 습관이 이번 판정 구간 마감 이후에 생성됐다면 BEFORE_DEADLINE을 반환한다")
    void 습관_생성이_마감_이후면_BEFORE_DEADLINE() throws Exception {
        Habit habit = habitRepository.findById(habitId).orElseThrow();
        ReflectionTestUtils.setField(habit, "createDate", LocalDateTime.now());
        habitRepository.saveAndFlush(habit);

        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("BEFORE_DEADLINE"))
                .andExpect(jsonPath("$[0].habitStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("이미 실패 처리된 습관은 판정 대상에서 제외된다 (그룹 내 활성 습관만 조회)")
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
    @DisplayName("승인된 인증 횟수가 days 이상이면 PASSED를 반환한다")
    void 승인_횟수가_충분하면_PASSED() throws Exception {
        createVerify(LocalDate.now().minusDays(1), HabitVerifyStatus.APPROVED);
        createVerify(LocalDate.now().minusDays(2), HabitVerifyStatus.APPROVED);
        createVerify(LocalDate.now().minusDays(3), HabitVerifyStatus.APPROVED);

        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("PASSED"))
                .andExpect(jsonPath("$[0].habitStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("미검토(PENDING) 건이 남아있고 검토 유예시간도 안 지났으면 PENDING_REVIEW로 보류된다")
    void 미검토_건이_있으면_PENDING_REVIEW() throws Exception {
        createVerify(LocalDate.now().minusDays(1), HabitVerifyStatus.APPROVED);
        createVerify(LocalDate.now().minusDays(2), HabitVerifyStatus.APPROVED);
        createVerify(LocalDate.now().minusDays(3), HabitVerifyStatus.APPROVED);
        createVerify(LocalDate.now().minusDays(4), HabitVerifyStatus.PENDING);

        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$[0].habitStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("검토 유예시간(마감 다음날 24시)이 지나면 미검토 건이 자동 승인되어 판정이 이어서 진행된다")
    void 유예시간이_지나면_미검토_건_자동_승인_후_판정() throws Exception {
        // 그룹 생성 요일을 "어제"로 맞춰서 이번 주 마감(currentDeadline)이 어제 00시,
        // 검토 유예시간(마감 + 24시간)이 오늘 00시로 이미 지난 상태를 만든다.
        LocalDateTime now = LocalDateTime.now();

        Group group = groupRepository.findById(groupId).orElseThrow();
        ReflectionTestUtils.setField(group, "createDate", now.minusDays(15));
        groupRepository.saveAndFlush(group);

        createVerify(LocalDate.now().minusDays(2), HabitVerifyStatus.APPROVED);
        createVerify(LocalDate.now().minusDays(3), HabitVerifyStatus.APPROVED);
        Long pendingVerifyId = createVerify(LocalDate.now().minusDays(4), HabitVerifyStatus.PENDING);

        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("PASSED"))
                .andExpect(jsonPath("$[0].habitStatus").value("ACTIVE"));

        HabitVerify autoApproved = habitVerifyRepository.findById(pendingVerifyId).orElseThrow();
        Assertions.assertEquals(HabitVerifyStatus.APPROVED, autoApproved.getStatus());
    }

    @Test
    @DisplayName("weekly-check를 아무도 호출하지 않아도 스케줄러가 유예시간 지난 미검토 건을 자동 승인하고 확정한다")
    void 스케줄러가_전체_활성_습관을_순회하며_자동_승인_및_판정() {
        LocalDateTime now = LocalDateTime.now();

        Group group = groupRepository.findById(groupId).orElseThrow();
        ReflectionTestUtils.setField(group, "createDate", now.minusDays(15));
        groupRepository.saveAndFlush(group);

        createVerify(LocalDate.now().minusDays(2), HabitVerifyStatus.APPROVED);
        createVerify(LocalDate.now().minusDays(3), HabitVerifyStatus.APPROVED);
        Long pendingVerifyId = createVerify(LocalDate.now().minusDays(4), HabitVerifyStatus.PENDING);

        // 컨트롤러/멤버 호출 없이 스케줄러가 하는 일만 직접 실행
        habitService.autoCheckAllActiveHabits();

        HabitVerify autoApproved = habitVerifyRepository.findById(pendingVerifyId).orElseThrow();
        Assertions.assertEquals(HabitVerifyStatus.APPROVED, autoApproved.getStatus());

        Habit habit = habitRepository.findById(habitId).orElseThrow();
        Assertions.assertEquals(HabitStatus.ACTIVE, habit.getStatus());
    }

    @Test
    @DisplayName("검토가 끝났는데도 days 미달이면 실패 처리되고 벌칙이 확정된다")
    void 승인_횟수가_부족하면_실패_및_벌칙_확정() throws Exception {
        createVerify(LocalDate.now().minusDays(1), HabitVerifyStatus.APPROVED);

        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("FAILED_INSUFFICIENT_SUBMISSION"))
                .andExpect(jsonPath("$[0].habitStatus").value("FAILED"));

        Assertions.assertTrue(penaltyVerifyRepository.findByHabitId(habitId).isPresent());
    }

    @Test
    @DisplayName("방장이 아닌 일반 멤버가 호출하면 403을 반환한다")
    void 방장이_아니면_403() throws Exception {
        Member regularMember = memberRepository.save(
                Member.create("일반멤버", "weekly-member02", "password1234")
        );
        Group group = groupRepository.findById(groupId).orElseThrow();
        groupMemberRepository.save(
                GroupMember.create(group, regularMember, GroupMemberRole.MEMBER)
        );
        String memberToken = authTokenService.createAccessToken(regularMember.getId());

        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("그룹 멤버가 아닌 사람이 호출하면 400을 반환한다")
    void 그룹_멤버가_아니면_400() throws Exception {
        Member other = memberRepository.save(
                Member.create("다른멤버", "weekly-other01", "password1234")
        );
        String otherToken = authTokenService.createAccessToken(other.getId());

        mockMvc.perform(post("/api/groups/{groupId}/weekly-check", groupId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isBadRequest());
    }
}

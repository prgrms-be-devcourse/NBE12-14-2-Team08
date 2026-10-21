package com.back.domain.habit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.back.domain.group.entity.Group;
import com.back.domain.group.entity.GroupStatus;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.dto.CreateHabitRequest;
import com.back.domain.habit.dto.HabitResponse;
import com.back.domain.habit.dto.HabitWeeklyCheckResponse;
import com.back.domain.habit.dto.WeeklyCheckResult;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.entity.HabitStatus;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.habitVerify.repository.HabitVerifyRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.penaltyverify.service.PenaltyVerifyService;
import com.back.global.exception.BusinessRuleException;
import com.back.global.exception.EntityNotFoundException;
import com.back.global.exception.ForbiddenException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    @InjectMocks
    private HabitService habitService;

    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private HabitRepository habitRepository;
    @Mock
    private HabitVerifyRepository habitVerifyRepository;
    @Mock
    private PenaltyVerifyService penaltyVerifyService;

    private static final Long GROUP_ID = 100L;
    private static final Long MEMBER_ID = 1L;
    private static final Long GROUP_MEMBER_ID = 10L;
    private static final Long HABIT_ID = 50L;

    private Group group(GroupStatus status, LocalDate deadline) {
        Group group = Group.builder()
            .title("아침 기상 챌린지")
            .penalty("커피 1잔 쏘기")
            .memberLimit(10)
            .status(status)
            .deadline(deadline)
            .build();
        ReflectionTestUtils.setField(group, "id", GROUP_ID);
        return group;
    }

    private Group activeGroup() {
        return group(GroupStatus.ACTIVE, null);
    }

    private GroupMember groupMember(Group group, GroupMemberRole role) {
        Member member = Member.create("일반회원", "member", "pass1234");
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        GroupMember groupMember = GroupMember.create(group, member, role);
        ReflectionTestUtils.setField(groupMember, "id", GROUP_MEMBER_ID);
        return groupMember;
    }

    private Habit habit(GroupMember groupMember) {
        Habit habit = Habit.create(groupMember, "기상 후 스트레칭", "10분 스트레칭", 3);
        ReflectionTestUtils.setField(habit, "id", HABIT_ID);
        return habit;
    }

    // 판정 대상이 되는 상태(그룹 생성 14일 전, 가입·습관 생성 10일 전, 인증 기록 없음)의 습관
    private Habit overdueHabit(Group group) {
        LocalDateTime now = LocalDateTime.now();
        ReflectionTestUtils.setField(group, "createDate", now.minusDays(14));
        GroupMember groupMember = groupMember(group, GroupMemberRole.MEMBER);
        ReflectionTestUtils.setField(groupMember, "createDate", now.minusDays(10));
        Habit habit = habit(groupMember);
        ReflectionTestUtils.setField(habit, "createDate", now.minusDays(10));
        return habit;
    }

    @Nested
    @DisplayName("습관 생성(createHabit)")
    class CreateHabit {

        private final CreateHabitRequest request = new CreateHabitRequest("기상 후 스트레칭", "10분 스트레칭", 3);

        @Test
        @DisplayName("활성 습관이 없으면 ACTIVE 습관이 생성된다")
        void 생성_성공() {
            GroupMember groupMember = groupMember(activeGroup(), GroupMemberRole.MEMBER);
            given(groupMemberRepository.findByGroupIdAndMemberIdForUpdate(GROUP_ID, MEMBER_ID))
                .willReturn(Optional.of(groupMember));
            given(habitRepository.existsByGroupMember_IdAndStatus(GROUP_MEMBER_ID, HabitStatus.ACTIVE))
                .willReturn(false);
            given(habitRepository.save(any(Habit.class))).willAnswer(invocation -> invocation.getArgument(0));

            HabitResponse response = habitService.createHabit(GROUP_ID, MEMBER_ID, request);

            assertThat(response.title()).isEqualTo("기상 후 스트레칭");
            assertThat(response.description()).isEqualTo("10분 스트레칭");
            assertThat(response.days()).isEqualTo(3);
            assertThat(response.status()).isEqualTo(HabitStatus.ACTIVE);
            assertThat(response.groupMemberId()).isEqualTo(GROUP_MEMBER_ID);
        }

        @Test
        @DisplayName("그룹 멤버가 아니면 IllegalArgumentException이 발생한다")
        void 그룹_멤버가_아니면_예외() {
            given(groupMemberRepository.findByGroupIdAndMemberIdForUpdate(GROUP_ID, MEMBER_ID))
                .willReturn(Optional.empty());

            assertThatThrownBy(() -> habitService.createHabit(GROUP_ID, MEMBER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 그룹의 멤버를 찾을 수 없습니다.");
            verify(habitRepository, never()).save(any(Habit.class));
        }

        @Test
        @DisplayName("종료된 그룹이면 BusinessRuleException이 발생한다")
        void 종료된_그룹이면_예외() {
            GroupMember groupMember = groupMember(group(GroupStatus.FINISH, null), GroupMemberRole.MEMBER);
            given(groupMemberRepository.findByGroupIdAndMemberIdForUpdate(GROUP_ID, MEMBER_ID))
                .willReturn(Optional.of(groupMember));

            assertThatThrownBy(() -> habitService.createHabit(GROUP_ID, MEMBER_ID, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("이미 종료된 그룹에서는 습관을 추가할 수 없습니다.");
            verify(habitRepository, never()).save(any(Habit.class));
        }

        @Test
        @DisplayName("이미 활성 습관이 있으면 IllegalStateException이 발생하고 저장하지 않는다")
        void 활성_습관이_이미_있으면_예외() {
            GroupMember groupMember = groupMember(activeGroup(), GroupMemberRole.MEMBER);
            given(groupMemberRepository.findByGroupIdAndMemberIdForUpdate(GROUP_ID, MEMBER_ID))
                .willReturn(Optional.of(groupMember));
            given(habitRepository.existsByGroupMember_IdAndStatus(GROUP_MEMBER_ID, HabitStatus.ACTIVE))
                .willReturn(true);

            assertThatThrownBy(() -> habitService.createHabit(GROUP_ID, MEMBER_ID, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 활성화된 습관이 존재합니다.");
            verify(habitRepository, never()).save(any(Habit.class));
        }
    }
}

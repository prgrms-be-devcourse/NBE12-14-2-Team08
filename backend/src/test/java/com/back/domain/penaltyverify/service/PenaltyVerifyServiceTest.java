package com.back.domain.penaltyverify.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.back.domain.group.entity.Group;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.penaltyverify.dto.SubmitPenaltyVerifyRequest;
import com.back.domain.penaltyverify.entity.PenaltyVerify;
import com.back.domain.penaltyverify.repository.PenaltyVerifyRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PenaltyVerifyServiceTest {

    @Mock private PenaltyVerifyRepository penaltyVerifyRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private HabitRepository habitRepository;

    @InjectMocks
    private PenaltyVerifyService penaltyVerifyService;

    @Test
    @DisplayName("1. 본인 습관이 아니면 벌칙 제출 시 예외 발생")
    void 본인_습관이_아니면_벌칙_제출시_예외() {
        // given
        Member ownerMember = mock(Member.class);
        when(ownerMember.getId()).thenReturn(1L);

        GroupMember groupMember = mock(GroupMember.class);
        when(groupMember.getMember()).thenReturn(ownerMember);

        Habit habit = mock(Habit.class);
        when(habit.getTitle()).thenReturn("기상 후 운동");

        Group group = mock(Group.class);
        when(group.getPenalty()).thenReturn("커피사기");
        when(groupMember.getGroup()).thenReturn(group);

        PenaltyVerify pv = PenaltyVerify.create(groupMember, habit);
        when(penaltyVerifyRepository.findByHabitId(100L)).thenReturn(Optional.of(pv));

        SubmitPenaltyVerifyRequest request =
            new SubmitPenaltyVerifyRequest("운동함", "https://x.com/a.jpg");

        // when & then: 로그인한 사용자 999L은 습관 소유자(1L)가 아님
        assertThatThrownBy(() -> penaltyVerifyService.submit(999L, 100L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("본인의 습관");
    }

    @Test
    @DisplayName("2. PENDING 상태가 아니면 승인 시 예외 발생")
    void PENDING_상태가_아니면_승인시_예외() {
        // given
        Group group = mock(Group.class);
        when(group.getId()).thenReturn(10L);
        when(group.getPenalty()).thenReturn("커피사기");

        GroupMember gm = mock(GroupMember.class);
        when(gm.getGroup()).thenReturn(group);

        Habit habit = mock(Habit.class);
        when(habit.getTitle()).thenReturn("기상 후 운동");

        PenaltyVerify pv = PenaltyVerify.create(gm, habit);
        pv.submit(LocalDate.now(), "설명", "https://x.com/a.jpg");
        pv.approve(); // 이미 APPROVED 상태

        when(penaltyVerifyRepository.findById(1L)).thenReturn(Optional.of(pv));

        GroupMember owner = ownerGroupMember();
        when(groupMemberRepository.findByGroupIdAndMemberId(any(), any()))
            .thenReturn(Optional.of(owner));

        // when & then: APPROVED 상태는 재승인 불가
        assertThatThrownBy(() -> penaltyVerifyService.approve(1L, 1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("승인 대기 상태가 아닙니다");
    }

    @Test
    @DisplayName("3. 방장이 아니면 승인 권한 없음 예외 발생")
    void 방장이_아니면_승인_권한_없음() {
        // given
        Group group = mock(Group.class);
        when(group.getId()).thenReturn(10L);
        when(group.getPenalty()).thenReturn("커피사기");

        GroupMember gm = mock(GroupMember.class);
        when(gm.getGroup()).thenReturn(group);

        Habit habit = mock(Habit.class);
        when(habit.getTitle()).thenReturn("기상 후 운동");

        PenaltyVerify pv = PenaltyVerify.create(gm, habit);
        pv.submit(LocalDate.now(), "설명", "https://x.com/a.jpg"); // PENDING 상태

        when(penaltyVerifyRepository.findById(1L)).thenReturn(Optional.of(pv));

        GroupMember notOwner = mock(GroupMember.class);
        when(notOwner.getRole()).thenReturn(GroupMemberRole.MEMBER);
        when(groupMemberRepository.findByGroupIdAndMemberId(any(), any()))
            .thenReturn(Optional.of(notOwner));

        // when & then
        assertThatThrownBy(() -> penaltyVerifyService.approve(2L, 1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("권한이 없습니다");
    }

    private GroupMember ownerGroupMember() {
        GroupMember owner = mock(GroupMember.class);
        when(owner.getRole()).thenReturn(GroupMemberRole.OWNER);
        return owner;
    }
}
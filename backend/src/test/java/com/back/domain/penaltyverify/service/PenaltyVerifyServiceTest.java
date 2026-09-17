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
@MockitoSettings(strictness = Strictness.LENIENT) // 불필요한 stubbing 경고를 무시하고 유연하게 허용
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
        when(habit.getGroupMember()).thenReturn(groupMember);

        when(habitRepository.findById(100L)).thenReturn(Optional.of(habit));

        SubmitPenaltyVerifyRequest request =
            new SubmitPenaltyVerifyRequest(LocalDate.now(), "운동함", "https://x.com/a.jpg");

        // when & then: memberId=999는 습관 소유자(1L)가 아님
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

        GroupMember gm = mock(GroupMember.class);
        when(gm.getGroup()).thenReturn(group);

        Habit habit = mock(Habit.class);

        PenaltyVerify pv = PenaltyVerify.submit(
            gm, habit, LocalDate.now(), "기상 후 운동", "커피사기", "설명", "https://x.com/a.jpg"
        );
        pv.approve(); // 이미 APPROVED 상태로 변경

        when(penaltyVerifyRepository.findById(1L)).thenReturn(Optional.of(pv));

        // 🚨 밖에서 미리 선언해서 중첩 when 에러 방지
        GroupMember owner = ownerGroupMember();
        when(groupMemberRepository.findByGroupIdAndMemberId(any(), any()))
            .thenReturn(Optional.of(owner));

        // when & then
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

        GroupMember gm = mock(GroupMember.class);
        when(gm.getGroup()).thenReturn(group);

        Habit habit = mock(Habit.class);

        PenaltyVerify pv = PenaltyVerify.submit(
            gm, habit, LocalDate.now(), "기상 후 운동", "커피사기", "설명", "https://x.com/a.jpg"
        );
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
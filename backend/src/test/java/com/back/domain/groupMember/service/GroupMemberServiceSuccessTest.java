package com.back.domain.groupMember.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.dto.GroupMemberResponse;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.entity.GroupMemberStatus;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.habitVerify.repository.HabitVerifyRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.penaltyverify.repository.PenaltyVerifyRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GroupMemberServiceSuccessTest {

    @InjectMocks
    private GroupMemberService groupMemberService;

    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private HabitRepository habitRepository;
    @Mock private HabitVerifyRepository habitVerifyRepository;
    @Mock private PenaltyVerifyRepository penaltyVerifyRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private final Long memberId = 1L;
    private final Long groupId = 100L;
    private final Long groupMemberId = 50L;
    private final String inviteCode = "abcdefgh";

    @Test
    @DisplayName("그룹 가입 성공 - 이미 가입되어 있는 유저인 경우 멱등성이 유지되며 바로 그룹 ID를 리턴한다")
    void joinGroup_Success_AlreadyMember() {
        // given
        Group mockGroup = Group.builder().build();
        ReflectionTestUtils.setField(mockGroup, "id", groupId);
        GroupRequest.Join request = new GroupRequest.Join("pass123");

        given(groupRepository.findByInviteCodeForUpdate(inviteCode)).willReturn(Optional.of(mockGroup));
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).willReturn(true);

        // when
        Long resultGroupId = groupMemberService.joinGroup(inviteCode, memberId, request);

        // then
        assertThat(resultGroupId).isEqualTo(groupId);
        verify(groupMemberRepository, never()).save(any(GroupMember.class));
    }

    @Test
    @DisplayName("그룹 가입 성공 - 신규 유저가 패스워드 및 정원 검증을 통과하여 정상 가입된다")
    void joinGroup_Success_NewMember() {
        // given
        Group mockGroup = Group.builder().password("encoded_pass").memberLimit(5).deadline(LocalDate.now().plusDays(7)) .build();
        ReflectionTestUtils.setField(mockGroup, "id", groupId);
        GroupRequest.Join request = new GroupRequest.Join("pass123");
        Member mockMember = mock(Member.class);

        given(groupRepository.findByInviteCodeForUpdate(inviteCode)).willReturn(Optional.of(mockGroup));
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).willReturn(false);
        given(passwordEncoder.matches("pass123", "encoded_pass")).willReturn(true);
        given(groupMemberRepository.countByGroupId(groupId)).willReturn(3L);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(mockMember));

        // when
        Long resultGroupId = groupMemberService.joinGroup(inviteCode, memberId, request);

        // then
        assertThat(resultGroupId).isEqualTo(groupId);
        verify(groupMemberRepository, times(1)).save(any(GroupMember.class));
    }

    @Test
    @DisplayName("그룹 멤버 리스트 조회 - 소속 유저 확인 통과 후 정상 반환된다")
    void getGroupMembers_Success() {
        // given
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).willReturn(true);
        given(groupMemberRepository.findByGroupIdWithHabit(groupId)).willReturn(Collections.emptyList());

        // when
        List<GroupMemberResponse.Simple> result = groupMemberService.getGroupMembers(groupId, memberId);

        // then
        assertThat(result).isNotNull();
        verify(groupMemberRepository, times(1)).findByGroupIdWithHabit(groupId);
    }

    @Test
    @DisplayName("그룹 멤버 상세 조회 - 소속 유저 확인 통과 후 벌칙 카운트가 포함된 DTO가 정상 반환된다")
    void getGroupMemberDetail_Success() {
        // given
        GroupMemberResponse.Detail mockDetail = new GroupMemberResponse.Detail(
                groupMemberId, memberId, "닉네임", "아이디", "MEMBER", 2
        );
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).willReturn(true);
        given(groupMemberRepository.findMemberDetailWithPenaltyCount(groupId, groupMemberId)).willReturn(Optional.of(mockDetail));

        // when
        GroupMemberResponse.Detail result = groupMemberService.getGroupMemberDetail(groupId, groupMemberId, memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.groupMemberId()).isEqualTo(groupMemberId);
        assertThat(result.penaltyCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("그룹 자발적 탈퇴 성공 - 활성화 상태의 일반 팀원(MEMBER)인 경우 연관 데이터가 일괄 벌크 삭제된다")
    void leaveGroup_Success_ActiveMember() {
        // given
        Group mockGroup = Group.builder()
                .deadline(LocalDate.now().plusDays(7))
                .build();
        GroupMember mockGroupMember = GroupMember.builder().group(mockGroup).role(GroupMemberRole.MEMBER).build();
        ReflectionTestUtils.setField(mockGroupMember, "id", groupMemberId);

        given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)).willReturn(Optional.of(mockGroupMember));
        given(habitRepository.findIdsByGroupMemberId(groupMemberId)).willReturn(List.of(10L, 11L));

        // when
        groupMemberService.leaveGroup(groupId, memberId);

        // then
        verify(habitVerifyRepository, times(1)).deleteByHabitIds(anyList());
        verify(penaltyVerifyRepository, times(1)).deleteByGroupMemberId(groupMemberId);
        verify(habitRepository, times(1)).deleteByIds(anyList());
        verify(groupMemberRepository, times(1)).delete(mockGroupMember);
    }

    @Test
    @DisplayName("그룹 자발적 탈퇴 성공 - 이미 종료(FINISH)된 그룹인 경우 데이터 파기 없이 LEFT 상태로 변경된다")
    void leaveGroup_Success_FinishedGroup() {
        // given
        Group mockGroup = mock(Group.class);
        given(mockGroup.isFinished()).willReturn(true);

        GroupMember mockGroupMember = GroupMember.builder().group(mockGroup).role(GroupMemberRole.MEMBER).build();

        given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)).willReturn(Optional.of(mockGroupMember));

        // when
        groupMemberService.leaveGroup(groupId, memberId);

        // then
        assertThat(mockGroupMember.getStatus()).isEqualTo(GroupMemberStatus.LEFT);
        verify(groupMemberRepository, never()).delete(any(GroupMember.class));
    }

    @Test
    @DisplayName("팀원 강제 추방 성공 - 방장 권한으로 일반 팀원을 타겟팅해 연관 데이터를 파기하고 완전 추방한다")
    void kickMember_Success() {
        // given
        Group mockGroup = Group.builder()
                .deadline(LocalDate.now().plusDays(7))
                .build();
        ReflectionTestUtils.setField(mockGroup, "id", groupId);

        GroupMember owner = GroupMember.builder().group(mockGroup).role(GroupMemberRole.OWNER).build();
        ReflectionTestUtils.setField(owner, "id", groupMemberId);

        Long targetGroupMemberId = 55L;
        GroupMember kickTarget = GroupMember.builder().group(mockGroup).role(GroupMemberRole.MEMBER).build();
        ReflectionTestUtils.setField(kickTarget, "id", targetGroupMemberId);

        given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)).willReturn(Optional.of(owner));
        given(groupMemberRepository.findById(targetGroupMemberId)).willReturn(Optional.of(kickTarget));
        given(habitRepository.findIdsByGroupMemberId(targetGroupMemberId)).willReturn(Collections.emptyList());

        // when
        groupMemberService.kickMember(groupId, targetGroupMemberId, memberId);

        // then
        verify(penaltyVerifyRepository, times(1)).deleteByGroupMemberId(targetGroupMemberId);
        verify(groupMemberRepository, times(1)).delete(kickTarget);
    }

    @Test
    @DisplayName("방장 권한 위임 성공 - 같은 그룹 내의 다른 팀원에게 OWNER 자격을 넘겨주고 자신은 MEMBER가 된다")
    void transferOwner_Success() {
        // given
        Group mockGroup = Group.builder().build();
        ReflectionTestUtils.setField(mockGroup, "id", groupId);

        GroupMember currentOwner = GroupMember.builder().group(mockGroup).role(GroupMemberRole.OWNER).build();
        ReflectionTestUtils.setField(currentOwner, "id", groupMemberId);

        Long nextOwnerGroupMemberId = 60L;
        GroupMember nextOwner = GroupMember.builder().group(mockGroup).role(GroupMemberRole.MEMBER).build();
        ReflectionTestUtils.setField(nextOwner, "id", nextOwnerGroupMemberId);

        given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)).willReturn(Optional.of(currentOwner));
        given(groupMemberRepository.findById(nextOwnerGroupMemberId)).willReturn(Optional.of(nextOwner));

        // when
        groupMemberService.transferOwner(groupId, nextOwnerGroupMemberId, memberId);

        // then
        assertThat(currentOwner.getRole()).isEqualTo(GroupMemberRole.MEMBER);
        assertThat(nextOwner.getRole()).isEqualTo(GroupMemberRole.OWNER);
    }
}

package com.back.domain.groupMember.service;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.BusinessRuleException;
import com.back.global.exception.EntityNotFoundException;
import com.back.global.exception.ForbiddenException;
import com.back.global.exception.GroupLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupMemberServiceExceptionTest {

    @InjectMocks
    private GroupMemberService groupMemberService;

    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private HabitRepository habitRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private final Long memberId = 1L;
    private final Long groupId = 100L;
    private final Long groupMemberId = 50L;
    private final String inviteCode = "abcdefgh";

    @Test
    @DisplayName("가입 실패 - 유효하지 않거나 존재하지 않는 초대 코드인 경우 EntityNotFoundException이 발생한다")
    void joinGroup_Fail_InvalidInviteCode() {
        // given
        GroupRequest.Join request = new GroupRequest.Join("pass123");
        given(groupRepository.findByInviteCode(inviteCode)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> groupMemberService.joinGroup(inviteCode, memberId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("유효하지 않거나 존재하지 않는 초대 코드입니다.");
    }

    @Test
    @DisplayName("가입 실패 - 그룹 비밀번호가 일치하지 않는 경우 ForbiddenException이 발생한다")
    void joinGroup_Fail_WrongPassword() {
        // given
        Group mockGroup = Group.builder()
                .password("encoded_pass")
                .deadline(LocalDate.now().plusDays(7))
                .build();
        ReflectionTestUtils.setField(mockGroup, "id", groupId);
        GroupRequest.Join request = new GroupRequest.Join("wrong_pass");

        given(groupRepository.findByInviteCode(inviteCode)).willReturn(Optional.of(mockGroup));
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).willReturn(false);
        given(passwordEncoder.matches("wrong_pass", "encoded_pass")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> groupMemberService.joinGroup(inviteCode, memberId, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("그룹 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("가입 실패 - 이미 마감/종료된 그룹인 경우 BusinessRuleException이 발생한다")
    void joinGroup_Fail_AlreadyFinished() {
        // given
        Group mockGroup = mock(Group.class);
        given(mockGroup.getId()).willReturn(groupId);
        given(mockGroup.getPassword()).willReturn("encoded_pass");
        given(mockGroup.isFinished()).willReturn(true);

        GroupRequest.Join request = new GroupRequest.Join("pass123");

        given(groupRepository.findByInviteCode(inviteCode)).willReturn(Optional.of(mockGroup));
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).willReturn(false);
        given(passwordEncoder.matches("pass123", "encoded_pass")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> groupMemberService.joinGroup(inviteCode, memberId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("이미 종료된 그룹입니다.");
    }

    @Test
    @DisplayName("가입 실패 - 설정된 최대 제한 인원을 초과하는 경우 GroupLimitExceededException이 발생한다")
    void joinGroup_Fail_LimitExceeded() {
        // given
        Group mockGroup = Group.builder()
                .password("encoded_pass")
                .memberLimit(5)
                .deadline(LocalDate.now().plusDays(7))
                .build();
        ReflectionTestUtils.setField(mockGroup, "id", groupId);
        GroupRequest.Join request = new GroupRequest.Join("pass123");

        given(groupRepository.findByInviteCode(inviteCode)).willReturn(Optional.of(mockGroup));
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).willReturn(false);
        given(passwordEncoder.matches("pass123", "encoded_pass")).willReturn(true);
        given(groupMemberRepository.countByGroupId(groupId)).willReturn(5L);

        // when & then
        assertThatThrownBy(() -> groupMemberService.joinGroup(inviteCode, memberId, request))
                .isInstanceOf(GroupLimitExceededException.class)
                .hasMessageContaining("그룹의 최대 인원이 초과되어 입장할 수 없습니다.");
    }

    @Test
    @DisplayName("탈퇴 실패 - 그룹이 활성화 상태일 때 방장(OWNER) 유저는 자발적 탈퇴를 시도하면 예외가 발생한다")
    void leaveGroup_Fail_OwnerCannotLeaveActiveGroup() {
        // given
        Group mockGroup = Group.builder()
                .deadline(LocalDate.now().plusDays(7))
                .build(); 
        GroupMember mockGroupMember = GroupMember.builder().group(mockGroup).role(GroupMemberRole.OWNER).build();

        given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)).willReturn(Optional.of(mockGroupMember));

        // when & then
        assertThatThrownBy(() -> groupMemberService.leaveGroup(groupId, memberId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("방장은 그룹 활성화 상태에서 그룹을 탈퇴할 수 없습니다.");
    }

    @Test
    @DisplayName("추방 실패 - 방장이 아닌 일반 팀원 권한으로 타인을 추방하려 할 때 ForbiddenException이 발생한다")
    void kickMember_Fail_NotOwner() {
        // given
        Group mockGroup = Group.builder().build();
        GroupMember normalMember = GroupMember.builder().group(mockGroup).role(GroupMemberRole.MEMBER).build();

        given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)).willReturn(Optional.of(normalMember));

        // when & then
        assertThatThrownBy(() -> groupMemberService.kickMember(groupId, 55L, memberId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("그룹 멤버 추방은 방장만 가능합니다.");
    }

    @Test
    @DisplayName("추방 실패 - 방장이 자기 자신을 추방 타겟으로 설정하면 BusinessRuleException이 발생한다")
    void kickMember_Fail_KickSelf() {
        // given
        Group mockGroup = Group.builder()
                .deadline(LocalDate.now().plusDays(7))
                .build();
        ReflectionTestUtils.setField(mockGroup, "id", groupId);

        GroupMember owner = GroupMember.builder().group(mockGroup).role(GroupMemberRole.OWNER).build();
        ReflectionTestUtils.setField(owner, "id", groupMemberId);

        given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)).willReturn(Optional.of(owner));
        given(groupMemberRepository.findById(groupMemberId)).willReturn(Optional.of(owner));

        // when & then
        assertThatThrownBy(() -> groupMemberService.kickMember(groupId, groupMemberId, memberId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("방장 스스로를 추방할 수 없습니다.");
    }

    @Test
    @DisplayName("위임 실패 - 방장이 본인 자신에게 권한을 다시 위임하려고 요청하면 예외가 발생한다")
    void transferOwner_Fail_TransferToSelf() {
        // given
        Group mockGroup = Group.builder().build();
        ReflectionTestUtils.setField(mockGroup, "id", groupId);

        GroupMember owner = GroupMember.builder().group(mockGroup).role(GroupMemberRole.OWNER).build();
        ReflectionTestUtils.setField(owner, "id", groupMemberId);

        given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)).willReturn(Optional.of(owner));
        given(groupMemberRepository.findById(groupMemberId)).willReturn(Optional.of(owner));

        // when & then
        assertThatThrownBy(() -> groupMemberService.transferOwner(groupId, groupMemberId, memberId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("자기 자신에게 방장 권한을 위임할 수 없습니다.");
    }
}
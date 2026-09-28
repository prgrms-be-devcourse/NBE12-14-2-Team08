package com.back.domain.group.service;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.entity.Group;
import com.back.domain.group.entity.GroupStatus;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.BusinessRuleException;
import com.back.global.exception.EntityNotFoundException;
import com.back.global.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceExceptionTest {

    @InjectMocks
    private GroupService groupService;

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private final Long memberId = 1L;
    private final Long groupId = 100L;

    @Test
    @DisplayName("그룹 생성 실패 - 존재하지 않는 회원이면 EntityNotFoundException이 발생한다")
    void createGroup_MemberNotFound() {
        // given
        GroupRequest.Create request = new GroupRequest.Create("제목", "설명", null, "벌칙", "123", 5);

        Group mockGroup = Group.builder()
                .title("제목")
                .inviteCode("mockcode")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(mockGroup, "createDate", java.time.LocalDateTime.now());

        given(passwordEncoder.encode(anyString())).willReturn("encoded_password");
        given(groupRepository.save(any(Group.class))).willReturn(mockGroup);
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> groupService.createGroup(memberId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("존재하지 않는 회원입니다.");
    }

    @Test
    @DisplayName("그룹 상세 조회 - 기한이 만료된 ACTIVE 그룹을 조회하면 FINISH 상태로 변경된다")
    void getGroupDetail_ExpiredChangesToFinish() {
        Group mockGroup = Group.builder().title("그룹").inviteCode("code").deadline(LocalDate.now().minusDays(1)).build();
        ReflectionTestUtils.setField(mockGroup, "createDate", LocalDateTime.now());

        given(groupRepository.findById(groupId)).willReturn(Optional.of(mockGroup));
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).willReturn(true);

        groupService.getGroupDetail(groupId, memberId);

        assertThat(mockGroup.getStatus()).isEqualTo(GroupStatus.FINISH);
    }

    @Test
    @DisplayName("그룹 상세 조회 실패 - 해당 그룹에 속하지 않은 멤버가 접근하면 ForbiddenException이 발생한다")
    void getGroupDetail_Forbidden() {
        given(groupRepository.findById(groupId)).willReturn(Optional.of(mock(Group.class)));
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).willReturn(false);

        assertThatThrownBy(() -> groupService.getGroupDetail(groupId, memberId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("해당 그룹의 접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("그룹 수정 실패 - 이미 마감일이 지난 그룹은 수정할 수 없다")
    void updateGroup_AlreadyFinished() {
        Group mockGroup = Group.builder().title("기존").deadline(LocalDate.now().minusDays(1)).build();
        given(groupRepository.findById(groupId)).willReturn(Optional.of(mockGroup));

        GroupRequest.Update request = new GroupRequest.Update("제목", null, null, null, null, 5);

        assertThatThrownBy(() -> groupService.updateGroup(groupId, memberId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("이미 종료된 그룹은 수정할 수 없습니다.");
    }

    @Test
    @DisplayName("그룹 수정 실패 - 방장(OWNER)이 아닌 일반 멤버가 요청하면 ForbiddenException이 발생한다")
    void updateGroup_NotOwner() {
        Group mockGroup = Group.builder().title("기존").deadline(LocalDate.now().plusDays(2)).build();
        GroupMember mockGroupMember = GroupMember.builder().role(GroupMemberRole.MEMBER).build();
        GroupRequest.Update request = new GroupRequest.Update("제목", null, null, null, null, 5);

        given(groupRepository.findById(groupId)).willReturn(Optional.of(mockGroup));
        given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)).willReturn(Optional.of(mockGroupMember));

        assertThatThrownBy(() -> groupService.updateGroup(groupId, memberId, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("그룹 수정은 방장만 가능합니다.");
    }

    @Test
    @DisplayName("그룹 삭제 실패 - 방장 외에 다른 멤버가 존재하면 BusinessRuleException이 발생한다")
    void deleteGroup_HasOtherMembers() {
        Group mockGroup = Group.builder().deadline(LocalDate.now().plusDays(2)).build();
        GroupMember mockGroupMember = GroupMember.builder().role(GroupMemberRole.OWNER).build();

        given(groupRepository.findById(groupId)).willReturn(Optional.of(mockGroup));
        given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)).willReturn(Optional.of(mockGroupMember));
        given(groupMemberRepository.countByGroupId(groupId)).willReturn(3L); // 방장 포함 3명 상황

        assertThatThrownBy(() -> groupService.deleteGroup(groupId, memberId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("그룹을 삭제하려면 방장을 제외한 모든 멤버가 퇴장해야 합니다.");
    }

    @Test
    @DisplayName("스케줄러 - 만료 그룹 자동 종료 벌크 연산이 정상 호출된다")
    void autoCloseExpiredGroups_Success() {
        given(groupRepository.bulkFinishExpiredGroups(any(LocalDate.class))).willReturn(5);

        groupService.autoCloseExpiredGroups();

        verify(groupRepository, times(1)).bulkFinishExpiredGroups(any(LocalDate.class));
    }
}

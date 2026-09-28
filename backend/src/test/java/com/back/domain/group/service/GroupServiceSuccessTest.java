package com.back.domain.group.service;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.entity.Group;
import com.back.domain.group.entity.GroupStatus;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.groupMember.service.GroupMemberService;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceSuccessTest {

    @InjectMocks
    private GroupService groupService;

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private GroupMemberService groupMemberService;

    private final String baseInviteUrl = "http://test-invite.com";
    private final Long memberId = 1L;
    private final Long groupId = 100L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(groupService, "baseInviteUrl", baseInviteUrl);
    }

    @Test
    @DisplayName("그룹 생성 - 성공 (방장 권한으로 멤버가 정상 등록된다)")
    void createGroup_Success() {
        // given
        GroupRequest.Create request = new GroupRequest.Create(
                "테스트 그룹", "설명", LocalDate.now().plusDays(7), "벌칙", "pass123", 5
        );
        Member mockMember = mock(Member.class);
        Group mockGroup = Group.builder()
                .title(request.title())
                .description(request.description())
                .deadline(request.deadline())
                .penalty(request.penalty())
                .password("encoded_pass")
                .inviteCode("12345678")
                .memberLimit(request.memberLimit())
                .build();
        ReflectionTestUtils.setField(mockGroup, "createDate", LocalDateTime.now());
        ReflectionTestUtils.setField(mockGroup, "id", groupId);

        given(groupRepository.existsByInviteCode(anyString())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encoded_pass");
        given(groupRepository.save(any(Group.class))).willReturn(mockGroup);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(mockMember));

        // when
        GroupResponse.Detail response = groupService.createGroup(memberId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(groupId);
        assertThat(response.inviteLink()).startsWith(baseInviteUrl);
        verify(groupMemberRepository, times(1)).save(any(GroupMember.class));
    }

    @Test
    @DisplayName("그룹 목록 조회 - 성공")
    void getGroupSimpleList_Success() {
        // given
        given(groupRepository.findMyGroupsWithCount(memberId, GroupStatus.ACTIVE)).willReturn(Collections.emptyList());

        // when
        List<GroupResponse.Simple> result = groupService.getGroupSimpleList(memberId, GroupStatus.ACTIVE);

        // then
        assertThat(result).isNotNull();
        verify(groupRepository, times(1)).findMyGroupsWithCount(memberId, GroupStatus.ACTIVE);
    }

    @Test
    @DisplayName("그룹 상세 조회 - 성공 (그룹원 자격 검증 포함)")
    void getGroupDetail_Success() {
        // given
        Group mockGroup = Group.builder().title("그룹").inviteCode("code").deadline(LocalDate.now().plusDays(1)).build();
        ReflectionTestUtils.setField(mockGroup, "createDate", LocalDateTime.now());

        given(groupRepository.findById(groupId)).willReturn(Optional.of(mockGroup));
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).willReturn(true);

        // when
        GroupResponse.Detail response = groupService.getGroupDetail(groupId, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(mockGroup.getStatus()).isEqualTo(GroupStatus.ACTIVE);
    }

    @Test
    @DisplayName("그룹 수정 - 성공 (방장 권한을 가진 유저가 그룹 정보 변경)")
    void updateGroup_Success() {
        // given
        Group mockGroup = Group.builder().title("기존").deadline(LocalDate.now().plusDays(2)).build();
        GroupMember mockGroupMember = GroupMember.builder().role(GroupMemberRole.OWNER).build();
        GroupRequest.Update request = new GroupRequest.Update("새제목", null, null, null, "newpass", 10);

        given(groupRepository.findById(groupId)).willReturn(Optional.of(mockGroup));
        given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)).willReturn(Optional.of(mockGroupMember));
        given(passwordEncoder.encode("newpass")).willReturn("encoded_newpass");

        // when
        groupService.updateGroup(groupId, memberId, request);

        // then
        assertThat(mockGroup.getTitle()).isEqualTo("새제목");
    }

    @Test
    @DisplayName("그룹 삭제 - 성공 (방장 혼자만 남은 그룹 정상 삭제)")
    void deleteGroup_Success() {
        // given
        Group mockGroup = Group.builder().deadline(LocalDate.now().plusDays(2)).build();
        GroupMember mockGroupMember = GroupMember.builder().role(GroupMemberRole.OWNER).build();
        ReflectionTestUtils.setField(mockGroupMember, "id", 50L);

        given(groupRepository.findById(groupId)).willReturn(Optional.of(mockGroup));
        given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)).willReturn(Optional.of(mockGroupMember));
        given(groupMemberRepository.countByGroupId(groupId)).willReturn(1L);

        // when
        groupService.deleteGroup(groupId, memberId);

        // then
        verify(groupMemberService, times(1)).deleteGroupMemberDataBulk(50L);
        verify(groupMemberRepository, times(1)).delete(mockGroupMember);
        verify(groupRepository, times(1)).delete(mockGroup);
    }
}

package com.back.domain.groupMember.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
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
import com.back.global.exception.BusinessRuleException;
import com.back.global.exception.EntityNotFoundException;
import com.back.global.exception.ForbiddenException;
import com.back.global.exception.GroupLimitExceededException;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GroupMemberServiceTest {

    @InjectMocks
    private GroupMemberService groupMemberService;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private HabitVerifyRepository habitVerifyRepository;

    @Mock
    private PenaltyVerifyRepository penaltyVerifyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EntityManager entityManager;

    private final Long memberId = 1L;
    private final Long groupId = 100L;
    private final Long groupMemberId = 50L;
    private final String inviteCode = "abcdefgh";

    private Group createActiveGroup() {
        Group group = Group.builder()
                .password("encoded_pass")
                .memberLimit(5)
                .deadline(LocalDate.now().plusDays(7))
                .build();

        ReflectionTestUtils.setField(group, "id", groupId);
        return group;
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                groupMemberService,
                "entityManager",
                entityManager
        );
    }

    // 그룹 가입
    @Nested
    @DisplayName("JoinGroup")
    class JoinGroup {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("이미 가입한 멤버는 기존 그룹 ID를 반환한다")
            void joinGroup_AlreadyMember() {
                // given
                Group group = createActiveGroup();
                GroupRequest.Join request = new GroupRequest.Join("pass123");

                given(groupRepository.findByInviteCodeForUpdate(inviteCode))
                        .willReturn(Optional.of(group));
                given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(true);

                // when
                Long result = groupMemberService.joinGroup(
                        inviteCode, memberId, request
                );

                // then
                assertThat(result).isEqualTo(groupId);
                verify(groupMemberRepository, never()).save(any(GroupMember.class));
            }

            @Test
            @DisplayName("비밀번호와 정원 검증을 통과하면 신규 가입한다")
            void joinGroup_NewMember() {
                // given
                Group group = createActiveGroup();
                Member member = org.mockito.Mockito.mock(Member.class);
                GroupRequest.Join request = new GroupRequest.Join("pass123");

                given(groupRepository.findByInviteCodeForUpdate(inviteCode))
                        .willReturn(Optional.of(group));
                given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(false);
                given(passwordEncoder.matches("pass123", "encoded_pass"))
                        .willReturn(true);
                given(groupMemberRepository.countByGroupIdAndStatus(
                        groupId, GroupMemberStatus.ACTIVE
                )).willReturn(3L);
                given(memberRepository.findById(memberId))
                        .willReturn(Optional.of(member));

                // when
                Long result = groupMemberService.joinGroup(
                        inviteCode, memberId, request
                );

                // then
                assertThat(result).isEqualTo(groupId);
                verify(groupMemberRepository, times(1))
                        .save(any(GroupMember.class));
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("유효하지 않은 초대 코드이면 예외가 발생한다")
            void joinGroup_InvalidInviteCode() {
                // given
                GroupRequest.Join request = new GroupRequest.Join("pass123");

                given(groupRepository.findByInviteCodeForUpdate(inviteCode))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.joinGroup(inviteCode, memberId, request)
                )
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("유효하지 않거나 존재하지 않는 초대 코드입니다.");
            }

            @Test
            @DisplayName("그룹 비밀번호가 일치하지 않으면 예외가 발생한다")
            void joinGroup_WrongPassword() {
                // given
                Group group = createActiveGroup();
                GroupRequest.Join request = new GroupRequest.Join("wrong_pass");

                given(groupRepository.findByInviteCodeForUpdate(inviteCode))
                        .willReturn(Optional.of(group));
                given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(false);
                given(passwordEncoder.matches("wrong_pass", "encoded_pass"))
                        .willReturn(false);

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.joinGroup(inviteCode, memberId, request)
                )
                        .isInstanceOf(ForbiddenException.class)
                        .hasMessageContaining("그룹 비밀번호가 일치하지 않습니다.");
            }

            @Test
            @DisplayName("이미 종료된 그룹이면 가입할 수 없다")
            void joinGroup_AlreadyFinished() {
                // given
                Group group = org.mockito.Mockito.mock(Group.class);
                GroupRequest.Join request = new GroupRequest.Join("pass123");

                given(group.isFinished()).willReturn(true);
                given(groupRepository.findByInviteCodeForUpdate(inviteCode))
                        .willReturn(Optional.of(group));

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.joinGroup(inviteCode, memberId, request)
                )
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("이미 마감된 그룹입니다.");
            }

            @Test
            @DisplayName("그룹 정원이 가득 차면 가입할 수 없다")
            void joinGroup_LimitExceeded() {
                // given
                Group group = createActiveGroup();
                GroupRequest.Join request = new GroupRequest.Join("pass123");

                given(groupRepository.findByInviteCodeForUpdate(inviteCode))
                        .willReturn(Optional.of(group));
                given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(false);
                given(passwordEncoder.matches("pass123", "encoded_pass"))
                        .willReturn(true);
                given(groupMemberRepository.countByGroupIdAndStatus(
                        groupId, GroupMemberStatus.ACTIVE
                )).willReturn(5L);

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.joinGroup(inviteCode, memberId, request)
                )
                        .isInstanceOf(GroupLimitExceededException.class)
                        .hasMessageContaining("그룹의 최대 인원이 초과되어 입장할 수 없습니다.");
            }
        }
    }

    // 그룹 멤버 목록 조회
    @Nested
    @DisplayName("GetGroupMembers")
    class GetGroupMembers {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("활성 멤버이면 그룹 멤버 목록을 반환한다")
            void getGroupMembers() {
                // given
                given(groupMemberRepository.existsByGroupIdAndMemberIdAndStatus(
                        groupId, memberId, GroupMemberStatus.ACTIVE
                )).willReturn(true);
                given(groupMemberRepository.findByGroupIdWithHabit(groupId))
                        .willReturn(Collections.emptyList());

                // when
                List<GroupMemberResponse.Simple> result =
                        groupMemberService.getGroupMembers(groupId, memberId);

                // then
                assertThat(result).isNotNull();
                assertThat(result).isEmpty();
                verify(groupMemberRepository).findByGroupIdWithHabit(groupId);
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("그룹의 활성 멤버가 아니면 목록을 조회할 수 없다")
            void getGroupMembers_NoAccess() {
                // given
                given(groupMemberRepository.existsByGroupIdAndMemberIdAndStatus(
                        groupId, memberId, GroupMemberStatus.ACTIVE
                )).willReturn(false);

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.getGroupMembers(groupId, memberId)
                )
                        .isInstanceOf(ForbiddenException.class)
                        .hasMessageContaining("해당 그룹의 접근 권한이 없습니다.");

                verify(groupMemberRepository, never()).findByGroupIdWithHabit(groupId);
            }
        }
    }

    // 그룹 멤버 상세 조회
    @Nested
    @DisplayName("GetGroupMemberDetail")
    class GetGroupMemberDetail {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("활성 멤버이면 벌칙 횟수를 포함한 상세 정보를 반환한다")
            void getGroupMemberDetail() {
                // given
                GroupMemberResponse.Detail detail =
                        new GroupMemberResponse.Detail(
                                groupMemberId,
                                memberId,
                                "닉네임",
                                "아이디",
                                "MEMBER",
                                2L
                        );

                given(groupMemberRepository.existsByGroupIdAndMemberIdAndStatus(
                        groupId, memberId, GroupMemberStatus.ACTIVE
                )).willReturn(true);
                given(groupMemberRepository.findMemberDetailWithPenaltyCount(
                        groupId, groupMemberId
                )).willReturn(Optional.of(detail));

                // when
                GroupMemberResponse.Detail result =
                        groupMemberService.getGroupMemberDetail(
                                groupId, groupMemberId, memberId
                        );

                // then
                assertThat(result).isNotNull();
                assertThat(result.groupMemberId()).isEqualTo(groupMemberId);
                assertThat(result.penaltyCount()).isEqualTo(2L);
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("활성 멤버가 아니면 상세 정보를 조회할 수 없다")
            void getGroupMemberDetail_NoAccess() {
                // given
                given(groupMemberRepository.existsByGroupIdAndMemberIdAndStatus(
                        groupId, memberId, GroupMemberStatus.ACTIVE
                )).willReturn(false);

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.getGroupMemberDetail(
                                groupId, groupMemberId, memberId
                        )
                )
                        .isInstanceOf(ForbiddenException.class)
                        .hasMessageContaining("해당 그룹의 접근 권한이 없습니다.");

                verify(groupMemberRepository, never())
                        .findMemberDetailWithPenaltyCount(groupId, groupMemberId);
            }

            @Test
            @DisplayName("상세 조회 대상이 없으면 예외가 발생한다")
            void getGroupMemberDetail_NotFound() {
                // given
                given(groupMemberRepository.existsByGroupIdAndMemberIdAndStatus(
                        groupId, memberId, GroupMemberStatus.ACTIVE
                )).willReturn(true);
                given(groupMemberRepository.findMemberDetailWithPenaltyCount(
                        groupId, groupMemberId
                )).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.getGroupMemberDetail(
                                groupId, groupMemberId, memberId
                        )
                )
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining(
                                "존재하지 않거나 해당 그룹의 가입 멤버가 아닙니다."
                        );
            }
        }
    }

    // 그룹 탈퇴
    @Nested
    @DisplayName("LeaveGroup")
    class LeaveGroup {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("진행 중인 그룹에서 탈퇴하면 연관 데이터와 멤버가 삭제된다")
            void leaveGroup_ActiveMember() {
                // given
                Group group = createActiveGroup();
                GroupMember groupMember = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.MEMBER)
                        .build();

                ReflectionTestUtils.setField(groupMember, "id", groupMemberId);

                given(groupRepository.findByIdForUpdate(groupId))
                        .willReturn(Optional.of(group));
                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(groupMember));
                given(groupMemberRepository.countByGroupIdAndStatus(
                        groupId, GroupMemberStatus.ACTIVE
                )).willReturn(2L);
                given(habitRepository.findIdsByGroupMemberId(groupMemberId))
                        .willReturn(List.of(10L, 11L));

                // when
                groupMemberService.leaveGroup(groupId, memberId);

                // then
                verify(habitVerifyRepository).deleteByHabitIds(anyList());
                verify(penaltyVerifyRepository).deleteByGroupMemberId(groupMemberId);
                verify(habitRepository).deleteByIds(anyList());
                verify(groupMemberRepository).delete(groupMember);
            }

            @Test
            @DisplayName("종료된 그룹에서 탈퇴하면 멤버 상태를 LEFT로 변경한다")
            void leaveGroup_FinishedGroup() {
                // given
                Group group = org.mockito.Mockito.mock(Group.class);
                GroupMember groupMember = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.MEMBER)
                        .build();

                given(group.isFinished()).willReturn(true);
                given(groupRepository.findByIdForUpdate(groupId))
                        .willReturn(Optional.of(group));
                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(groupMember));
                given(groupMemberRepository.countByGroupIdAndStatus(
                        groupId, GroupMemberStatus.ACTIVE
                )).willReturn(2L);

                // when
                groupMemberService.leaveGroup(groupId, memberId);

                // then
                assertThat(groupMember.getStatus()).isEqualTo(GroupMemberStatus.LEFT);
                verify(groupMemberRepository, never()).delete(any(GroupMember.class));
                verify(habitRepository, never()).findIdsByGroupMemberId(groupMember.getId());
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("진행 중인 그룹에 다른 멤버가 남아 있으면 방장은 바로 탈퇴할 수 없다")
            void leaveGroup_OwnerCannotLeaveActiveGroup() {
                // given
                Group group = createActiveGroup();
                GroupMember owner = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.OWNER)
                        .build();

                given(groupRepository.findByIdForUpdate(groupId))
                        .willReturn(Optional.of(group));
                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(owner));
                given(groupMemberRepository.countByGroupIdAndStatus(
                        groupId, GroupMemberStatus.ACTIVE
                )).willReturn(2L);

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.leaveGroup(groupId, memberId)
                )
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining(
                                "방장은 다른 멤버가 남아있는 상태에서 바로 탈퇴할 수 없습니다."
                        );
            }

            @Test
            @DisplayName("존재하지 않는 그룹이면 탈퇴할 수 없다")
            void leaveGroup_GroupNotFound() {
                given(groupRepository.findByIdForUpdate(groupId))
                        .willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        groupMemberService.leaveGroup(groupId, memberId)
                )
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("존재하지 않는 그룹입니다.");
            }

            @Test
            @DisplayName("그룹에 가입하지 않은 멤버는 탈퇴할 수 없다")
            void leaveGroup_MemberNotFound() {
                Group group = createActiveGroup();

                given(groupRepository.findByIdForUpdate(groupId))
                        .willReturn(Optional.of(group));
                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        groupMemberService.leaveGroup(groupId, memberId)
                )
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("해당 그룹의 멤버가 아닙니다.");
            }

            @Test
            @DisplayName("이미 탈퇴한 멤버는 다시 탈퇴할 수 없다")
            void leaveGroup_AlreadyLeft() {
                Group group = createActiveGroup();
                GroupMember groupMember = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.MEMBER)
                        .status(GroupMemberStatus.LEFT)
                        .build();

                given(groupRepository.findByIdForUpdate(groupId))
                        .willReturn(Optional.of(group));
                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(groupMember));

                assertThatThrownBy(() ->
                        groupMemberService.leaveGroup(groupId, memberId)
                )
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("이미 탈퇴한 멤버입니다.");
            }
        }
    }

    @Nested
    @DisplayName("DeleteGroupMemberDataBulk")
    class DeleteGroupMemberDataBulk {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("습관 데이터가 없어도 벌칙 인증 데이터는 삭제한다")
            void deleteGroupMemberDataBulk_NoHabits() {
                // given
                given(habitRepository.findIdsByGroupMemberId(groupMemberId))
                        .willReturn(Collections.emptyList());

                // when
                groupMemberService.deleteGroupMemberDataBulk(groupMemberId);

                // then
                verify(habitRepository).findIdsByGroupMemberId(groupMemberId);
                verify(penaltyVerifyRepository)
                        .deleteByGroupMemberId(groupMemberId);

                verify(habitVerifyRepository, never()).deleteByHabitIds(anyList());
                verify(habitRepository, never()).deleteByIds(anyList());
            }
        }
    }

    // 멤버 추방
    @Nested
    @DisplayName("KickMember")
    class KickMember {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("방장이 진행 중인 그룹의 일반 멤버를 추방한다")
            void kickMember() {
                // given
                Group group = createActiveGroup();

                GroupMember owner = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.OWNER)
                        .build();
                ReflectionTestUtils.setField(owner, "id", groupMemberId);

                Long targetId = 55L;
                GroupMember target = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.MEMBER)
                        .build();
                ReflectionTestUtils.setField(target, "id", targetId);

                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(owner));
                given(groupMemberRepository.findById(targetId))
                        .willReturn(Optional.of(target));
                given(habitRepository.findIdsByGroupMemberId(targetId))
                        .willReturn(Collections.emptyList());

                // when
                groupMemberService.kickMember(groupId, targetId, memberId);

                // then
                verify(penaltyVerifyRepository).deleteByGroupMemberId(targetId);
                verify(groupMemberRepository).delete(target);
            }

            @Test
            @DisplayName("마지막 멤버가 탈퇴하면 연관 데이터를 삭제하고 그룹도 삭제한다")
            void leaveGroup_LastMember() {
                // given
                Group group = createActiveGroup();
                GroupMember groupMember = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.OWNER)
                        .build();

                given(groupRepository.findByIdForUpdate(groupId))
                        .willReturn(Optional.of(group));
                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(groupMember));
                given(groupMemberRepository.countByGroupIdAndStatus(
                        groupId, GroupMemberStatus.ACTIVE
                )).willReturn(1L);

                // deleteGroupAndAllMembers()에서 그룹을 다시 조회한다.
                given(groupRepository.findById(groupId))
                        .willReturn(Optional.of(group));

                // when
                groupMemberService.leaveGroup(groupId, memberId);

                // then
                verify(habitVerifyRepository).deleteHabitVerifiesByGroupId(groupId);
                verify(penaltyVerifyRepository).deletePenaltyVerifiesByGroupId(groupId);
                verify(habitRepository).deleteHabitsByGroupId(groupId);
                verify(groupMemberRepository).deleteGroupMembersByGroupId(groupId);
                verify(entityManager).flush();
                verify(entityManager).clear();
                verify(groupRepository).delete(group);
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("방장이 아닌 멤버는 다른 멤버를 추방할 수 없다")
            void kickMember_NotOwner() {
                // given
                Group group = createActiveGroup();
                GroupMember normalMember = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.MEMBER)
                        .build();

                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(normalMember));

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.kickMember(groupId, 55L, memberId)
                )
                        .isInstanceOf(ForbiddenException.class)
                        .hasMessageContaining("그룹 멤버 추방은 방장만 가능합니다.");
            }

            @Test
            @DisplayName("방장은 자기 자신을 추방할 수 없다")
            void kickMember_KickSelf() {
                // given
                Group group = createActiveGroup();
                GroupMember owner = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.OWNER)
                        .build();
                ReflectionTestUtils.setField(owner, "id", groupMemberId);

                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(owner));
                given(groupMemberRepository.findById(groupMemberId))
                        .willReturn(Optional.of(owner));

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.kickMember(groupId, groupMemberId, memberId)
                )
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("방장 스스로를 추방할 수 없습니다.");
            }

            @Test
            @DisplayName("추방 대상 멤버가 존재하지 않으면 예외가 발생한다")
            void kickMember_TargetNotFound() {
                // given
                Group group = createActiveGroup();
                GroupMember owner = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.OWNER)
                        .build();

                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(owner));
                given(groupMemberRepository.findById(groupMemberId))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.kickMember(groupId, groupMemberId, memberId)
                )
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("존재하지 않거나 해당 그룹의 멤버가 아닙니다.");
            }

            @Test
            @DisplayName("다른 그룹의 멤버는 추방할 수 없다")
            void kickMember_DifferentGroup() {
                // given
                Group group = createActiveGroup();
                Group otherGroup = Group.builder()
                        .deadline(LocalDate.now().plusDays(7))
                        .build();
                ReflectionTestUtils.setField(otherGroup, "id", 200L);

                GroupMember owner = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.OWNER)
                        .build();
                ReflectionTestUtils.setField(owner, "id", groupMemberId);

                GroupMember target = GroupMember.builder()
                        .group(otherGroup)
                        .role(GroupMemberRole.MEMBER)
                        .build();
                ReflectionTestUtils.setField(target, "id", 60L);

                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(owner));
                given(groupMemberRepository.findById(60L))
                        .willReturn(Optional.of(target));

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.kickMember(groupId, 60L, memberId)
                )
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("해당 그룹에 속한 멤버가 아닙니다.");
            }

            @Test
            @DisplayName("종료된 그룹에서는 멤버를 추방할 수 없다")
            void kickMember_FinishedGroup() {
                // given
                Group group = org.mockito.Mockito.mock(Group.class);
                given(group.getId()).willReturn(groupId);
                given(group.isFinished()).willReturn(true);

                GroupMember owner = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.OWNER)
                        .build();
                ReflectionTestUtils.setField(owner, "id", groupMemberId);

                GroupMember target = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.MEMBER)
                        .build();
                ReflectionTestUtils.setField(target, "id", 60L);

                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(owner));
                given(groupMemberRepository.findById(60L))
                        .willReturn(Optional.of(target));

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.kickMember(groupId, 60L, memberId)
                )
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("이미 종료된 그룹은 멤버를 추방할 수 없습니다.");
            }

            @Test
            void kickMember_LeftMember() {
                // given
                Group group = createActiveGroup();

                GroupMember owner = org.mockito.Mockito.mock(GroupMember.class);
                GroupMember kickTarget = org.mockito.Mockito.mock(GroupMember.class);

                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(owner));
                given(owner.getRole()).willReturn(GroupMemberRole.OWNER);

                given(groupMemberRepository.findById(groupMemberId))
                        .willReturn(Optional.of(kickTarget));
                given(kickTarget.getGroup()).willReturn(group);
                given(kickTarget.getId()).willReturn(groupMemberId);
                given(kickTarget.getStatus()).willReturn(GroupMemberStatus.LEFT);

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.kickMember(groupId, groupMemberId, memberId)
                )
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessage("현재 활동 중인 멤버만 추방할 수 있습니다.");

                verify(groupMemberRepository, never()).delete(kickTarget);
            }
        }
    }

    // 방장 권한 위임
    @Nested
    @DisplayName("TransferOwner")
    class TransferOwner {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("같은 그룹의 다른 멤버에게 방장 권한을 위임한다")
            void transferOwner() {
                // given
                Group group = createActiveGroup();

                GroupMember currentOwner = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.OWNER)
                        .build();
                ReflectionTestUtils.setField(currentOwner, "id", groupMemberId);

                Long nextOwnerId = 60L;
                GroupMember nextOwner = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.MEMBER)
                        .build();
                ReflectionTestUtils.setField(nextOwner, "id", nextOwnerId);

                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(currentOwner));
                given(groupMemberRepository.findById(nextOwnerId))
                        .willReturn(Optional.of(nextOwner));

                // when
                groupMemberService.transferOwner(groupId, nextOwnerId, memberId);

                // then
                assertThat(currentOwner.getRole()).isEqualTo(GroupMemberRole.MEMBER);
                assertThat(nextOwner.getRole()).isEqualTo(GroupMemberRole.OWNER);
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("방장은 자기 자신에게 권한을 위임할 수 없다")
            void transferOwner_ToSelf() {
                // given
                Group group = createActiveGroup();
                GroupMember owner = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.OWNER)
                        .build();
                ReflectionTestUtils.setField(owner, "id", groupMemberId);

                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(owner));
                given(groupMemberRepository.findById(groupMemberId))
                        .willReturn(Optional.of(owner));

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.transferOwner(groupId, groupMemberId, memberId)
                )
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining(
                                "자기 자신에게 방장 권한을 위임할 수 없습니다."
                        );
            }

            @Test
            @DisplayName("방장이 아닌 멤버는 방장 권한을 위임할 수 없다")
            void transferOwner_NotOwner() {
                Group group = createActiveGroup();
                GroupMember member = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.MEMBER)
                        .build();

                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(member));

                assertThatThrownBy(() ->
                        groupMemberService.transferOwner(groupId, 60L, memberId)
                )
                        .isInstanceOf(ForbiddenException.class)
                        .hasMessageContaining("방장 권한 위임은 현재 방장만 가능합니다.");
            }

            @Test
            @DisplayName("위임 대상 멤버가 존재하지 않으면 예외가 발생한다")
            void transferOwner_TargetNotFound() {
                Group group = createActiveGroup();
                GroupMember owner = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.OWNER)
                        .build();

                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(owner));
                given(groupMemberRepository.findById(60L))
                        .willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        groupMemberService.transferOwner(groupId, 60L, memberId)
                )
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("권한을 위임할 대상 멤버가 존재하지 않습니다.");
            }

            @Test
            @DisplayName("다른 그룹의 멤버에게는 방장 권한을 위임할 수 없다")
            void transferOwner_DifferentGroup() {
                Group group = createActiveGroup();
                Group otherGroup = Group.builder()
                        .deadline(LocalDate.now().plusDays(7))
                        .build();
                ReflectionTestUtils.setField(otherGroup, "id", 200L);

                GroupMember owner = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.OWNER)
                        .build();
                ReflectionTestUtils.setField(owner, "id", groupMemberId);

                GroupMember target = GroupMember.builder()
                        .group(otherGroup)
                        .role(GroupMemberRole.MEMBER)
                        .build();
                ReflectionTestUtils.setField(target, "id", 60L);

                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(owner));
                given(groupMemberRepository.findById(60L))
                        .willReturn(Optional.of(target));

                assertThatThrownBy(() ->
                        groupMemberService.transferOwner(groupId, 60L, memberId)
                )
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("해당 그룹에 속한 멤버가 아닙니다.");
            }

            @Test
            @DisplayName("종료된 그룹에서는 방장 권한을 위임할 수 없다")
            void transferOwner_FinishedGroup() {
                Group group = org.mockito.Mockito.mock(Group.class);
                given(group.getId()).willReturn(groupId);
                given(group.isFinished()).willReturn(true);

                GroupMember owner = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.OWNER)
                        .build();
                ReflectionTestUtils.setField(owner, "id", groupMemberId);

                GroupMember target = GroupMember.builder()
                        .group(group)
                        .role(GroupMemberRole.MEMBER)
                        .build();
                ReflectionTestUtils.setField(target, "id", 60L);

                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(owner));
                given(groupMemberRepository.findById(60L))
                        .willReturn(Optional.of(target));

                assertThatThrownBy(() ->
                        groupMemberService.transferOwner(groupId, 60L, memberId)
                )
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("이미 종료된 그룹은 방장을 위임할 수 없습니다.");
            }

            @Test
            void transferOwner_LeftMember() {
                // given
                Group group = createActiveGroup();

                GroupMember owner = org.mockito.Mockito.mock(GroupMember.class);
                GroupMember nextOwner = org.mockito.Mockito.mock(GroupMember.class);

                given(groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId))
                        .willReturn(Optional.of(owner));
                given(owner.getRole()).willReturn(GroupMemberRole.OWNER);
                given(owner.getGroup()).willReturn(group);

                given(groupMemberRepository.findById(groupMemberId))
                        .willReturn(Optional.of(nextOwner));
                given(nextOwner.getGroup()).willReturn(group);
                given(nextOwner.getId()).willReturn(groupMemberId);
                given(owner.getId()).willReturn(memberId);
                given(nextOwner.getStatus()).willReturn(GroupMemberStatus.LEFT);

                // when & then
                assertThatThrownBy(() ->
                        groupMemberService.transferOwner(groupId, groupMemberId, memberId)
                )
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessage("현재 활동 중인 멤버에게만 방장 권한을 위임할 수 있습니다.");

                verify(nextOwner, never()).changeRole(GroupMemberRole.OWNER);
            }
        }
    }
}
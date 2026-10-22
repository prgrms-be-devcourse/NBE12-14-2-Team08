
package com.back.domain.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.entity.Group;
import com.back.domain.group.entity.GroupStatus;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.entity.GroupMemberStatus;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.groupMember.service.GroupMemberService;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.BusinessRuleException;
import com.back.global.exception.EntityNotFoundException;
import com.back.global.exception.ForbiddenException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class GroupServiceTest {

    @InjectMocks
    private GroupService groupService;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private static final String BASE_INVITE_URL = "http://test-invite.com";
    private static final Long MEMBER_ID = 1L;
    private static final Long GROUP_ID = 100L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                groupService,
                "baseInviteUrl",
                BASE_INVITE_URL
        );
    }

    // =========================
    // 그룹 생성
    // =========================

    @Nested
    @DisplayName("그룹 생성")
    class CreateGroup {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("방장 권한으로 멤버가 정상 등록된다")
            void createGroup_Success() {
                // given
                GroupRequest.Create request = new GroupRequest.Create(
                        "테스트 그룹",
                        "설명",
                        LocalDate.now().plusDays(7),
                        "벌칙",
                        "pass123",
                        5
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

                ReflectionTestUtils.setField(
                        mockGroup,
                        "createDate",
                        LocalDateTime.now()
                );
                ReflectionTestUtils.setField(mockGroup, "id", GROUP_ID);

                given(groupRepository.existsByInviteCode(anyString()))
                        .willReturn(false);
                given(passwordEncoder.encode(request.password()))
                        .willReturn("encoded_pass");
                given(groupRepository.save(any(Group.class)))
                        .willReturn(mockGroup);
                given(memberRepository.findById(MEMBER_ID))
                        .willReturn(Optional.of(mockMember));

                // when
                GroupResponse.Detail response =
                        groupService.createGroup(MEMBER_ID, request);

                // then
                assertThat(response).isNotNull();
                assertThat(response.id()).isEqualTo(GROUP_ID);
                assertThat(response.inviteLink())
                        .startsWith(BASE_INVITE_URL);

                verify(groupRepository).save(any(Group.class));
                verify(groupMemberRepository)
                        .save(any(GroupMember.class));
            }
        }

        @Nested
        @DisplayName("예외")
        class Faliure {

            @Test
            @DisplayName("존재하지 않는 회원이면 EntityNotFoundException이 발생한다")
            void createGroup_MemberNotFound() {
                // given
                GroupRequest.Create request = new GroupRequest.Create(
                        "제목",
                        "설명",
                        null,
                        "벌칙",
                        "123",
                        5
                );

                Group mockGroup = Group.builder()
                        .title("제목")
                        .inviteCode("mockcode")
                        .build();

                ReflectionTestUtils.setField(
                        mockGroup,
                        "createDate",
                        LocalDateTime.now()
                );

                given(passwordEncoder.encode(anyString()))
                        .willReturn("encoded_password");
                given(groupRepository.save(any(Group.class)))
                        .willReturn(mockGroup);
                given(memberRepository.findById(MEMBER_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(
                        () -> groupService.createGroup(MEMBER_ID, request)
                )
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("존재하지 않는 회원입니다.");

                verify(groupMemberRepository, never())
                        .save(any(GroupMember.class));
            }
        }
    }

    // =========================
    // 그룹 조회
    // =========================

    @Nested
    @DisplayName("그룹 조회")
    class GetGroup {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("그룹 목록 조회 - 결과가 없으면 빈 목록을 반환한다")
            void getGroupSimpleList_Empty() {
                // given
                given(groupRepository.findMyGroupsWithCount(
                        MEMBER_ID,
                        GroupStatus.ACTIVE
                )).willReturn(Collections.emptyList());

                // when
                List<GroupResponse.Simple> result =
                        groupService.getGroupSimpleList(
                                MEMBER_ID,
                                GroupStatus.ACTIVE
                        );

                // then
                assertThat(result).isNotNull().isEmpty();

                verify(groupRepository).findMyGroupsWithCount(
                        MEMBER_ID,
                        GroupStatus.ACTIVE
                );
            }

            @Test
            @DisplayName("가입한 멤버가 그룹 상세 정보를 정상 조회한다")
            void getGroupDetail_Success() {
                // given
                Group mockGroup = Group.builder()
                        .title("그룹")
                        .inviteCode("code")
                        .deadline(LocalDate.now().plusDays(1))
                        .status(GroupStatus.ACTIVE)
                        .build();

                ReflectionTestUtils.setField(
                        mockGroup,
                        "createDate",
                        LocalDateTime.now()
                );

                given(groupRepository.findById(GROUP_ID))
                        .willReturn(Optional.of(mockGroup));

                given(groupMemberRepository
                        .existsByGroupIdAndMemberIdAndStatus(
                                GROUP_ID,
                                MEMBER_ID,
                                GroupMemberStatus.ACTIVE
                        )).willReturn(true);

                // when
                GroupResponse.Detail response =
                        groupService.getGroupDetail(GROUP_ID, MEMBER_ID);

                // then
                assertThat(response).isNotNull();
                assertThat(mockGroup.getStatus())
                        .isEqualTo(GroupStatus.ACTIVE);

                verify(groupRepository).findById(GROUP_ID);
                verify(groupMemberRepository)
                        .existsByGroupIdAndMemberIdAndStatus(
                                GROUP_ID,
                                MEMBER_ID,
                                GroupMemberStatus.ACTIVE
                        );
            }
        }

        @Nested
        @DisplayName("예외")
        class Exception {

            @Test
            @DisplayName("가입하지 않은 멤버는 그룹 상세 정보에 접근할 수 없다")
            void getGroupDetail_Forbidden() {
                // given
                given(groupRepository.findById(GROUP_ID))
                        .willReturn(Optional.of(Group.builder().build()));

                given(groupMemberRepository
                        .existsByGroupIdAndMemberIdAndStatus(
                                GROUP_ID,
                                MEMBER_ID,
                                GroupMemberStatus.ACTIVE
                        )).willReturn(false);

                // when & then
                assertThatThrownBy(
                        () -> groupService.getGroupDetail(
                                GROUP_ID,
                                MEMBER_ID
                        )
                )
                        .isInstanceOf(ForbiddenException.class)
                        .hasMessageContaining(
                                "해당 그룹의 접근 권한이 없습니다."
                        );
            }
        }
    }

    // =========================
    // 그룹 수정
    // =========================

    @Nested
    @DisplayName("그룹 수정")
    class UpdateGroup {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("방장이 그룹 정보를 변경한다")
            void updateGroup_Success() {
                // given
                Group mockGroup = Group.builder()
                        .title("기존")
                        .status(GroupStatus.ACTIVE)
                        .build();

                GroupMember mockGroupMember = GroupMember.builder()
                        .role(GroupMemberRole.OWNER)
                        .build();

                GroupRequest.Update request = new GroupRequest.Update(
                        "새제목",
                        null,
                        null,
                        null,
                        "newpass",
                        10
                );

                given(groupRepository.findByIdForUpdate(GROUP_ID))
                        .willReturn(Optional.of(mockGroup));

                given(groupMemberRepository.findByGroupIdAndMemberId(
                        GROUP_ID,
                        MEMBER_ID
                )).willReturn(Optional.of(mockGroupMember));

                given(passwordEncoder.encode("newpass"))
                        .willReturn("encoded_newpass");

                // when
                groupService.updateGroup(GROUP_ID, MEMBER_ID, request);

                // then
                assertThat(mockGroup.getTitle()).isEqualTo("새제목");

                verify(groupRepository).findByIdForUpdate(GROUP_ID);
                verify(groupMemberRepository)
                        .findByGroupIdAndMemberId(GROUP_ID, MEMBER_ID);
                verify(passwordEncoder).encode("newpass");
            }
        }

        @Nested
        @DisplayName("예외")
        class Exception {

            @Test
            @DisplayName("이미 종료된 그룹은 수정할 수 없다")
            void updateGroup_AlreadyFinished() {
                // given
                Group mockGroup = Group.builder()
                        .title("기존")
                        .status(GroupStatus.FINISH)
                        .build();

                GroupRequest.Update request = new GroupRequest.Update(
                        "제목",
                        null,
                        null,
                        null,
                        null,
                        5
                );

                given(groupRepository.findByIdForUpdate(GROUP_ID))
                        .willReturn(Optional.of(mockGroup));

                // when & then
                assertThatThrownBy(
                        () -> groupService.updateGroup(
                                GROUP_ID,
                                MEMBER_ID,
                                request
                        )
                )
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining(
                                "이미 종료된 그룹은 수정할 수 없습니다."
                        );
            }

            @Test
            @DisplayName("방장이 아닌 멤버는 그룹을 수정할 수 없다")
            void updateGroup_NotOwner() {
                // given
                Group mockGroup = Group.builder()
                        .title("기존")
                        .status(GroupStatus.ACTIVE)
                        .build();

                GroupMember mockGroupMember = GroupMember.builder()
                        .role(GroupMemberRole.MEMBER)
                        .build();

                GroupRequest.Update request = new GroupRequest.Update(
                        "제목",
                        null,
                        null,
                        null,
                        null,
                        5
                );

                given(groupRepository.findByIdForUpdate(GROUP_ID))
                        .willReturn(Optional.of(mockGroup));

                given(groupMemberRepository.findByGroupIdAndMemberId(
                        GROUP_ID,
                        MEMBER_ID
                )).willReturn(Optional.of(mockGroupMember));

                // when & then
                assertThatThrownBy(
                        () -> groupService.updateGroup(
                                GROUP_ID,
                                MEMBER_ID,
                                request
                        )
                )
                        .isInstanceOf(ForbiddenException.class)
                        .hasMessageContaining(
                                "그룹 수정은 방장만 가능합니다."
                        );
            }
        }
    }

    // =========================
    // 그룹 삭제
    // =========================

    @Nested
    @DisplayName("그룹 삭제")
    class DeleteGroup {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("방장 혼자 남은 그룹을 삭제한다")
            void deleteGroup_Success() {
                // given
                Group mockGroup = Group.builder()
                        .status(GroupStatus.ACTIVE)
                        .build();

                GroupMember mockGroupMember = GroupMember.builder()
                        .role(GroupMemberRole.OWNER)
                        .build();

                ReflectionTestUtils.setField(
                        mockGroupMember,
                        "id",
                        50L
                );

                given(groupRepository.findByIdForUpdate(GROUP_ID))
                        .willReturn(Optional.of(mockGroup));

                given(groupMemberRepository.findByGroupIdAndMemberId(
                        GROUP_ID,
                        MEMBER_ID
                )).willReturn(Optional.of(mockGroupMember));

                given(groupMemberRepository.countByGroupIdAndStatus(
                        GROUP_ID,
                        GroupMemberStatus.ACTIVE
                )).willReturn(1L);

                // when
                groupService.deleteGroup(GROUP_ID, MEMBER_ID);

                // then
                verify(groupMemberService, times(1))
                        .deleteGroupAndAllMembers(mockGroup);
            }
        }

        @Nested
        @DisplayName("예외")
        class Exception {

            @Test
            @DisplayName("이미 종료된 그룹은 삭제할 수 없다")
            void deleteGroup_AlreadyFinished() {
                // given
                Group mockGroup = Group.builder()
                        .status(GroupStatus.FINISH)
                        .build();

                given(groupRepository.findByIdForUpdate(GROUP_ID))
                        .willReturn(Optional.of(mockGroup));

                // when & then
                assertThatThrownBy(
                        () -> groupService.deleteGroup(
                                GROUP_ID,
                                MEMBER_ID
                        )
                )
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining(
                                "이미 종료된 그룹은 삭제할 수 없습니다."
                        );

                verify(groupMemberService, never())
                        .deleteGroupAndAllMembers(any(Group.class));
            }

            @Test
            @DisplayName("방장이 아닌 멤버는 그룹을 삭제할 수 없다")
            void deleteGroup_NotOwner() {
                // given
                Group mockGroup = Group.builder()
                        .status(GroupStatus.ACTIVE)
                        .build();

                GroupMember mockGroupMember = GroupMember.builder()
                        .role(GroupMemberRole.MEMBER)
                        .build();

                given(groupRepository.findByIdForUpdate(GROUP_ID))
                        .willReturn(Optional.of(mockGroup));

                given(groupMemberRepository.findByGroupIdAndMemberId(
                        GROUP_ID,
                        MEMBER_ID
                )).willReturn(Optional.of(mockGroupMember));

                // when & then
                assertThatThrownBy(
                        () -> groupService.deleteGroup(
                                GROUP_ID,
                                MEMBER_ID
                        )
                )
                        .isInstanceOf(ForbiddenException.class)
                        .hasMessageContaining(
                                "그룹 삭제는 방장만 가능합니다."
                        );

                verify(groupMemberService, never())
                        .deleteGroupAndAllMembers(any(Group.class));
            }

            @Test
            @DisplayName("다른 멤버가 남아 있으면 그룹을 삭제할 수 없다")
            void deleteGroup_HasOtherMembers() {
                // given
                Group mockGroup = Group.builder()
                        .status(GroupStatus.ACTIVE)
                        .deadline(LocalDate.now().plusDays(2))
                        .build();

                GroupMember mockGroupMember = GroupMember.builder()
                        .role(GroupMemberRole.OWNER)
                        .build();

                given(groupRepository.findByIdForUpdate(GROUP_ID))
                        .willReturn(Optional.of(mockGroup));

                given(groupMemberRepository.findByGroupIdAndMemberId(
                        GROUP_ID,
                        MEMBER_ID
                )).willReturn(Optional.of(mockGroupMember));

                given(groupMemberRepository.countByGroupIdAndStatus(
                        GROUP_ID,
                        GroupMemberStatus.ACTIVE
                )).willReturn(3L);

                // when & then
                assertThatThrownBy(
                        () -> groupService.deleteGroup(
                                GROUP_ID,
                                MEMBER_ID
                        )
                )
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining(
                                "그룹을 삭제하려면 방장을 제외한 모든 멤버가 퇴장해야 합니다."
                        );

                verify(groupMemberService, never())
                        .deleteGroupAndAllMembers(any(Group.class));
            }
        }
    }

    // =========================
    // 그룹 자동 종료
    // =========================

    @Nested
    @DisplayName("그룹 자동 종료")
    class AutoCloseExpiredGroups {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("만료된 그룹을 자동 종료한다")
            void autoCloseExpiredGroups_Success() {
                // given
                LocalDate today = LocalDate.now();

                Group mockGroup1 = Group.builder()
                        .status(GroupStatus.ACTIVE)
                        .build();

                Group mockGroup2 = Group.builder()
                        .status(GroupStatus.ACTIVE)
                        .build();

                List<Group> expiredGroups =
                        List.of(mockGroup1, mockGroup2);

                given(groupRepository.findExpiredGroupsForUpdate(today))
                        .willReturn(expiredGroups);

                // when
                groupService.autoCloseExpiredGroups();

                // then
                verify(groupRepository)
                        .findExpiredGroupsForUpdate(today);

                assertThat(mockGroup1.getStatus())
                        .isEqualTo(GroupStatus.FINISH);
                assertThat(mockGroup2.getStatus())
                        .isEqualTo(GroupStatus.FINISH);
            }
        }
    }
}

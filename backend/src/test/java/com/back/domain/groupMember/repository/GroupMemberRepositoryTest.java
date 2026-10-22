
package com.back.domain.groupMember.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.dto.GroupMemberResponse;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.entity.GroupMemberStatus;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.penaltyverify.entity.PenaltyVerify;
import com.back.domain.penaltyverify.repository.PenaltyVerifyRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
class GroupMemberRepositoryTest {

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Group group;
    private Member member;
    private GroupMember groupMember;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private PenaltyVerifyRepository penaltyVerifyRepository;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(
                Member.create("테스트회원", "test-user", "encoded-password")
        );

        group = groupRepository.save(
                Group.builder()
                        .title("테스트 그룹")
                        .description("테스트 설명")
                        .deadline(LocalDate.now().plusDays(7))
                        .penalty("벌칙")
                        .password("1234")
                        .inviteCode("TEST-CODE")
                        .memberLimit(5)
                        .build()
        );

        groupMember = groupMemberRepository.save(
                GroupMember.create(group, member, GroupMemberRole.MEMBER)
        );

        groupMemberRepository.flush();
    }

    @Test
    @DisplayName("그룹 ID와 회원 ID로 그룹 멤버를 조회한다")
    void findByGroupIdAndMemberId() {
        // when
        var result = groupMemberRepository.findByGroupIdAndMemberId(
                group.getId(),
                member.getId()
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(groupMember.getId());
    }

    @Test
    @DisplayName("그룹 ID와 회원 ID로 그룹 멤버 존재 여부를 확인한다")
    void existsByGroupIdAndMemberId() {
        // when & then
        assertThat(
                groupMemberRepository.existsByGroupIdAndMemberId(
                        group.getId(),
                        member.getId()
                )
        ).isTrue();

        assertThat(
                groupMemberRepository.existsByGroupIdAndMemberId(
                        group.getId(),
                        -1L
                )
        ).isFalse();
    }

    @Test
    @DisplayName("회원 ID로 그룹 멤버 존재 여부를 확인한다")
    void existsByMemberId() {
        // when & then
        assertThat(groupMemberRepository.existsByMemberId(member.getId()))
                .isTrue();

        assertThat(groupMemberRepository.existsByMemberId(-1L))
                .isFalse();
    }

    @Test
    @DisplayName("그룹 ID로 멤버 수를 조회한다")
    void countByGroupId() {
        // when
        long count = groupMemberRepository.countByGroupId(group.getId());

        // then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("그룹 ID와 상태로 멤버 수를 조회한다")
    void countByGroupIdAndStatus() {
        // when
        long activeCount = groupMemberRepository.countByGroupIdAndStatus(
                group.getId(),
                GroupMemberStatus.ACTIVE
        );

        long leftCount = groupMemberRepository.countByGroupIdAndStatus(
                group.getId(),
                GroupMemberStatus.LEFT
        );

        // then
        assertThat(activeCount).isEqualTo(1);
        assertThat(leftCount).isZero();
    }

    @Test
    @DisplayName("그룹 ID와 상태로 멤버 존재 여부를 확인한다")
    void existsByGroupIdAndMemberIdAndStatus() {
        // when & then
        assertThat(
                groupMemberRepository.existsByGroupIdAndMemberIdAndStatus(
                        group.getId(),
                        member.getId(),
                        GroupMemberStatus.ACTIVE
                )
        ).isTrue();

        assertThat(
                groupMemberRepository.existsByGroupIdAndMemberIdAndStatus(
                        group.getId(),
                        member.getId(),
                        GroupMemberStatus.LEFT
                )
        ).isFalse();
    }

    @Test
    @DisplayName("그룹 멤버 ID와 그룹 ID로 존재 여부를 확인한다")
    void existsByIdAndGroupId() {
        // when & then
        assertThat(
                groupMemberRepository.existsByIdAndGroupId(
                        groupMember.getId(),
                        group.getId()
                )
        ).isTrue();

        assertThat(
                groupMemberRepository.existsByIdAndGroupId(
                        groupMember.getId(),
                        -1L
                )
        ).isFalse();
    }

    @Test
    @DisplayName("그룹 ID로 그룹 멤버 목록을 조회한다")
    void findAllByGroupId() {
        // when
        var result = groupMemberRepository.findAllByGroupId(group.getId());

        // then
        assertThat(result)
                .hasSize(1)
                .extracting(GroupMember::getId)
                .containsExactly(groupMember.getId());
    }

    @Test
    @DisplayName("회원 ID로 해당 회원의 그룹 멤버 목록을 조회한다")
    void findAllByMemberId() {
        // when
        var result = groupMemberRepository.findAllByMemberId(member.getId());

        // then
        assertThat(result)
                .hasSize(1)
                .extracting(GroupMember::getId)
                .containsExactly(groupMember.getId());
    }

    @Test
    @Transactional
    @DisplayName("그룹 ID와 회원 ID로 그룹 멤버를 잠금 조회한다")
    void findByGroupIdAndMemberIdForUpdate() {
        // when
        var result = groupMemberRepository.findByGroupIdAndMemberIdForUpdate(
                group.getId(),
                member.getId()
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(groupMember.getId());
    }

    @Test
    @DisplayName("그룹 ID로 멤버 목록과 활성 습관 정보를 조회한다")
    void findByGroupIdWithHabit() {
        // given: 기존 멤버에게 활성 습관 추가
        Habit habit = habitRepository.save(
                Habit.create(groupMember, "운동하기", "매일 운동하기", 7)
        );

        // given: 습관이 없는 두 번째 멤버 추가
        Member anotherMember = memberRepository.save(
                Member.create("두번째회원", "test-user-2", "encoded-password")
        );

        groupMemberRepository.save(
                GroupMember.create(group, anotherMember, GroupMemberRole.MEMBER)
        );

        groupMemberRepository.flush();

        // when
        var result = groupMemberRepository.findByGroupIdWithHabit(group.getId());

        // then
        assertThat(result).hasSize(2);

        GroupMemberResponse.Simple memberWithHabit = result.stream()
                .filter(item -> item.groupMemberId().equals(groupMember.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(memberWithHabit.memberId()).isEqualTo(member.getId());
        assertThat(memberWithHabit.nickname()).isEqualTo("테스트회원");
        assertThat(memberWithHabit.username()).isEqualTo("test-user");
        assertThat(memberWithHabit.role()).isEqualTo("MEMBER");
        assertThat(memberWithHabit.habitId()).isEqualTo(habit.getId());
        assertThat(memberWithHabit.habitTitle()).isEqualTo("운동하기");
        assertThat(memberWithHabit.habitDescription()).isEqualTo("매일 운동하기");

        GroupMemberResponse.Simple memberWithoutHabit = result.stream()
                .filter(item -> item.memberId().equals(anotherMember.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(memberWithoutHabit.habitId()).isNull();
        assertThat(memberWithoutHabit.habitTitle()).isNull();
        assertThat(memberWithoutHabit.habitDescription()).isNull();
    }

    @Test
    @DisplayName("그룹 ID와 그룹 멤버 ID로 벌칙 횟수를 포함한 멤버 상세 정보를 조회한다")
    void findMemberDetailWithPenaltyCount() {
        var result = groupMemberRepository.findMemberDetailWithPenaltyCount(
                group.getId(),
                groupMember.getId()
        );

        assertThat(result).isPresent();

        var detail = result.get();

        assertThat(detail.groupMemberId()).isEqualTo(groupMember.getId());
        assertThat(detail.memberId()).isEqualTo(member.getId());
        assertThat(detail.nickname()).isEqualTo("테스트회원");
        assertThat(detail.username()).isEqualTo("test-user");
        assertThat(detail.role()).isEqualTo("MEMBER");
        assertThat(detail.penaltyCount()).isZero();
    }

    @Test
    @DisplayName("멤버의 벌칙 인증 기록이 여러 건이면 해당 개수를 반환한다")
    void findMemberDetailWithPenaltyCount_multiplePenalties() {
        Habit habit1 = habitRepository.save(
                Habit.create(groupMember, "운동하기", "매일 운동하기", 7)
        );

        Habit habit2 = habitRepository.save(
                Habit.create(groupMember, "독서하기", "매일 독서하기", 7)
        );

        penaltyVerifyRepository.save(
                PenaltyVerify.create(groupMember, habit1)
        );

        penaltyVerifyRepository.save(
                PenaltyVerify.create(groupMember, habit2)
        );

        penaltyVerifyRepository.flush();

        var result = groupMemberRepository.findMemberDetailWithPenaltyCount(
                group.getId(),
                groupMember.getId()
        );

        assertThat(result).isPresent();
        assertThat(result.get().penaltyCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("다른 그룹의 멤버 ID로 상세 정보를 조회하면 결과가 없다")
    void findMemberDetailWithPenaltyCount_wrongGroup() {
        Group anotherGroup = groupRepository.save(
                Group.builder()
                        .title("다른 그룹")
                        .description("다른 그룹 설명")
                        .deadline(LocalDate.now().plusDays(7))
                        .penalty("다른 벌칙")
                        .password("5678")
                        .inviteCode("ANOTHER-CODE")
                        .memberLimit(5)
                        .build()
        );

        var result = groupMemberRepository.findMemberDetailWithPenaltyCount(
                anotherGroup.getId(),
                groupMember.getId()
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("활성 습관이 없는 멤버도 그룹 멤버 목록에 포함된다")
    void findByGroupIdWithHabit_withoutActiveHabit() {
        var result = groupMemberRepository.findByGroupIdWithHabit(group.getId());

        assertThat(result).hasSize(1);

        GroupMemberResponse.Simple simple = result.get(0);

        assertThat(simple.groupMemberId()).isEqualTo(groupMember.getId());
        assertThat(simple.habitId()).isNull();
        assertThat(simple.habitTitle()).isNull();
        assertThat(simple.habitDescription()).isNull();
    }

    @Test
    @DisplayName("그룹 ID로 해당 그룹의 모든 멤버를 삭제한다")
    void deleteGroupMembersByGroupId() {
        int deletedCount =
                groupMemberRepository.deleteGroupMembersByGroupId(group.getId());

        assertThat(deletedCount).isEqualTo(1);
        assertThat(groupMemberRepository.countByGroupId(group.getId())).isZero();

        // 그룹 자체는 삭제되지 않아야 한다.
        assertThat(groupRepository.findById(group.getId())).isPresent();

        // 회원 계정도 삭제되지 않아야 한다.
        assertThat(memberRepository.findById(member.getId())).isPresent();
    }
}

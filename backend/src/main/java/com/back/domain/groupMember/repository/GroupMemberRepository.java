package com.back.domain.groupMember.repository;

import com.back.domain.groupMember.dto.GroupMemberResponse;
import com.back.domain.groupMember.entity.GroupMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    @Query("select new com.back.domain.groupMember.dto.GroupMemberResponse$Simple(" +
            "gm.id, m.id, m.nickname, m.username, cast(gm.role as string), h.id, h.title, h.description) " +
            "from GroupMember gm " +
            "join gm.member m " +
            "left join Habit h on h.groupMember.id = gm.id and h.status = com.back.domain.habit.entity.HabitStatus.ACTIVE " +
            "where gm.group.id = :groupId")
    List<GroupMemberResponse.Simple> findByGroupIdWithHabit(
            @Param("groupId") Long groupId);

    @Query("select new com.back.domain.groupMember.dto.GroupMemberResponse$Detail(" +
            "gm.id, m.id, m.nickname, m.username, cast(gm.role as string), cast(count(pv) as int)) " +
            "from GroupMember gm " +
            "join gm.member m " +
            "left join PenaltyVerify pv on pv.groupMember.id = gm.id " +
            "where gm.group.id = :groupId and gm.id = :groupMemberId " +
            "group by gm.id, m.id, m.nickname, m.username, gm.role")
    Optional<GroupMemberResponse.Detail> findMemberDetailWithPenaltyCount(
            @Param("groupId") Long groupId,
            @Param("groupMemberId") Long groupMemberId);

    Optional<GroupMember> findByGroupIdAndMemberId(Long groupId, Long memberId);
    boolean existsByGroupIdAndMemberId(Long groupId, Long memberId);

    boolean existsByMemberId(Long memberId);

    @EntityGraph(attributePaths = "group")
    List<GroupMember> findAllByMemberId(Long memberId); //특정 회원이 들어갔던 방 정보 불러오기

    long countByGroupId(Long groupId);

    boolean existsByIdAndGroupId(Long groupMemberId, Long groupId);
}
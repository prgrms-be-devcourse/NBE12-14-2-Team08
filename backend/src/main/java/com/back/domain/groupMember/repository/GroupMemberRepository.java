package com.back.domain.groupMember.repository;

import com.back.domain.groupMember.dto.GroupMemberResponse;
import com.back.domain.groupMember.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    @Query("select new com.back.domain.groupMember.dto.GroupMemberResponse$Simple(" +
            "gm.id, m.id, m.nickname, cast(gm.role as string), h.title, h.description) " +
            "from GroupMember gm " +
            "join gm.member m " +
            "left join Habit h on h.groupMember.id = gm.id " +
            "where gm.group.id = :groupId")
    List<GroupMemberResponse.Simple> findByGroupIdWithHabit(@Param("groupId") Long groupId);

    Optional<GroupMember> findByGroupIdAndMemberId(Long groupId, Long memberId);
    boolean existsByGroupIdAndMemberId(Long groupId, Long memberId);

    boolean existsByMemberId(Long memberId);

    long countByGroupId(Long groupId);
}
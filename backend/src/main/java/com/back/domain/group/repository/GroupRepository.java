package com.back.domain.group.repository;

import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.entity.Group;
import com.back.domain.group.entity.GroupStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByInviteCode(String inviteCode);
    boolean existsByInviteCode(String inviteCode);

    @Query("select new com.back.domain.group.dto.GroupResponse$Simple(g.id, g.title, g.description, g.memberLimit, count(gm), cast(g.status as string))" +
            "from Group g " +
            "left join GroupMember gm on gm.group.id = g.id " +
            "where g.status = :status " +
            "and g.id in (select gm2.group.id from GroupMember gm2 where gm2.member.id = :memberId) " +
            "group by g.id, g.title, g.description, g.memberLimit, g.status")
    List<GroupResponse.Simple> findMyGroupsWithCount(@Param("memberId") Long memberId,
                                                     @Param("status") GroupStatus status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Group g SET g.status = com.back.domain.group.entity.GroupStatus.FINISH " +
            "WHERE g.status = com.back.domain.group.entity.GroupStatus.ACTIVE AND g.deadline < :today")
    int bulkFinishExpiredGroups(@Param("today") LocalDate today);
}
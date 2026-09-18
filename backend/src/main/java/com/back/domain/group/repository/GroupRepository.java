package com.back.domain.group.repository;

import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.dto.GroupResponse.Simple;
import com.back.domain.group.entity.Group;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByInviteCode(String inviteCode);
    boolean existsByInviteCode(String inviteCode);

    @Query("select new com.back.domain.group.dto.GroupResponse$Simple(g.id, g.title, g.description, g.memberLimit, count(gm))" +
            "from Group g " +
            "left join GroupMember gm on gm.group.id = g.id " +
            "where g.id in (select gm2.group.id from GroupMember gm2 where gm2.member.id = :memberId) " +
            "group by g.id, g.title, g.description, g.memberLimit")
    List<GroupResponse.Simple> findMyGroupsWithCount(@Param("memberId") Long memberId);
}
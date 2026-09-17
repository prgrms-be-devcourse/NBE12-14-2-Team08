package com.back.domain.groupMember.repository;

import com.back.domain.groupMember.entity.GroupMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByGroupId(Long groupId);
    Optional<GroupMember> findByGroupIdAndMemberId(Long groupId, Long memberId);
    boolean existsByGroupIdAndMemberId(Long groupId, Long memberId);
}
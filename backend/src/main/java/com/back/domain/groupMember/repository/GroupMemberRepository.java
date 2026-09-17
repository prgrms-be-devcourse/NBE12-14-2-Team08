package com.back.domain.groupMember.repository;

import com.back.domain.groupMember.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByGroupId(Long groupId);

    Optional<GroupMember> findByGroupIdAndMemberId(Long groupId, Long memberId);

    boolean existsByGroupIdAndMemberId(Long groupId, Long memberId);

    boolean existsByMemberId(Long memberId);

    Optional<GroupMember> findByIdAndMemberId(Long groupMemberId, Long memberId);
}
package com.back.domain.groupMember.repository;

import com.back.domain.groupMember.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupMemberRepository
        extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroup_IdAndMember_Id(
            Long groupId,
            Long memberId
    );
}
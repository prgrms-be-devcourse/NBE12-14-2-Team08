package com.back.domain.groupMember.repository;

import com.back.domain.groupMember.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupMemberRepository
        extends JpaRepository<GroupMember, Integer> {

    Optional<GroupMember> findByGroup_IdAndMember_Id(
            int groupId,
            int memberId
    );
}
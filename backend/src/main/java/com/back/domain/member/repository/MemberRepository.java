package com.back.domain.member.repository;

import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByUsername(String username);

    Optional<Member> findByUsername(String username);

    boolean existsByIdAndStatus(Long id, MemberStatus status);
}
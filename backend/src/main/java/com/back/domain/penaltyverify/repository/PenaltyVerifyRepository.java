package com.back.domain.penaltyverify.repository;

import com.back.domain.penaltyverify.entity.PenaltyVerify;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PenaltyVerifyRepository extends JpaRepository<PenaltyVerify, Long> {

    @Override
    @EntityGraph(attributePaths = {"groupMember", "groupMember.group", "groupMember.member"})
    Optional<PenaltyVerify> findById(Long id);

    List<PenaltyVerify> findByHabitId(Long habitId);

    @Query("""
        SELECT pv FROM PenaltyVerify pv
        JOIN FETCH pv.groupMember gm
        JOIN FETCH gm.member
        WHERE gm.group.id = :groupId AND pv.status = 'PENDING'
    """)
    List<PenaltyVerify> findPendingByGroupId(Long groupId);


    @Query("""
        SELECT pv FROM PenaltyVerify pv
        WHERE pv.id IN :ids AND pv.groupMember.group.id = :groupId
    """)
    List<PenaltyVerify> findAllByIdInAndGroupId(@Param("ids") List<Long> ids, @Param("groupId") Long groupId);

    boolean existsByGroupMemberIdAndHabitIdAndVerifyDate(
        Long groupMemberId, Long habitId, LocalDate verifyDate
    );
}

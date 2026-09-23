package com.back.domain.habitVerify.repository;

import com.back.domain.habitVerify.entity.HabitVerify;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HabitVerifyRepository
        extends JpaRepository<HabitVerify, Long> {

    List<HabitVerify> findAllByHabitIdOrderByVerifyDateDesc(
            Long habitId
    );

    Optional<HabitVerify> findByIdAndHabitId(
            Long id,
            Long habitId
    );

    boolean existsByHabitIdAndVerifyDate(
            Long habitId,
            LocalDate verifyDate
    );

    boolean existsByHabitIdAndVerifyDateAndIdNot(
            Long habitId,
            LocalDate verifyDate,
            Long id
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from HabitVerify hv where hv.habit.id in :habitIds")
    void deleteByHabitIds(@Param("habitIds") List<Long> habitIds);

    @Query("""
        SELECT hv
        FROM HabitVerify hv
        JOIN FETCH hv.habit h
        JOIN FETCH h.groupMember gm
        JOIN FETCH gm.member m
        WHERE gm.group.id = :groupId
          AND hv.status = 'PENDING'
        ORDER BY hv.verifyDate DESC
        """)
    List<HabitVerify> findPendingByGroupId(
            @Param("groupId") Long groupId
    );
}
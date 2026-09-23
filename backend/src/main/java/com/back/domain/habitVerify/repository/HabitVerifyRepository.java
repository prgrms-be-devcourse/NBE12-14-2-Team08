package com.back.domain.habitVerify.repository;

import com.back.domain.habitVerify.entity.HabitVerify;
import com.back.domain.habitVerify.entity.HabitVerifyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // 이번 주 정산 구간 내 특정 상태의 제출 건수 조회 (주간 인증 횟수 판정용)
    long countByHabit_IdAndCreateDateBetweenAndStatus(
            Long habitId,
            LocalDateTime weekStart,
            LocalDateTime weekEnd,
            HabitVerifyStatus status
    );

    // 이번 주 정산 구간 내 특정 상태의 제출 건 목록 조회 (그레이스 기간 만료 후 자동 승인 처리용)
    List<HabitVerify> findAllByHabit_IdAndCreateDateBetweenAndStatus(
            Long habitId,
            LocalDateTime weekStart,
            LocalDateTime weekEnd,
            HabitVerifyStatus status
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
    @Query("""
    SELECT hv
    FROM HabitVerify hv
    JOIN hv.habit h
    JOIN h.groupMember gm
    WHERE hv.id IN :ids
      AND gm.group.id = :groupId
    """)
    List<HabitVerify> findAllByIdInAndGroupId(
            @Param("ids") List<Long> ids,
            @Param("groupId") Long groupId
    );
}
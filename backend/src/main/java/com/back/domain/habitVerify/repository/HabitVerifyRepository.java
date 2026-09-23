package com.back.domain.habitVerify.repository;

import com.back.domain.habitVerify.entity.HabitVerify;
import com.back.domain.habitVerify.entity.HabitVerifyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
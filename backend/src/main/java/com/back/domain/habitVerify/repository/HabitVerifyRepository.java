package com.back.domain.habitVerify.repository;

import com.back.domain.habitVerify.entity.HabitVerify;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
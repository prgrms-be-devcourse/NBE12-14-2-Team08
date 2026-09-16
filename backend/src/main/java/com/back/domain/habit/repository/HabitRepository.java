package com.back.domain.habit.repository;

import com.back.domain.habit.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HabitRepository extends JpaRepository<Habit, Long> {
    boolean existsByGroupMember_Id(Long groupMemberId);
    Optional<Habit> findByIdAndGroupMember_Member_Id(
            int habitId,
            int memberId
    );
}

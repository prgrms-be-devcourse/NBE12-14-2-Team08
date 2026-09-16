package com.back.domain.habit.repository;

import com.back.domain.habit.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitRepository extends JpaRepository<Habit, Long> {
    boolean existsByGroupMember_Id(Long groupMemberId);
}

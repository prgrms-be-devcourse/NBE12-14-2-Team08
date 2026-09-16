package com.back.domain.habit.repository;

import com.back.domain.habit.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

public class HabitRepository extends JpaRepository<Habit, Long> {
}

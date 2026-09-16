package com.back.domain.habit.service;

import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HabitService {
    private final HabitRepository habitRepository;

    public Optional<Habit> findById(Long habitId) {
        return habitRepository.findById(habitId);
    }
}

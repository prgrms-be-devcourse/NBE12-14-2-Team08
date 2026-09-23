package com.back.domain.habit.dto;

import com.back.domain.habit.entity.HabitStatus;

public record HabitWeeklyCheckResponse(
        Long habitId,
        WeeklyCheckResult result,
        HabitStatus habitStatus
) {
}

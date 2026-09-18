package com.back.domain.habit.dto;

import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.entity.HabitStatus;

import java.time.LocalDateTime;

public record HabitResponse(
        Long id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        Long groupMemberId,
        String title,
        String description,
        int days,
        HabitStatus status
) {
    public HabitResponse(Habit habit) {
        this(
                habit.getId(),
                habit.getCreateDate(),
                habit.getModifyDate(),
                habit.getGroupMember().getId(),
                habit.getTitle(),
                habit.getDescription(),
                habit.getDays(),
                habit.getStatus()
        );
    }
}

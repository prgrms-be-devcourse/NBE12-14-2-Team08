package com.back.domain.habitVerify.dto;

import com.back.domain.habitVerify.entity.HabitVerifyStatus;

public record HabitVerifyRequest(
        HabitVerifyStatus status,
        String description,
        String imageUrl
) {
}
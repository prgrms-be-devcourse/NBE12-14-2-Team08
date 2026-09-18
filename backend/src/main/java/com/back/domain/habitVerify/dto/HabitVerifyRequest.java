package com.back.domain.habitVerify.dto;

import com.back.domain.habitVerify.entity.HabitVerifyStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record HabitVerifyRequest(
        @NotNull
        LocalDate verifyDate,
        @NotNull
        HabitVerifyStatus status,
        String description,
        String imageUrl
) {
}
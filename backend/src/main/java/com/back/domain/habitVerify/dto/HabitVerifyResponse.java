package com.back.domain.habitVerify.dto;

import com.back.domain.habitVerify.entity.HabitVerify;
import com.back.domain.habitVerify.entity.HabitVerifyStatus;

import java.time.LocalDate;

public record HabitVerifyResponse(
        Long id,
        Long habitId,

        LocalDate verifyDate,
        HabitVerifyStatus status,

        String description,
        String imageUrl
) {

    public static HabitVerifyResponse from(HabitVerify habitVerify) {
        return new HabitVerifyResponse(
                habitVerify.getId(),
                habitVerify.getHabit().getId(),
                habitVerify.getVerifyDate(),
                habitVerify.getStatus(),
                habitVerify.getDescription(),
                habitVerify.getImageUrl()

        );
    }
}

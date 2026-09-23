package com.back.domain.habitVerify.dto;

import com.back.domain.habitVerify.entity.HabitVerify;
import com.back.domain.habitVerify.entity.HabitVerifyStatus;

import java.time.LocalDate;

public record HabitVerifySummaryResponse(
        Long id,
        LocalDate verifyDate,
        String habitTitle,
        String memberNickname,
        String description,
        String imageUrl,
        HabitVerifyStatus status
) {
    public static HabitVerifySummaryResponse from(HabitVerify habitVerify) {
        return new HabitVerifySummaryResponse(
                habitVerify.getId(),
                habitVerify.getVerifyDate(),
                habitVerify.getHabit().getTitle(),
                habitVerify.getHabit().getGroupMember().getMember().getNickname(),
                habitVerify.getDescription(),
                habitVerify.getImageUrl(),
                habitVerify.getStatus()
        );
    }
}
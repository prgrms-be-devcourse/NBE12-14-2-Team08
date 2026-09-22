package com.back.domain.habitVerify.dto;

public record HabitVerifyRequest(
        String description,
        String imageUrl
) {
}
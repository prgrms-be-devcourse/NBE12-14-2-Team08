package com.back.domain.habitVerify.dto;

import jakarta.validation.constraints.NotBlank;

public record HabitVerifyRequest(
        String description,

        @NotBlank(message = "인증 사진은 필수입니다.")
        String imageUrl
) {
}

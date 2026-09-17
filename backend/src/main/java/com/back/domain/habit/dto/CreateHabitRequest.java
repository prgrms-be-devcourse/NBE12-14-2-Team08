package com.back.domain.habit.dto;

import com.back.domain.member.entity.Member;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;



public record CreateHabitRequest(

        @NotBlank(message = "습관 제목은 필수입니다.")
        String title,

        String description,

        @Min(value = 1, message = "실천 일수는 최소 1일입니다.")
        @Max(value = 7, message = "실천 일수는 최대 7일입니다.")
        int days
) {
}
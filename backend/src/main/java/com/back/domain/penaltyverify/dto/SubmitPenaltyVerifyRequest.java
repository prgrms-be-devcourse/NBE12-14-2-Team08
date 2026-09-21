package com.back.domain.penaltyverify.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitPenaltyVerifyRequest(
    String description,

    @NotBlank(message = "인증 이미지는 필수입니다.")
    String imageUrl

) {

}

package com.back.domain.penaltyverify.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitPenaltyVerifyRequest(
    String description,

    @NotBlank(message = "인증 사진은 필수입니다.")
    String imageUrl

) {

}

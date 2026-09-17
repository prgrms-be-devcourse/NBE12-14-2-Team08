package com.back.domain.penaltyverify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SubmitPenaltyVerifyRequest(
    @NotNull
    LocalDate verifyDate,
    String description,
    @NotBlank
    String imageUrl

) {

}

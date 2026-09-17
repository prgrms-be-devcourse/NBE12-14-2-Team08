package com.back.domain.penaltyverify.dto;

import com.back.domain.penaltyverify.entity.PenaltyVerify;
import com.back.domain.penaltyverify.entity.PenaltyVerifyStatus;
import java.time.LocalDate;

public record PenaltyVerifyDetailResponse(
    Long id,
    LocalDate verifyDate,
    String habitTitle,
    String penaltyText,
    String description,
    String imageUrl,
    PenaltyVerifyStatus status
) {
    public static PenaltyVerifyDetailResponse from(PenaltyVerify py){
        return new PenaltyVerifyDetailResponse(
            py.getId(),
            py.getVerifyDate(),
            py.getHabitTitle(),
            py.getPenaltyText(),
            py.getDescription(),
            py.getImageUrl(),
            py.getStatus()
        );
    }
}

package com.back.domain.penaltyverify.dto;

import com.back.domain.penaltyverify.entity.PenaltyVerify;
import com.back.domain.penaltyverify.entity.PenaltyVerifyStatus;
import java.time.LocalDate;

// 벌칙 요약 목록 (관리자 검토 및 사용자 벌칙 기록 공용)
public record PenaltyVerifySummaryResponse (
    Long id,
    LocalDate verifyDate,
    String habitTitle,
    String description,
    String memberNickname,
    String imageUrl,
    String penaltyText,
    PenaltyVerifyStatus status
){
    public static PenaltyVerifySummaryResponse from(PenaltyVerify py){
        return new PenaltyVerifySummaryResponse(
            py.getId(),
            py.getVerifyDate(),
            py.getHabitTitle(),
            py.getDescription(),
            py.getGroupMember().getMember().getNickname(),
            py.getImageUrl(),
            py.getPenaltyText(),
            py.getStatus()
        );
    }
}

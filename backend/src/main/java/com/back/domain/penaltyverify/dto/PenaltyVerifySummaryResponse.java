package com.back.domain.penaltyverify.dto;

import com.back.domain.penaltyverify.entity.PenaltyVerify;
import com.back.domain.penaltyverify.entity.PenaltyVerifyStatus;
import java.time.LocalDate;

// 관리자 페이지 목록
public record PenaltyVerifySummaryResponse (
    Long id,
    LocalDate verifyDate,
    String habitTitle,
    String memberNickname,
    String imageUrl,
    PenaltyVerifyStatus status
){
    public static PenaltyVerifySummaryResponse from(PenaltyVerify py){
        return new PenaltyVerifySummaryResponse(
            py.getId(),
            py.getVerifyDate(),
            py.getHabitTitle(),
            py.getGroupMember().getMember().getNickname(),
            py.getImageUrl(),
            py.getStatus()
        );
    }
}

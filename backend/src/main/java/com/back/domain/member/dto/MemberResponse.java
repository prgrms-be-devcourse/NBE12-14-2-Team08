package com.back.domain.member.dto;

import com.back.domain.member.entity.Member;
import java.time.LocalDateTime;

public record MemberResponse(
        Long memberId,
        String name,
        String username,
        LocalDateTime createDate,
        LocalDateTime modifyDate
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getNickname(),
                member.getUsername(),
                member.getCreateDate(),
                member.getModifyDate()
        );
    }
}
package com.back.domain.groupMember.dto;

public interface GroupMemberResponse {

    record Simple(
            Long Id,
            Long memberId,
            String nickname,
            String role,
            String habitTitle,
            String habitDescription
    ) {}

    record Detail(
            Long groupMemberId,
            Long memberId,
            String nickname,
            String role,
            int penaltyCount
    ) {}
}

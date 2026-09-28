package com.back.domain.groupMember.dto;

public interface GroupMemberResponse {

    record Simple(
            Long id,
            Long memberId,
            String nickname,
            String username,
            String role,
            Long habitId,
            String habitTitle,
            String habitDescription
    ) {}

    record Detail(
            Long groupMemberId,
            Long memberId,
            String nickname,
            String username,
            String role,
            int penaltyCount
    ) {}
}

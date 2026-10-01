package com.back.domain.groupMember.dto;

public interface GroupMemberResponse {

    record Simple(
            Long groupMemberId,
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
            Long penaltyCount
    ) {}

    record JoinSuccess(
            Long groupId
    ) {}
}

package com.back.domain.group.dto;

public record SettlementResponse(
        Long groupId,
        String groupTitle,
        PersonalStats personalStats,
        GroupComparison groupComparison
) {

    public record PersonalStats(
            Long memberId,
            String nickname,
            long approvedCount,
            long totalVerifyCount,
            long rejectedCount,
            long penaltyCount
    ) {}

    public record Winner(
            Long memberId,
            String nickname,
            long value
    ) {}

    public record GroupComparison(
            Winner mostDescriptionChars,
            Winner mostDeadlineVerifications,
            Winner mostRejectedVerifications,
            Winner longestPenaltyDelay
    ) {}
}
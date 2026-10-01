package com.back.domain.group.dto;

import com.back.domain.group.entity.Group;
import com.back.domain.group.entity.GroupStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface GroupResponse {
    record Detail(
            Long id,
            String title,
            String description,
            LocalDate startDate,
            LocalDate deadline,
            String penalty,
            String inviteCode,
            int memberLimit,
            LocalDateTime createDate,
            String inviteLink,
            GroupStatus status,
            boolean isJoined,
            long currentMemberCount
    ) {
        public static Detail from(Group group, String baseInviteUrl, boolean isJoined, long currentMemberCount) {
            String fullInviteLink = baseInviteUrl + group.getInviteCode();

            GroupStatus currentStatus = group.getStatus();
            if (group.getStatus() == GroupStatus.ACTIVE
                    && group.getDeadline() != null
                    && LocalDate.now().isAfter(group.getDeadline())) {
                currentStatus = GroupStatus.FINISH;
            }

            return new Detail(
                    group.getId(),
                    group.getTitle(),
                    group.getDescription(),
                    group.getCreateDate().toLocalDate(),
                    group.getDeadline(),
                    group.getPenalty(),
                    group.getInviteCode(),
                    group.getMemberLimit(),
                    group.getCreateDate(),
                    fullInviteLink,
                    currentStatus,
                    isJoined,
                    currentMemberCount
            );
        }
    }

    record Simple(
            Long id,
            String title,
            String description,
            int memberLimit,
            Long currentMemberCount,
            String status,
            String role
    ) {}
}
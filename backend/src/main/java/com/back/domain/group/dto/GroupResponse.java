package com.back.domain.group.dto;

import com.back.domain.group.entity.Group;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface GroupResponse {
    record Detail(
            Long id,
            String title,
            String description,
            LocalDate deadline,
            String penalty,
            String inviteCode,
            int memberLimit,
            LocalDateTime createDate,
            String inviteLink
    ) {
        public static Detail from(Group group, String baseInviteUrl) {
            String fullInviteLink = baseInviteUrl + group.getInviteCode();

            return new Detail(
                    group.getId(),
                    group.getTitle(),
                    group.getDescription(),
                    group.getDeadline(),
                    group.getPenalty(),
                    group.getInviteCode(),
                    group.getMemberLimit(),
                    group.getCreateDate(),
                    fullInviteLink
            );
        }
    }

    record Simple(
            Long id,
            String title,
            String description,
            int memberLimit,
            Long currentMemberCount
    ) {}
}
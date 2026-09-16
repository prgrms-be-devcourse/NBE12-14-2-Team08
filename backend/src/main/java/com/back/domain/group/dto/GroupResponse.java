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
            LocalDateTime createDate
    ) {
        public static Detail from(Group group) {
            return new Detail(
                    group.getId(),
                    group.getTitle(),
                    group.getDescription(),
                    group.getDeadline(),
                    group.getPenalty(),
                    group.getInviteCode(),
                    group.getMemberLimit(),
                    group.getCreateDate()
            );
        }
    }

    record Simple(
            Long id,
            String title,
            LocalDate deadline,
            int memberLimit
    ) {
        public static Simple from(Group group) {
            return new Simple(
                    group.getId(),
                    group.getTitle(),
                    group.getDeadline(),
                    group.getMemberLimit()
            );
        }
    }
}
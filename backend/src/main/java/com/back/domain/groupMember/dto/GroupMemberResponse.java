package com.back.domain.groupMember.dto;

import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;

public interface GroupMemberResponse {

    record Simple(
            Long Id,
            Long memeberId,
            String nickname,
            GroupMemberRole role
    ) {
        public static Simple from(GroupMember groupMember) {
            return new Simple(
                    groupMember.getId(),
                    groupMember.getMember().getId(),
                    groupMember.getMember().getNickname(),
                    groupMember.getRole()
            );
        }
    }
}

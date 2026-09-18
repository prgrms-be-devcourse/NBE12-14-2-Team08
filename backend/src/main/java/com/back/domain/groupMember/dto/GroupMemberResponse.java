package com.back.domain.groupMember.dto;

import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;

public interface GroupMemberResponse {

    record Simple(
            Long Id,
            Long memberId,
            String nickname,
            String role,
            String habitTitle,
            String habitDescription
    ) {}
}

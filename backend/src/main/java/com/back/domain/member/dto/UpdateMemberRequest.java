package com.back.domain.member.dto;

public record UpdateMemberRequest(
        String nickname,
        String currentPassword,
        String newPassword
) {
}

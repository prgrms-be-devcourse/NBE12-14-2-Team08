package com.back.domain.member.dto;

public record UpdateMemberRequest(
        String name,
        String password
) {
}
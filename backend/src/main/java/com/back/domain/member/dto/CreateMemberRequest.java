package com.back.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMemberRequest(
        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotBlank(message = "아이디는 필수입니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}

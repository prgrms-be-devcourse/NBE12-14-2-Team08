package com.back.domain.group.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public interface GroupRequest {

    record Create(
            @NotBlank(message = "그룹 이름은 필수입니다.")
            String title,

            String description,
            LocalDate deadline,
            String penalty,

            @NotBlank(message = "비밀번호는 필수입니다.")
            String password,

            @Min(value = 1, message = "최대 인원은 1명 이상이어야 합니다.")
            int memberLimit
    ) {}

    record Update(
            String title,

            String description,
            LocalDate deadline,
            String penalty,
            String password,

            @Min(value = 1, message = "최대 인원은 1명 이상이어야 합니다.")
            int memberLimit
    ) {}

    record Join(
            @NotBlank(message = "초대 코드는 필수입니다.")
            String inviteCode
    ) {}
}
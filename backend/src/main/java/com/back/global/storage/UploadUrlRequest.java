package com.back.global.storage;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "스토리지 업로드 서명 URL 발급 요청")
public record UploadUrlRequest(
    @NotBlank
    @Schema(description = "업로드할 파일명 (확장자 포함)", example = "today_habit.png")
    String filename
) {}

package com.back.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    int status,
    String message,
    Instant timestamp,
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<String> errors

) {

    // 일반 에러용
    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, Instant.now(), null);
    }

    // @Valid 검증 에러용
    public static ErrorResponse of(int status, String message, List<String> errors) {
        return new ErrorResponse(status, message, Instant.now(), errors);
    }

}

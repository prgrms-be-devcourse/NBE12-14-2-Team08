package com.back.domain.habitVerify.dto;

import java.util.List;

public record BulkActionRequest(
        List<Long> ids
) {
}
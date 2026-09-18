package com.back.domain.penaltyverify.dto;

import java.util.List;

public record BulkActionRequest(
    List<Long> ids
) {

}

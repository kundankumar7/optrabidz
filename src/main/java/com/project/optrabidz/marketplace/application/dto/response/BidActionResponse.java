package com.project.optrabidz.marketplace.application.dto.response;

import com.project.optrabidz.marketplace.domain.model.BidState;

import java.time.Instant;

public record BidActionResponse(
        Long bidId,
        BidState bidState,
        Instant actionAt
) {
}

package com.project.optrabidz.marketplace.application.dto.response;

import com.project.optrabidz.marketplace.domain.model.ListingState;

import java.time.Instant;

public record PublishListingResponse(
        Long listingId,
        ListingState listingState,
        Instant publishedAt,
        Instant expiresAt
) {
}

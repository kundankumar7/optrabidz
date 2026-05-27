package com.project.optrabidz.marketplace.application.dto.response;

import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingState;

import java.time.Instant;

public record ListingResponse(
        Long listingId,
        FundingModel fundingModel,
        ListingState listingState,
        String title,
        String fundingPurposeDescription,
        String publicDisplayName,
        String businessDescription,
        ListingDebtTermsResponse debtTerms,
        Instant createdAt,
        Instant publishedAt,
        Instant expiresAt,
        Instant closedAt
) {
}

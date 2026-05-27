package com.project.optrabidz.marketplace.application.dto.response;

public record RecommendedListingResponse(
        ListingResponse listing,
        RecommendationInfoResponse recommendation
) {
}

package com.project.optrabidz.marketplace.application.dto.response;

import com.project.optrabidz.marketplace.domain.model.BidState;
import com.project.optrabidz.marketplace.domain.model.FundingModel;

import java.time.Instant;

public record BidResponse(
        Long bidId,
        Long listingId,
        FundingModel fundingModel,
        BidState bidState,
        BidDebtTermsResponse debtTerms,
        String proposalMessage,
        Instant createdAt,
        Instant withdrawnAt,
        Instant rejectedAt,
        Instant acceptedAt,
        Instant fundedAt
) {
}

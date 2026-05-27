package com.project.optrabidz.marketplace.application.dto.request;

import com.project.optrabidz.marketplace.domain.model.FundingModel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SubmitBidRequest(
        @NotNull Long listingId,
        @NotNull FundingModel fundingModel,
        @Valid @NotNull BidDebtTermsRequest debtTerms,
        String proposalMessage
) {
}

package com.project.optrabidz.marketplace.application.dto.request;

import com.project.optrabidz.marketplace.domain.model.FundingModel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateListingRequest(
        @NotNull FundingModel fundingModel,
        @NotBlank String title,
        @NotBlank String fundingPurposeDescription,
        @Valid @NotNull ListingDebtTermsRequest debtTerms
) {
}

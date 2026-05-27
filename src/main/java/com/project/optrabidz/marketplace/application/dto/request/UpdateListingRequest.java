package com.project.optrabidz.marketplace.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateListingRequest(
        @NotBlank String title,
        @NotBlank String fundingPurposeDescription,
        @Valid @NotNull ListingDebtTermsRequest debtTerms
) {
}

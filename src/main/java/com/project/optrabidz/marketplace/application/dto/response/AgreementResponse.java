package com.project.optrabidz.marketplace.application.dto.response;

import com.project.optrabidz.marketplace.domain.model.FundingModel;

import java.time.Instant;

public record AgreementResponse(
        Long agreementId,
        Long listingId,
        Long bidId,
        FundingModel fundingModel,
        String startupDisplayName,
        String investorDisplayName,
        AgreementDebtTermsResponse debtTerms,
        Instant createdAt
) {
}

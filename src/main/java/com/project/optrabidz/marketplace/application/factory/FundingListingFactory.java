package com.project.optrabidz.marketplace.application.factory;

import com.project.optrabidz.marketplace.application.dto.request.CreateListingRequest;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class FundingListingFactory {
    private final DebtTermsFactory debtTermsFactory;

    public FundingListingFactory(DebtTermsFactory debtTermsFactory) {
        this.debtTermsFactory = debtTermsFactory;
    }

    public FundingListing createDraft(Long startupId, CreateListingRequest request, Instant now) {
        return FundingListing.createDraft(
                startupId,
                request.fundingModel(),
                request.title(),
                request.fundingPurposeDescription(),
                debtTermsFactory.createListingTerms(request.debtTerms(), now),
                now
        );
    }
}

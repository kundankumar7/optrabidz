package com.project.optrabidz.marketplace.application.factory;

import com.project.optrabidz.marketplace.application.dto.request.SubmitBidRequest;
import com.project.optrabidz.marketplace.domain.model.Bid;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class BidFactory {
    private final DebtTermsFactory debtTermsFactory;

    public BidFactory(DebtTermsFactory debtTermsFactory) {
        this.debtTermsFactory = debtTermsFactory;
    }

    public Bid submit(Long investorId, SubmitBidRequest request, Instant now) {
        return Bid.submit(
                request.listingId(),
                investorId,
                request.fundingModel(),
                debtTermsFactory.createBidTerms(request.debtTerms(), now),
                request.proposalMessage(),
                now
        );
    }
}

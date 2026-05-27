package com.project.optrabidz.marketplace.application.factory;

import com.project.optrabidz.marketplace.application.dto.request.BidDebtTermsRequest;
import com.project.optrabidz.marketplace.application.dto.request.ListingDebtTermsRequest;
import com.project.optrabidz.marketplace.domain.model.BidDebtTerms;
import com.project.optrabidz.marketplace.domain.model.ListingDebtTerms;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DebtTermsFactory {
    public ListingDebtTerms createListingTerms(ListingDebtTermsRequest request, Instant now) {
        return ListingDebtTerms.create(
                request.requestedAmount(),
                request.currencyCode(),
                request.minimumInterestRate(),
                request.maximumInterestRate(),
                request.requestedTenureMonths(),
                request.repaymentPlanType(),
                request.oneTimeRepaymentDueAfterMonths(),
                now
        );
    }

    public BidDebtTerms createBidTerms(BidDebtTermsRequest request, Instant now) {
        return BidDebtTerms.create(
                request.proposedAmount(),
                request.proposedInterestRate(),
                request.proposedTenureMonths(),
                request.repaymentPlanType(),
                request.oneTimeRepaymentDueAfterMonths(),
                now
        );
    }
}

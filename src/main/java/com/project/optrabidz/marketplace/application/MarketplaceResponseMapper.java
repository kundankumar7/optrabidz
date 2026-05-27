package com.project.optrabidz.marketplace.application;

import com.project.optrabidz.marketplace.application.dto.response.AgreementDebtTermsResponse;
import com.project.optrabidz.marketplace.application.dto.response.AgreementResponse;
import com.project.optrabidz.marketplace.application.dto.response.BidDebtTermsResponse;
import com.project.optrabidz.marketplace.application.dto.response.BidResponse;
import com.project.optrabidz.marketplace.application.dto.response.ListingDebtTermsResponse;
import com.project.optrabidz.marketplace.application.dto.response.ListingResponse;
import com.project.optrabidz.marketplace.domain.model.Agreement;
import com.project.optrabidz.marketplace.domain.model.AgreementDebtTerms;
import com.project.optrabidz.marketplace.domain.model.Bid;
import com.project.optrabidz.marketplace.domain.model.BidDebtTerms;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.ListingDebtTerms;
import org.springframework.stereotype.Component;

@Component
public class MarketplaceResponseMapper {
    public ListingResponse toListingResponse(FundingListing listing,
                                             String startupDisplayName,
                                             String startupBusinessDescription) {
        return new ListingResponse(
                listing.getListingId(),
                listing.getFundingModel(),
                listing.getListingState(),
                listing.getTitle(),
                listing.getFundingPurposeDescription(),
                startupDisplayName,
                startupBusinessDescription,
                toDebtTermsResponse(listing.getDebtTerms()),
                listing.getCreatedAt(),
                listing.getPublishedAt(),
                listing.getExpiresAt(),
                listing.getClosedAt()
        );
    }

    public BidResponse toBidResponse(Bid bid) {
        return new BidResponse(
                bid.getBidId(),
                bid.getListingId(),
                bid.getFundingModel(),
                bid.getBidState(),
                toDebtTermsResponse(bid.getDebtTerms()),
                bid.getProposalMessage(),
                bid.getCreatedAt(),
                bid.getWithdrawnAt(),
                bid.getRejectedAt(),
                bid.getAcceptedAt(),
                bid.getFundedAt()
        );
    }

    public AgreementResponse toAgreementResponse(Agreement agreement,
                                                 String startupDisplayName,
                                                 String investorDisplayName) {
        return new AgreementResponse(
                agreement.getAgreementId(),
                agreement.getListingId(),
                agreement.getBidId(),
                agreement.getFundingModel(),
                startupDisplayName,
                investorDisplayName,
                toDebtTermsResponse(agreement.getDebtTerms()),
                agreement.getCreatedAt()
        );
    }

    private ListingDebtTermsResponse toDebtTermsResponse(ListingDebtTerms terms) {
        return new ListingDebtTermsResponse(
                terms.getRequestedAmount(),
                terms.getCurrencyCode(),
                terms.getMinimumInterestRate(),
                terms.getMaximumInterestRate(),
                terms.getRequestedTenureMonths(),
                terms.getRepaymentPlanType(),
                terms.getOneTimeRepaymentDueAfterMonths()
        );
    }

    private BidDebtTermsResponse toDebtTermsResponse(BidDebtTerms terms) {
        return new BidDebtTermsResponse(
                terms.getProposedAmount(),
                terms.getProposedInterestRate(),
                terms.getProposedTenureMonths(),
                terms.getRepaymentPlanType(),
                terms.getOneTimeRepaymentDueAfterMonths()
        );
    }

    private AgreementDebtTermsResponse toDebtTermsResponse(AgreementDebtTerms terms) {
        return new AgreementDebtTermsResponse(
                terms.getPrincipalAmount(),
                terms.getInterestRate(),
                terms.getTenureMonths(),
                terms.getRepaymentPlanType(),
                terms.getOneTimeRepaymentDueAfterMonths()
        );
    }
}

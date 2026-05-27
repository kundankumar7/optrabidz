package com.project.optrabidz.marketplace.infrastructure.mapper;

import com.project.optrabidz.marketplace.domain.model.Agreement;
import com.project.optrabidz.marketplace.domain.model.AgreementDebtTerms;
import com.project.optrabidz.marketplace.domain.model.Bid;
import com.project.optrabidz.marketplace.domain.model.BidDebtTerms;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingDebtTerms;
import org.springframework.stereotype.Component;

@Component
public class MarketplacePersistenceMapper {
    public com.project.optrabidz.marketplace.infrastructure.entity.FundingListing toEntity(FundingListing listing) {
        com.project.optrabidz.marketplace.infrastructure.entity.FundingListing entity =
                new com.project.optrabidz.marketplace.infrastructure.entity.FundingListing();
        entity.setListingId(listing.getListingId());
        entity.setStartupId(listing.getStartupId());
        entity.setFundingModel(listing.getFundingModel());
        entity.setListingState(listing.getListingState());
        entity.setTitle(listing.getTitle());
        entity.setFundingPurposeDescription(listing.getFundingPurposeDescription());
        entity.setCreatedAt(listing.getCreatedAt());
        entity.setPublishedAt(listing.getPublishedAt());
        entity.setExpiresAt(listing.getExpiresAt());
        entity.setClosedAt(listing.getClosedAt());
        entity.setDebtTerms(toEntity(listing.getDebtTerms()));
        return entity;
    }

    public FundingListing toDomain(com.project.optrabidz.marketplace.infrastructure.entity.FundingListing entity) {
        return FundingListing.builder()
                .listingId(entity.getListingId())
                .startupId(entity.getStartupId())
                .fundingModel(entity.getFundingModel())
                .listingState(entity.getListingState())
                .title(entity.getTitle())
                .fundingPurposeDescription(entity.getFundingPurposeDescription())
                .createdAt(entity.getCreatedAt())
                .publishedAt(entity.getPublishedAt())
                .expiresAt(entity.getExpiresAt())
                .closedAt(entity.getClosedAt())
                .debtTerms(toDomain(entity.getDebtTerms(), entity))
                .build();
    }

    public com.project.optrabidz.marketplace.infrastructure.entity.Bid toEntity(Bid bid) {
        com.project.optrabidz.marketplace.infrastructure.entity.Bid entity =
                new com.project.optrabidz.marketplace.infrastructure.entity.Bid();
        entity.setBidId(bid.getBidId());
        entity.setListingId(bid.getListingId());
        entity.setInvestorId(bid.getInvestorId());
        entity.setBidState(bid.getBidState());
        entity.setProposalMessage(bid.getProposalMessage());
        entity.setCreatedAt(bid.getCreatedAt());
        entity.setWithdrawnAt(bid.getWithdrawnAt());
        entity.setRejectedAt(bid.getRejectedAt());
        entity.setAcceptedAt(bid.getAcceptedAt());
        entity.setFundedAt(bid.getFundedAt());
        entity.setDebtTerms(toEntity(bid.getDebtTerms()));
        return entity;
    }

    public Bid toDomain(com.project.optrabidz.marketplace.infrastructure.entity.Bid entity,
                        FundingModel fundingModel) {
        return Bid.builder()
                .bidId(entity.getBidId())
                .listingId(entity.getListingId())
                .investorId(entity.getInvestorId())
                .fundingModel(fundingModel)
                .bidState(entity.getBidState())
                .proposalMessage(entity.getProposalMessage())
                .createdAt(entity.getCreatedAt())
                .withdrawnAt(entity.getWithdrawnAt())
                .rejectedAt(entity.getRejectedAt())
                .acceptedAt(entity.getAcceptedAt())
                .fundedAt(entity.getFundedAt())
                .debtTerms(toDomain(entity.getDebtTerms(), entity))
                .build();
    }

    public com.project.optrabidz.marketplace.infrastructure.entity.Agreement toEntity(Agreement agreement) {
        com.project.optrabidz.marketplace.infrastructure.entity.Agreement entity =
                new com.project.optrabidz.marketplace.infrastructure.entity.Agreement();
        entity.setAgreementId(agreement.getAgreementId());
        entity.setListingId(agreement.getListingId());
        entity.setBidId(agreement.getBidId());
        entity.setStartupId(agreement.getStartupId());
        entity.setInvestorId(agreement.getInvestorId());
        entity.setCreatedAt(agreement.getCreatedAt());
        entity.setDebtTerms(toEntity(agreement.getDebtTerms()));
        return entity;
    }

    public Agreement toDomain(com.project.optrabidz.marketplace.infrastructure.entity.Agreement entity) {
        return Agreement.builder()
                .agreementId(entity.getAgreementId())
                .listingId(entity.getListingId())
                .bidId(entity.getBidId())
                .startupId(entity.getStartupId())
                .investorId(entity.getInvestorId())
                .fundingModel(FundingModel.DEBT)
                .createdAt(entity.getCreatedAt())
                .debtTerms(toDomain(entity.getDebtTerms()))
                .build();
    }

    private com.project.optrabidz.marketplace.infrastructure.entity.ListingDebtTerms toEntity(ListingDebtTerms terms) {
        com.project.optrabidz.marketplace.infrastructure.entity.ListingDebtTerms entity =
                new com.project.optrabidz.marketplace.infrastructure.entity.ListingDebtTerms();
        entity.setListingDebtTermsId(terms.getListingDebtTermsId());
        entity.setRequestedAmount(terms.getRequestedAmount());
        entity.setCurrencyCode(terms.getCurrencyCode());
        entity.setMinimumInterestRate(terms.getMinimumInterestRate());
        entity.setMaximumInterestRate(terms.getMaximumInterestRate());
        entity.setRequestedTenureMonths(terms.getRequestedTenureMonths());
        entity.setRepaymentPlanType(terms.getRepaymentPlanType());
        entity.setOneTimeRepaymentDueAfterMonths(terms.getOneTimeRepaymentDueAfterMonths());
        entity.setCreatedAt(terms.getCreatedAt());
        entity.setUpdatedAt(terms.getUpdatedAt());
        return entity;
    }

    private ListingDebtTerms toDomain(com.project.optrabidz.marketplace.infrastructure.entity.ListingDebtTerms entity,
                                      com.project.optrabidz.marketplace.infrastructure.entity.FundingListing listing) {
        if (entity == null) {
            throw new IllegalStateException("Debt terms are required for listing " + listing.getListingId());
        }
        return new ListingDebtTerms(
                entity.getListingDebtTermsId(),
                entity.getListing() == null ? listing.getListingId() : entity.getListing().getListingId(),
                entity.getRequestedAmount(),
                entity.getCurrencyCode(),
                entity.getMinimumInterestRate(),
                entity.getMaximumInterestRate(),
                entity.getRequestedTenureMonths(),
                entity.getRepaymentPlanType(),
                entity.getOneTimeRepaymentDueAfterMonths(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private com.project.optrabidz.marketplace.infrastructure.entity.BidDebtTerms toEntity(BidDebtTerms terms) {
        com.project.optrabidz.marketplace.infrastructure.entity.BidDebtTerms entity =
                new com.project.optrabidz.marketplace.infrastructure.entity.BidDebtTerms();
        entity.setBidDebtTermsId(terms.getBidDebtTermsId());
        entity.setProposedAmount(terms.getProposedAmount());
        entity.setProposedInterestRate(terms.getProposedInterestRate());
        entity.setProposedTenureMonths(terms.getProposedTenureMonths());
        entity.setRepaymentPlanType(terms.getRepaymentPlanType());
        entity.setOneTimeRepaymentDueAfterMonths(terms.getOneTimeRepaymentDueAfterMonths());
        entity.setCreatedAt(terms.getCreatedAt());
        entity.setUpdatedAt(terms.getUpdatedAt());
        return entity;
    }

    private BidDebtTerms toDomain(com.project.optrabidz.marketplace.infrastructure.entity.BidDebtTerms entity,
                                  com.project.optrabidz.marketplace.infrastructure.entity.Bid bid) {
        if (entity == null) {
            throw new IllegalStateException("Debt terms are required for bid " + bid.getBidId());
        }
        return new BidDebtTerms(
                entity.getBidDebtTermsId(),
                entity.getBid() == null ? bid.getBidId() : entity.getBid().getBidId(),
                entity.getProposedAmount(),
                entity.getProposedInterestRate(),
                entity.getProposedTenureMonths(),
                entity.getRepaymentPlanType(),
                entity.getOneTimeRepaymentDueAfterMonths(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private com.project.optrabidz.marketplace.infrastructure.entity.AgreementDebtTerms toEntity(AgreementDebtTerms terms) {
        com.project.optrabidz.marketplace.infrastructure.entity.AgreementDebtTerms entity =
                new com.project.optrabidz.marketplace.infrastructure.entity.AgreementDebtTerms();
        entity.setAgreementDebtTermsId(terms.getAgreementDebtTermsId());
        entity.setPrincipalAmount(terms.getPrincipalAmount());
        entity.setInterestRate(terms.getInterestRate());
        entity.setTenureMonths(terms.getTenureMonths());
        entity.setRepaymentPlanType(terms.getRepaymentPlanType());
        entity.setOneTimeRepaymentDueAfterMonths(terms.getOneTimeRepaymentDueAfterMonths());
        entity.setCreatedAt(terms.getCreatedAt());
        return entity;
    }

    private AgreementDebtTerms toDomain(com.project.optrabidz.marketplace.infrastructure.entity.AgreementDebtTerms entity) {
        return new AgreementDebtTerms(
                entity.getAgreementDebtTermsId(),
                entity.getAgreement() == null ? null : entity.getAgreement().getAgreementId(),
                entity.getPrincipalAmount(),
                entity.getInterestRate(),
                entity.getTenureMonths(),
                entity.getRepaymentPlanType(),
                entity.getOneTimeRepaymentDueAfterMonths(),
                entity.getCreatedAt()
        );
    }
}

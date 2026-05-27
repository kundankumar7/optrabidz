package com.project.optrabidz.marketplace.infrastructure.entity;

import com.project.optrabidz.marketplace.domain.model.BidState;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "bid")
public class Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_id", nullable = false, updatable = false)
    private Long bidId;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "bid_state", nullable = false, columnDefinition = "bid_state_enum")
    private BidState bidState;

    @Column(name = "proposal_message")
    private String proposalMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "funded_at")
    private Instant fundedAt;

    @OneToOne(mappedBy = "bid", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private BidDebtTerms debtTerms;

    public Long getBidId() {
        return bidId;
    }

    public void setBidId(Long bidId) {
        this.bidId = bidId;
    }

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public Long getInvestorId() {
        return investorId;
    }

    public void setInvestorId(Long investorId) {
        this.investorId = investorId;
    }

    public BidState getBidState() {
        return bidState;
    }

    public void setBidState(BidState bidState) {
        this.bidState = bidState;
    }

    public String getProposalMessage() {
        return proposalMessage;
    }

    public void setProposalMessage(String proposalMessage) {
        this.proposalMessage = proposalMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(Instant withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Instant rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Instant getFundedAt() {
        return fundedAt;
    }

    public void setFundedAt(Instant fundedAt) {
        this.fundedAt = fundedAt;
    }

    public BidDebtTerms getDebtTerms() {
        return debtTerms;
    }

    public void setDebtTerms(BidDebtTerms debtTerms) {
        this.debtTerms = debtTerms;
        if (debtTerms != null) {
            debtTerms.setBid(this);
        }
    }
}

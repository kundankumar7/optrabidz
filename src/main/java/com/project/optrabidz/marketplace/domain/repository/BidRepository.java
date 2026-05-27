package com.project.optrabidz.marketplace.domain.repository;

import com.project.optrabidz.marketplace.domain.model.Bid;
import com.project.optrabidz.marketplace.domain.model.BidState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface BidRepository {
    Bid save(Bid bid);

    Bid saveAndFlush(Bid bid);

    Optional<Bid> findById(Long bidId);

    Optional<Bid> findAcceptedByListingId(Long listingId);

    Optional<Bid> findLatestByInvestorIdAndListingId(Long investorId, Long listingId);

    boolean existsActiveByInvestorIdAndListingId(Long investorId, Long listingId);

    boolean existsAcceptedByListingId(Long listingId);

    int rejectOtherSubmittedBids(Long listingId, Long acceptedBidId, Instant rejectedAt);

    Page<Bid> findByListingId(Long listingId, BidState state, Pageable pageable);

    Page<Bid> findByInvestorId(Long investorId, BidState state, Pageable pageable);
}

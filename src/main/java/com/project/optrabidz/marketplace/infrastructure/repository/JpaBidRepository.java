package com.project.optrabidz.marketplace.infrastructure.repository;

import com.project.optrabidz.marketplace.domain.model.BidState;
import com.project.optrabidz.marketplace.infrastructure.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface JpaBidRepository extends JpaRepository<Bid, Long> {
    @Override
    @EntityGraph(attributePaths = "debtTerms")
    Optional<Bid> findById(Long bidId);

    @Query(
            value = """
                    select *
                    from bid b
                    where b.listing_id = :listingId
                      and (cast(:state as text) is null
                           or b.bid_state = cast(cast(:state as text) as bid_state_enum))
                    order by b.created_at desc
                    """,
            countQuery = """
                    select count(*)
                    from bid b
                    where b.listing_id = :listingId
                      and (cast(:state as text) is null
                           or b.bid_state = cast(cast(:state as text) as bid_state_enum))
                    """,
            nativeQuery = true
    )
    Page<Bid> findByListingIdAndOptionalState(@Param("listingId") Long listingId,
                                              @Param("state") String state,
                                              Pageable pageable);

    @Query(
            value = """
                    select *
                    from bid b
                    where b.investor_id = :investorId
                      and (cast(:state as text) is null
                           or b.bid_state = cast(cast(:state as text) as bid_state_enum))
                    order by b.created_at desc
                    """,
            countQuery = """
                    select count(*)
                    from bid b
                    where b.investor_id = :investorId
                      and (cast(:state as text) is null
                           or b.bid_state = cast(cast(:state as text) as bid_state_enum))
                    """,
            nativeQuery = true
    )
    Page<Bid> findByInvestorIdAndOptionalState(@Param("investorId") Long investorId,
                                               @Param("state") String state,
                                               Pageable pageable);

    @Query(value = """
            select *
            from bid b
            where b.listing_id = :listingId
              and b.bid_state = 'ACCEPTED'::bid_state_enum
            order by b.accepted_at desc
            limit 1
            """, nativeQuery = true)
    Optional<Bid> findAcceptedByListingId(@Param("listingId") Long listingId);

    @EntityGraph(attributePaths = "debtTerms")
    Optional<Bid> findFirstByInvestorIdAndListingIdOrderByCreatedAtDesc(Long investorId, Long listingId);

    @Query(value = """
            select exists (
                select 1
                from bid b
                where b.investor_id = :investorId
                  and b.listing_id = :listingId
                  and b.bid_state in (
                    'SUBMITTED'::bid_state_enum,
                    'ACCEPTED'::bid_state_enum,
                    'PENDING_SETTLEMENT'::bid_state_enum,
                    'FUNDED'::bid_state_enum
                  )
            )
            """, nativeQuery = true)
    boolean existsActiveByInvestorIdAndListingId(@Param("investorId") Long investorId,
                                                 @Param("listingId") Long listingId);

    @Query(value = """
            select exists (
                select 1
                from bid b
                where b.listing_id = :listingId
                  and b.bid_state = 'ACCEPTED'::bid_state_enum
            )
            """, nativeQuery = true)
    boolean existsAcceptedByListingId(@Param("listingId") Long listingId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update bid
            set bid_state = 'REJECTED',
                rejected_at = :rejectedAt
            where listing_id = :listingId
              and bid_id <> :acceptedBidId
              and bid_state = 'SUBMITTED'::bid_state_enum
            """, nativeQuery = true)
    int rejectOtherSubmittedBids(@Param("listingId") Long listingId,
                                 @Param("acceptedBidId") Long acceptedBidId,
                                 @Param("rejectedAt") Instant rejectedAt);
}

package com.project.optrabidz.marketplace.infrastructure.repository;

import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingState;
import com.project.optrabidz.marketplace.infrastructure.entity.FundingListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaFundingListingRepository extends JpaRepository<FundingListing, Long> {
    @Override
    @EntityGraph(attributePaths = "debtTerms")
    Optional<FundingListing> findById(Long listingId);

    @Query(
            value = """
                    select *
                    from funding_listing listing
                    where listing.startup_id = :startupId
                      and (cast(:state as text) is null
                           or listing.listing_state = cast(cast(:state as text) as listing_state_enum))
                      and (cast(:fundingModel as text) is null
                           or listing.funding_model = cast(cast(:fundingModel as text) as funding_model_enum))
                    order by listing.created_at desc
                    """,
            countQuery = """
                    select count(*)
                    from funding_listing listing
                    where listing.startup_id = :startupId
                      and (cast(:state as text) is null
                           or listing.listing_state = cast(cast(:state as text) as listing_state_enum))
                      and (cast(:fundingModel as text) is null
                           or listing.funding_model = cast(cast(:fundingModel as text) as funding_model_enum))
                    """,
            nativeQuery = true
    )
    Page<FundingListing> findOwnedListings(@Param("startupId") Long startupId,
                                           @Param("state") String state,
                                           @Param("fundingModel") String fundingModel,
                                           Pageable pageable);

    @Query(
            value = """
                    select listing.*
                    from funding_listing listing
                    join listing_debt_terms terms
                      on terms.listing_id = listing.listing_id
                    where listing.listing_state = 'OPEN'::listing_state_enum
                      and (cast(:fundingModel as text) is null
                           or listing.funding_model = cast(cast(:fundingModel as text) as funding_model_enum))
                      and (:minAmount is null or terms.requested_amount >= :minAmount)
                      and (:maxAmount is null or terms.requested_amount <= :maxAmount)
                      and (:currencyCode is null or terms.currency_code = :currencyCode)
                    order by
                      case when :sortMode = 'CLOSING_SOON' then listing.expires_at end asc nulls last,
                      case when :sortMode = 'NEWEST' then listing.published_at end desc nulls last,
                      listing.listing_id desc
                    """,
            countQuery = """
                    select count(*)
                    from funding_listing listing
                    join listing_debt_terms terms
                      on terms.listing_id = listing.listing_id
                    where listing.listing_state = 'OPEN'::listing_state_enum
                      and (cast(:fundingModel as text) is null
                           or listing.funding_model = cast(cast(:fundingModel as text) as funding_model_enum))
                      and (:minAmount is null or terms.requested_amount >= :minAmount)
                      and (:maxAmount is null or terms.requested_amount <= :maxAmount)
                      and (:currencyCode is null or terms.currency_code = :currencyCode)
                    """,
            nativeQuery = true
    )
    Page<FundingListing> findOpenListings(@Param("fundingModel") String fundingModel,
                                          @Param("minAmount") BigDecimal minAmount,
                                          @Param("maxAmount") BigDecimal maxAmount,
                                          @Param("currencyCode") String currencyCode,
                                          @Param("sortMode") String sortMode,
                                          Pageable pageable);

    @Query("""
            select listing.listingId as listingId,
                   listing.fundingModel as fundingModel
            from FundingListing listing
            where listing.listingId in :listingIds
            """)
    List<ListingFundingModelView> findFundingModelsByListingIdIn(@Param("listingIds") Collection<Long> listingIds);

    @Query(value = """
            select listing_id
            from funding_listing
            where listing_state = 'OPEN'
              and expires_at is not null
              and expires_at <= :now
            order by expires_at asc
            for update skip locked
            limit :batchSize
            """, nativeQuery = true)
    List<Long> findExpiredOpenListingIds(@Param("now") Instant now, @Param("batchSize") int batchSize);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update funding_listing
            set listing_state = 'CLOSED',
                closed_at = :now
            where listing_id in (:listingIds)
              and listing_state = 'OPEN'
            """, nativeQuery = true)
    int closeOpenListings(@Param("listingIds") List<Long> listingIds, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update funding_listing
            set listing_state = 'AGREEMENT_REACHED',
                closed_at = :closedAt
            where listing_id = :listingId
              and listing_state = 'OPEN'
            """, nativeQuery = true)
    int markAgreementReachedIfOpen(@Param("listingId") Long listingId, @Param("closedAt") Instant closedAt);

    interface ListingFundingModelView {
        Long getListingId();

        FundingModel getFundingModel();
    }
}

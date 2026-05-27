package com.project.optrabidz.marketplace.infrastructure.repository;

import com.project.optrabidz.marketplace.domain.model.Bid;
import com.project.optrabidz.marketplace.domain.model.BidDebtTerms;
import com.project.optrabidz.marketplace.domain.model.BidState;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingDebtTerms;
import com.project.optrabidz.marketplace.domain.model.ListingSortMode;
import com.project.optrabidz.marketplace.domain.model.ListingState;
import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;
import com.project.optrabidz.marketplace.domain.repository.BidRepository;
import com.project.optrabidz.marketplace.domain.repository.FundingListingRepository;
import com.project.optrabidz.marketplace.infrastructure.mapper.MarketplacePersistenceMapper;
import com.project.optrabidz.testsupport.PostgresJpaIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@Import({
        MarketplacePersistenceMapper.class,
        FundingListingRepositoryAdapter.class,
        BidRepositoryAdapter.class
})
class MarketplaceRepositoryIT extends PostgresJpaIntegrationTestSupport {
    private static final Instant NOW = Instant.parse("2026-05-19T10:00:00Z");

    @Autowired
    private FundingListingRepository listingRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void findOpenListingsUsesPostgresEnumAndAmountFilters() {
        FundingListing matching = listingRepository.save(openListing(
                11L,
                "Working capital listing",
                "INR",
                new BigDecimal("550000.00"),
                NOW.plusSeconds(3_600)
        ));
        listingRepository.save(openListing(
                12L,
                "USD listing",
                "USD",
                new BigDecimal("550000.00"),
                NOW.plusSeconds(3_600)
        ));
        listingRepository.save(closedListing(
                13L,
                "Closed INR listing",
                "INR",
                new BigDecimal("550000.00")
        ));

        Page<FundingListing> listings = listingRepository.findOpenListings(
                FundingModel.DEBT,
                new BigDecimal("500000.00"),
                new BigDecimal("600000.00"),
                "inr",
                ListingSortMode.NEWEST,
                PageRequest.of(0, 10)
        );

        assertThat(listings.getTotalElements()).isEqualTo(1);
        assertThat(listings.getContent().get(0).getListingId()).isEqualTo(matching.getListingId());
        assertThat(listings.getContent().get(0).getListingState()).isEqualTo(ListingState.OPEN);
        assertThat(listings.getContent().get(0).getDebtTerms().getCurrencyCode()).isEqualTo("INR");
    }

    @Test
    void expireOpenListingsClosesOnlyExpiredOpenListings() {
        FundingListing expiredOpen = listingRepository.save(openListing(
                21L,
                "Expired open listing",
                "INR",
                new BigDecimal("300000.00"),
                NOW.minusSeconds(60)
        ));
        FundingListing futureOpen = listingRepository.save(openListing(
                22L,
                "Future open listing",
                "INR",
                new BigDecimal("350000.00"),
                NOW.plusSeconds(3_600)
        ));
        FundingListing closed = listingRepository.save(closedListing(
                23L,
                "Already closed listing",
                "INR",
                new BigDecimal("400000.00")
        ));

        int expiredCount = listingRepository.expireOpenListings(NOW, 10);

        assertThat(expiredCount).isEqualTo(1);
        assertThat(listingRepository.findById(expiredOpen.getListingId()).orElseThrow().getListingState())
                .isEqualTo(ListingState.CLOSED);
        assertThat(listingRepository.findById(futureOpen.getListingId()).orElseThrow().getListingState())
                .isEqualTo(ListingState.OPEN);
        assertThat(listingRepository.findById(closed.getListingId()).orElseThrow().getListingState())
                .isEqualTo(ListingState.CLOSED);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void listingExpiryCanRunInParallelWithoutDoubleProcessingRows() throws Exception {
        List<Long> expiredListingIds = inTransaction(() -> List.of(
                listingRepository.save(openListing(41L, "Expired listing 1", "INR", new BigDecimal("300000.00"), NOW.minusSeconds(600))).getListingId(),
                listingRepository.save(openListing(42L, "Expired listing 2", "INR", new BigDecimal("310000.00"), NOW.minusSeconds(500))).getListingId(),
                listingRepository.save(openListing(43L, "Expired listing 3", "INR", new BigDecimal("320000.00"), NOW.minusSeconds(400))).getListingId(),
                listingRepository.save(openListing(44L, "Expired listing 4", "INR", new BigDecimal("330000.00"), NOW.minusSeconds(300))).getListingId(),
                listingRepository.save(openListing(45L, "Expired listing 5", "INR", new BigDecimal("340000.00"), NOW.minusSeconds(200))).getListingId()
        ));

        int expiredCount = runTwoWorkers(() -> listingRepository.expireOpenListings(NOW, 3));

        assertThat(expiredCount).isEqualTo(5);
        assertThat(inTransaction(() -> expiredListingIds.stream()
                .map(listingRepository::findById)
                .map(value -> value.orElseThrow())
                .map(FundingListing::getListingState)
                .toList()))
                .containsOnly(ListingState.CLOSED);
    }

    @Test
    void bidNativeQueriesRespectActiveAndAcceptedStates() {
        FundingListing listing = listingRepository.save(openListing(
                31L,
                "Bid query listing",
                "INR",
                new BigDecimal("500000.00"),
                NOW.plusSeconds(3_600)
        ));
        Bid submitted = bidRepository.save(bid(listing.getListingId(), 201L, BidState.SUBMITTED));
        Bid accepted = bidRepository.save(bid(listing.getListingId(), 202L, BidState.ACCEPTED));
        bidRepository.save(bid(listing.getListingId(), 203L, BidState.WITHDRAWN));

        assertThat(bidRepository.existsActiveByInvestorIdAndListingId(201L, listing.getListingId())).isTrue();
        assertThat(bidRepository.existsActiveByInvestorIdAndListingId(203L, listing.getListingId())).isFalse();
        assertThat(bidRepository.existsAcceptedByListingId(listing.getListingId())).isTrue();

        Optional<Bid> acceptedBid = bidRepository.findAcceptedByListingId(listing.getListingId());
        assertThat(acceptedBid).isPresent();
        assertThat(acceptedBid.get().getBidId()).isEqualTo(accepted.getBidId());

        Page<Bid> submittedBids = bidRepository.findByListingId(
                listing.getListingId(),
                BidState.SUBMITTED,
                PageRequest.of(0, 10)
        );
        assertThat(submittedBids.getContent())
                .extracting(Bid::getBidId)
                .containsExactly(submitted.getBidId());
    }

    private static FundingListing openListing(Long startupId,
                                              String title,
                                              String currencyCode,
                                              BigDecimal requestedAmount,
                                              Instant expiresAt) {
        return FundingListing.builder()
                .startupId(startupId)
                .fundingModel(FundingModel.DEBT)
                .listingState(ListingState.OPEN)
                .title(title)
                .fundingPurposeDescription("Funds needed for business expansion.")
                .createdAt(NOW.minusSeconds(300))
                .publishedAt(NOW.minusSeconds(200))
                .expiresAt(expiresAt)
                .debtTerms(ListingDebtTerms.create(
                        requestedAmount,
                        currencyCode,
                        new BigDecimal("9.50"),
                        new BigDecimal("12.75"),
                        18,
                        RepaymentPlanType.INSTALLMENT_MONTHLY,
                        null,
                        NOW.minusSeconds(300)
                ))
                .build();
    }

    private static FundingListing closedListing(Long startupId,
                                                String title,
                                                String currencyCode,
                                                BigDecimal requestedAmount) {
        return FundingListing.builder()
                .startupId(startupId)
                .fundingModel(FundingModel.DEBT)
                .listingState(ListingState.CLOSED)
                .title(title)
                .fundingPurposeDescription("Funds needed for business expansion.")
                .createdAt(NOW.minusSeconds(500))
                .publishedAt(NOW.minusSeconds(400))
                .expiresAt(NOW.minusSeconds(200))
                .closedAt(NOW.minusSeconds(100))
                .debtTerms(ListingDebtTerms.create(
                        requestedAmount,
                        currencyCode,
                        new BigDecimal("9.50"),
                        new BigDecimal("12.75"),
                        18,
                        RepaymentPlanType.INSTALLMENT_MONTHLY,
                        null,
                        NOW.minusSeconds(500)
                ))
                .build();
    }

    private static Bid bid(Long listingId, Long investorId, BidState bidState) {
        return Bid.builder()
                .listingId(listingId)
                .investorId(investorId)
                .fundingModel(FundingModel.DEBT)
                .bidState(bidState)
                .proposalMessage("We are interested in funding this listing.")
                .createdAt(NOW.minusSeconds(investorId))
                .acceptedAt(bidState == BidState.ACCEPTED ? NOW.minusSeconds(30) : null)
                .withdrawnAt(bidState == BidState.WITHDRAWN ? NOW.minusSeconds(20) : null)
                .debtTerms(BidDebtTerms.create(
                        new BigDecimal("500000.00"),
                        new BigDecimal("10.50"),
                        18,
                        RepaymentPlanType.INSTALLMENT_MONTHLY,
                        null,
                        NOW.minusSeconds(investorId)
                ))
                .build();
    }

    private int runTwoWorkers(Supplier<Integer> worker) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> task = () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return inTransaction(worker);
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(task);
            Future<Integer> second = executor.submit(task);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return first.get() + second.get();
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private <T> T inTransaction(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
    }
}

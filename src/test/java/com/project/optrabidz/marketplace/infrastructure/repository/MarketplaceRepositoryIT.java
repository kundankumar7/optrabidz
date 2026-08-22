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
import com.project.optrabidz.testsupport.PostgresTestDataFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PostgresTestDataFixture testData;

    @BeforeEach
    void setUpTestData() {
        testData = new PostgresTestDataFixture(jdbcTemplate, NOW);
    }

    @Test
    void findOpenListingsUsesPostgresEnumAndAmountFilters() {
        FundingListing matching = listingRepository.save(openListing(
                testData.createStartup("matching listing").startupId(),
                "Working capital listing",
                "INR",
                new BigDecimal("550000.00"),
                NOW.plusSeconds(3_600)
        ));
        listingRepository.save(openListing(
                testData.createStartup("USD listing").startupId(),
                "USD listing",
                "USD",
                new BigDecimal("550000.00"),
                NOW.plusSeconds(3_600)
        ));
        listingRepository.save(closedListing(
                testData.createStartup("closed INR listing").startupId(),
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
                testData.createStartup("expired open listing").startupId(),
                "Expired open listing",
                "INR",
                new BigDecimal("300000.00"),
                NOW.minusSeconds(60)
        ));
        FundingListing futureOpen = listingRepository.save(openListing(
                testData.createStartup("future open listing").startupId(),
                "Future open listing",
                "INR",
                new BigDecimal("350000.00"),
                NOW.plusSeconds(3_600)
        ));
        FundingListing closed = listingRepository.save(closedListing(
                testData.createStartup("already closed listing").startupId(),
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
                saveOpenListing("parallel listing 1", "Expired listing 1", "300000.00", NOW.minusSeconds(600)),
                saveOpenListing("parallel listing 2", "Expired listing 2", "310000.00", NOW.minusSeconds(500)),
                saveOpenListing("parallel listing 3", "Expired listing 3", "320000.00", NOW.minusSeconds(400)),
                saveOpenListing("parallel listing 4", "Expired listing 4", "330000.00", NOW.minusSeconds(300)),
                saveOpenListing("parallel listing 5", "Expired listing 5", "340000.00", NOW.minusSeconds(200))
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
                testData.createStartup("bid query listing").startupId(),
                "Bid query listing",
                "INR",
                new BigDecimal("500000.00"),
                NOW.plusSeconds(3_600)
        ));
        Long submittedInvestorId = testData.createInvestor("submitted bid investor").investorId();
        Long acceptedInvestorId = testData.createInvestor("accepted bid investor").investorId();
        Long withdrawnInvestorId = testData.createInvestor("withdrawn bid investor").investorId();
        Bid submitted = bidRepository.save(bid(listing.getListingId(), submittedInvestorId, BidState.SUBMITTED));
        Bid accepted = bidRepository.save(bid(listing.getListingId(), acceptedInvestorId, BidState.ACCEPTED));
        bidRepository.save(bid(listing.getListingId(), withdrawnInvestorId, BidState.WITHDRAWN));

        assertThat(bidRepository.existsActiveByInvestorIdAndListingId(submittedInvestorId, listing.getListingId())).isTrue();
        assertThat(bidRepository.existsActiveByInvestorIdAndListingId(withdrawnInvestorId, listing.getListingId())).isFalse();
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

    @Test
    void agreementReachedConditionalUpdateChangesOnlyAnOpenListingOnce() {
        FundingListing listing = listingRepository.save(openListing(
                testData.createStartup("conditional update listing").startupId(),
                "Conditional update listing",
                "INR",
                new BigDecimal("575000.00"),
                NOW.plusSeconds(3_600)
        ));

        int firstUpdate = listingRepository.markAgreementReachedIfOpen(
                listing.getListingId(),
                NOW
        );
        int secondUpdate = listingRepository.markAgreementReachedIfOpen(
                listing.getListingId(),
                NOW.plusSeconds(1)
        );

        assertThat(firstUpdate).isEqualTo(1);
        assertThat(secondUpdate).isZero();
        assertThat(listingRepository.findById(listing.getListingId()))
                .get()
                .extracting(FundingListing::getListingState)
                .isEqualTo(ListingState.AGREEMENT_REACHED);
    }

    private static FundingListing openListing(Long startupId,
                                              String title,
                                              String currencyCode,
                                              BigDecimal requestedAmount,
                                              Instant expiresAt) {
        Instant createdAt = expiresAt.isBefore(NOW)
                ? expiresAt.minusSeconds(300)
                : NOW.minusSeconds(300);
        return FundingListing.builder()
                .startupId(startupId)
                .fundingModel(FundingModel.DEBT)
                .listingState(ListingState.OPEN)
                .title(title)
                .fundingPurposeDescription("Funds needed for business expansion.")
                .createdAt(createdAt)
                .publishedAt(createdAt.plusSeconds(100))
                .expiresAt(expiresAt)
                .debtTerms(ListingDebtTerms.create(
                        requestedAmount,
                        currencyCode,
                        new BigDecimal("9.50"),
                        new BigDecimal("12.75"),
                        18,
                        RepaymentPlanType.INSTALLMENT_MONTHLY,
                        null,
                        createdAt
                ))
                .build();
    }

    private Long saveOpenListing(String startupLabel,
                                 String title,
                                 String amount,
                                 Instant expiresAt) {
        Long startupId = testData.createStartup(startupLabel).startupId();
        return listingRepository.save(openListing(
                startupId,
                title,
                "INR",
                new BigDecimal(amount),
                expiresAt
        )).getListingId();
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
                .createdAt(NOW.minusSeconds(300))
                .acceptedAt(bidState == BidState.ACCEPTED ? NOW.minusSeconds(30) : null)
                .withdrawnAt(bidState == BidState.WITHDRAWN ? NOW.minusSeconds(20) : null)
                .debtTerms(BidDebtTerms.create(
                        new BigDecimal("500000.00"),
                        new BigDecimal("10.50"),
                        18,
                        RepaymentPlanType.INSTALLMENT_MONTHLY,
                        null,
                        NOW.minusSeconds(300)
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

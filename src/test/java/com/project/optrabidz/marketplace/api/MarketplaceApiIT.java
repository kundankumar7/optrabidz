package com.project.optrabidz.marketplace.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarketplaceApiIT extends ApiIntegrationTestSupport {

    @Test
    void startupCanCreateUpdatePublishBrowseAndCloseListing() throws Exception {
        AuthenticatedClient startup = eligibleStartup("Marketplace Startup");
        BigDecimal originalAmount = new BigDecimal("712345.67");
        BigDecimal updatedAmount = new BigDecimal("722345.67");

        MvcResult createResult = mockMvc.perform(post("/api/v1/funding-listings")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createListingRequest("Working Capital Listing", originalAmount))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.listingState").value("DRAFT"))
                .andExpect(jsonPath("$.data.fundingModel").value("DEBT"))
                .andReturn();
        Long listingId = readLong(createResult, "/data/listingId");

        mockMvc.perform(patch("/api/v1/funding-listings/{listingId}", listingId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateListingRequest("Updated Working Capital Listing", updatedAmount))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.listingId").value(listingId.intValue()))
                .andExpect(jsonPath("$.data.title").value("Updated Working Capital Listing"))
                .andExpect(jsonPath("$.data.listingState").value("DRAFT"));

        mockMvc.perform(post("/api/v1/funding-listings/{listingId}/actions/publish", listingId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.listingId").value(listingId.intValue()))
                .andExpect(jsonPath("$.data.listingState").value("OPEN"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/startups/me/funding-listings")
                        .queryParam("state", "OPEN")
                        .queryParam("fundingModel", "DEBT")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].listingId").value(listingId.intValue()))
                .andExpect(jsonPath("$.data.items[0].listingState").value("OPEN"));

        mockMvc.perform(get("/api/v1/funding-listings")
                        .queryParam("fundingModel", "DEBT")
                        .queryParam("minAmount", updatedAmount.toPlainString())
                        .queryParam("maxAmount", updatedAmount.toPlainString())
                        .queryParam("currencyCode", "INR")
                        .queryParam("page", "1")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].listingId").value(listingId.intValue()))
                .andExpect(jsonPath("$.data.items[0].listingState").value("OPEN"));

        mockMvc.perform(get("/api/v1/funding-listings/{listingId}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.listingId").value(listingId.intValue()))
                .andExpect(jsonPath("$.data.listingState").value("OPEN"));

        mockMvc.perform(post("/api/v1/funding-listings/{listingId}/actions/close", listingId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "Test cleanup close"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.listingId").value(listingId.intValue()))
                .andExpect(jsonPath("$.data.listingState").value("CLOSED"))
                .andExpect(jsonPath("$.data.closedAt").isNotEmpty());
    }

    @Test
    void browseListingsHonorsNewestAndClosingSoonSorts() throws Exception {
        AuthenticatedClient startup = eligibleStartup("Marketplace Sort Startup");
        BigDecimal firstAmount = new BigDecimal("912345.11");
        BigDecimal secondAmount = new BigDecimal("912345.12");

        Long firstListingId = createAndPublishListing(startup, "First Sort Listing", firstAmount);
        TimeUnit.MILLISECONDS.sleep(10);
        Long secondListingId = createAndPublishListing(startup, "Second Sort Listing", secondAmount);

        mockMvc.perform(get("/api/v1/funding-listings")
                        .queryParam("fundingModel", "DEBT")
                        .queryParam("minAmount", firstAmount.toPlainString())
                        .queryParam("maxAmount", secondAmount.toPlainString())
                        .queryParam("currencyCode", "INR")
                        .queryParam("sort", "NEWEST")
                        .queryParam("page", "1")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.items[0].listingId").value(secondListingId.intValue()))
                .andExpect(jsonPath("$.data.items[1].listingId").value(firstListingId.intValue()));

        mockMvc.perform(get("/api/v1/funding-listings")
                        .queryParam("fundingModel", "DEBT")
                        .queryParam("minAmount", firstAmount.toPlainString())
                        .queryParam("maxAmount", secondAmount.toPlainString())
                        .queryParam("currencyCode", "INR")
                        .queryParam("sort", "CLOSING_SOON")
                        .queryParam("page", "1")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.items[0].listingId").value(firstListingId.intValue()))
                .andExpect(jsonPath("$.data.items[1].listingId").value(secondListingId.intValue()));
    }

    @Test
    void publishingRequiresStartupClassificationWhenGovernanceRequiresIt() throws Exception {
        String requestId = "kan-27-marketplace-governance";
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);
        createCompleteStartupProfile(startup, "Unclassified Startup");

        MvcResult createResult = mockMvc.perform(post("/api/v1/funding-listings")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createListingRequest("Unclassified Listing", new BigDecimal("743210.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.listingState").value("DRAFT"))
                .andReturn();
        Long listingId = readLong(createResult, "/data/listingId");

        mockMvc.perform(post("/api/v1/funding-listings/{listingId}/actions/publish", listingId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("X-Request-Id", requestId))
                .andExpect(jsonPath("$.type").value(
                        "urn:optrabidz:problem:governance-action-not-eligible"))
                .andExpect(jsonPath("$.title").value("Business rule violation"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value(
                        "The requested action does not satisfy governance eligibility rules"))
                .andExpect(jsonPath("$.instance").value(
                        "urn:optrabidz:request:" + requestId))
                .andExpect(jsonPath("$.code").value("GOVERNANCE_ACTION_NOT_ELIGIBLE"))
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(content().string(not(containsString(
                        "Startup is not eligible to publish listings"))));
    }

    @Test
    void investorCanDiscoverRecommendedListingSubmitBidAndStartupCanAccept() throws Exception {
        AuthenticatedClient startup = eligibleStartup("Agreement Startup");
        BigDecimal requestedAmount = new BigDecimal("834567.89");
        Long listingId = createAndPublishListing(startup, "Recommended Fintech Listing", requestedAmount);

        AuthenticatedClient investor = eligibleInvestor("Agreement Investor");

        mockMvc.perform(get("/api/v1/funding-listings/recommended")
                        .queryParam("fundingModel", "DEBT")
                        .queryParam("minAmount", requestedAmount.toPlainString())
                        .queryParam("maxAmount", requestedAmount.toPlainString())
                        .queryParam("currencyCode", "INR")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].listing.listingId").value(listingId.intValue()))
                .andExpect(jsonPath("$.data.items[0].recommendation.score").value(greaterThan(0)));

        MvcResult bidResult = mockMvc.perform(post("/api/v1/bids")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(submitBidRequest(listingId, new BigDecimal("825000.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.listingId").value(listingId.intValue()))
                .andExpect(jsonPath("$.data.bidState").value("SUBMITTED"))
                .andReturn();
        Long bidId = readLong(bidResult, "/data/bidId");

        mockMvc.perform(get("/api/v1/investors/me/bids/by-listing/{listingId}", listingId)
                        .session(investor.session())
                        .cookie(investor.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bidId").value(bidId.intValue()))
                .andExpect(jsonPath("$.data.bidState").value("SUBMITTED"));

        mockMvc.perform(get("/api/v1/bids")
                        .queryParam("listingId", listingId.toString())
                        .queryParam("state", "SUBMITTED")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].bidId").value(bidId.intValue()));

        MvcResult acceptResult = mockMvc.perform(post("/api/v1/bids/{bidId}/actions/accept", bidId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmation", "ACCEPT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bid.bidId").value(bidId.intValue()))
                .andExpect(jsonPath("$.data.bid.bidState").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.listing.listingId").value(listingId.intValue()))
                .andExpect(jsonPath("$.data.listing.listingState").value("AGREEMENT_REACHED"))
                .andExpect(jsonPath("$.data.agreement.listingId").value(listingId.intValue()))
                .andExpect(jsonPath("$.data.agreement.bidId").value(bidId.intValue()))
                .andReturn();
        Long agreementId = readLong(acceptResult, "/data/agreement/agreementId");

        mockMvc.perform(get("/api/v1/funding-listings/{listingId}/accepted-bid", listingId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bidId").value(bidId.intValue()))
                .andExpect(jsonPath("$.data.bidState").value("ACCEPTED"));

        mockMvc.perform(get("/api/v1/startups/me/agreements")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].agreementId").value(agreementId.intValue()));

        mockMvc.perform(get("/api/v1/investors/me/agreements")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].agreementId").value(agreementId.intValue()));

        mockMvc.perform(get("/api/v1/agreements/{agreementId}", agreementId)
                        .session(investor.session())
                        .cookie(investor.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agreementId").value(agreementId.intValue()))
                .andExpect(jsonPath("$.data.debtTerms.principalAmount").value(825000.00));
    }

    @Test
    void duplicateBidAndWrongBidRolesAreRejected() throws Exception {
        AuthenticatedClient startup = eligibleStartup("Duplicate Bid Startup");
        Long listingId = createAndPublishListing(startup, "Duplicate Bid Listing", new BigDecimal("845678.90"));
        AuthenticatedClient investor = eligibleInvestor("Duplicate Bid Investor");

        mockMvc.perform(post("/api/v1/bids")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(submitBidRequest(listingId, new BigDecimal("830000.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bidState").value("SUBMITTED"));

        String duplicateRequestId = "kan-28-bid-already-exists";
        MvcResult duplicateResult = mockMvc.perform(post("/api/v1/bids")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .header("X-Request-Id", duplicateRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(submitBidRequest(listingId, new BigDecimal("831000.00")))))
                .andExpectAll(marketplaceProblem(
                        409,
                        "BID_ALREADY_EXISTS",
                        "An active bid already exists for this listing",
                        "urn:optrabidz:request:" + duplicateRequestId,
                        duplicateRequestId
                ))
                .andReturn();
        assertBodyExcludes(
                duplicateResult,
                "Investor already has an active bid for this listing",
                "MARKETPLACE.BID.ALREADY_EXISTS"
        );

        String accessRequestId = "kan-28-marketplace-access";
        MvcResult accessResult = mockMvc.perform(post("/api/v1/bids")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .header("X-Request-Id", accessRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(submitBidRequest(listingId, new BigDecimal("832000.00")))))
                .andExpectAll(marketplaceProblem(
                        403,
                        "MARKETPLACE_ACCESS_DENIED",
                        "You are not authorized to perform this marketplace action",
                        "urn:optrabidz:request:" + accessRequestId,
                        accessRequestId
                ))
                .andReturn();
        assertBodyExcludes(
                accessResult,
                "STARTUP",
                "INVESTOR",
                "MARKETPLACE.ACCESS.DENIED"
        );
    }

    @Test
    void missingMarketplaceResourcesUseSafeProblemDetails() throws Exception {
        long missingId = Long.MAX_VALUE;
        String listingRequestId = "kan-28-listing-not-found";
        MvcResult listingResult = mockMvc.perform(get(
                        "/api/v1/funding-listings/{listingId}",
                        missingId
                ).header("X-Request-Id", listingRequestId))
                .andExpectAll(marketplaceProblem(
                        404,
                        "LISTING_NOT_FOUND",
                        "The requested listing was not found",
                        "urn:optrabidz:request:" + listingRequestId,
                        listingRequestId
                ))
                .andReturn();
        assertBodyExcludes(
                listingResult,
                Long.toString(missingId),
                "MARKETPLACE.LISTING.NOT_FOUND"
        );

        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);
        String bidRequestId = "kan-28-bid-not-found";
        MvcResult bidResult = mockMvc.perform(get("/api/v1/bids/{bidId}", missingId)
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-Request-Id", bidRequestId))
                .andExpectAll(marketplaceProblem(
                        404,
                        "BID_NOT_FOUND",
                        "The requested bid was not found",
                        "urn:optrabidz:request:" + bidRequestId,
                        bidRequestId
                ))
                .andReturn();
        assertBodyExcludes(
                bidResult,
                Long.toString(missingId),
                "MARKETPLACE.BID.NOT_FOUND"
        );

        String agreementRequestId = "kan-28-agreement-not-found";
        MvcResult agreementResult = mockMvc.perform(get(
                        "/api/v1/agreements/{agreementId}",
                        missingId
                )
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-Request-Id", agreementRequestId))
                .andExpectAll(marketplaceProblem(
                        404,
                        "AGREEMENT_NOT_FOUND",
                        "The requested agreement was not found",
                        "urn:optrabidz:request:" + agreementRequestId,
                        agreementRequestId
                ))
                .andReturn();
        assertBodyExcludes(
                agreementResult,
                Long.toString(missingId),
                "MARKETPLACE.AGREEMENT.NOT_FOUND"
        );
    }

    @Test
    void listingStateAndFundingModelFailuresUseSafeProblemDetails() throws Exception {
        AuthenticatedClient startup = eligibleStartup("Listing Failure Startup");
        Long listingId = createAndPublishListing(
                startup,
                "Listing State Failure",
                new BigDecimal("856789.01")
        );

        String stateRequestId = "kan-28-listing-state";
        MvcResult stateResult = mockMvc.perform(patch(
                        "/api/v1/funding-listings/{listingId}",
                        listingId
                )
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .header("X-Request-Id", stateRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateListingRequest(
                                "Invalid update",
                                new BigDecimal("856790.01")
                        ))))
                .andExpectAll(marketplaceProblem(
                        409,
                        "LISTING_STATE_CONFLICT",
                        "The requested action conflicts with the current listing state",
                        "urn:optrabidz:request:" + stateRequestId,
                        stateRequestId
                ))
                .andReturn();
        assertBodyExcludes(
                stateResult,
                "OPEN",
                "Listing " + listingId,
                "MARKETPLACE.LISTING.STATE_CONFLICT"
        );

        String modelRequestId = "kan-28-unsupported-funding-model";
        Map<String, Object> equityRequest = Map.of(
                "fundingModel", "EQUITY",
                "title", "Unsupported equity listing",
                "fundingPurposeDescription",
                "Funds needed for working capital and business expansion.",
                "debtTerms", listingDebtTerms(new BigDecimal("860000.00"))
        );
        MvcResult modelResult = mockMvc.perform(post("/api/v1/funding-listings")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .header("X-Request-Id", modelRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(equityRequest)))
                .andExpectAll(marketplaceProblem(
                        422,
                        "UNSUPPORTED_FUNDING_MODEL",
                        "The requested funding model is not supported",
                        "urn:optrabidz:request:" + modelRequestId,
                        modelRequestId
                ))
                .andReturn();
        assertBodyExcludes(
                modelResult,
                "EQUITY",
                "MARKETPLACE.FUNDING_MODEL.UNSUPPORTED"
        );
    }

    @Test
    void bidStateAndAcceptanceFailuresUseSafeProblemDetails() throws Exception {
        AuthenticatedClient startup = eligibleStartup("Bid Failure Startup");
        Long listingId = createAndPublishListing(
                startup,
                "Bid Failure Listing",
                new BigDecimal("867890.12")
        );
        AuthenticatedClient firstInvestor = eligibleInvestor("Bid Failure Investor One");
        AuthenticatedClient secondInvestor = eligibleInvestor("Bid Failure Investor Two");
        Long firstBidId = submitBid(
                firstInvestor,
                listingId,
                new BigDecimal("850000.00")
        );
        Long secondBidId = submitBid(
                secondInvestor,
                listingId,
                new BigDecimal("851000.00")
        );

        mockMvc.perform(post("/api/v1/bids/{bidId}/actions/withdraw", secondBidId)
                        .session(secondInvestor.session())
                        .cookie(secondInvestor.xsrfCookie())
                        .header("X-CSRF-TOKEN", secondInvestor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "Changed investment strategy"))))
                .andExpect(status().isOk());

        String bidStateRequestId = "kan-28-bid-state";
        MvcResult bidStateResult = mockMvc.perform(post(
                        "/api/v1/bids/{bidId}/actions/withdraw",
                        secondBidId
                )
                        .session(secondInvestor.session())
                        .cookie(secondInvestor.xsrfCookie())
                        .header("X-CSRF-TOKEN", secondInvestor.csrfToken())
                        .header("X-Request-Id", bidStateRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "Repeated withdrawal"))))
                .andExpectAll(marketplaceProblem(
                        409,
                        "BID_STATE_CONFLICT",
                        "The requested action conflicts with the current bid state",
                        "urn:optrabidz:request:" + bidStateRequestId,
                        bidStateRequestId
                ))
                .andReturn();
        assertBodyExcludes(
                bidStateResult,
                "SUBMITTED",
                "Bid " + secondBidId,
                "MARKETPLACE.BID.STATE_CONFLICT"
        );

        mockMvc.perform(post("/api/v1/bids/{bidId}/actions/accept", firstBidId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmation", "ACCEPT"))))
                .andExpect(status().isOk());

        String acceptanceRequestId = "kan-28-bid-acceptance";
        MvcResult acceptanceResult = mockMvc.perform(post(
                        "/api/v1/bids/{bidId}/actions/accept",
                        secondBidId
                )
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .header("X-Request-Id", acceptanceRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmation", "ACCEPT"))))
                .andExpectAll(marketplaceProblem(
                        409,
                        "BID_ACCEPTANCE_CONFLICT",
                        "The bid cannot be accepted in the current marketplace state",
                        "urn:optrabidz:request:" + acceptanceRequestId,
                        acceptanceRequestId
                ))
                .andReturn();
        assertBodyExcludes(
                acceptanceResult,
                "AGREEMENT_REACHED",
                "Listing " + listingId,
                "bid " + secondBidId,
                "MARKETPLACE.BID.ACCEPTANCE_CONFLICT"
        );
    }

    @Test
    void concurrentBidAcceptanceAllowsOnlyOneAcceptedBidPerListing() throws Exception {
        AuthenticatedClient startup = eligibleStartup("Concurrent Accept Startup");
        Long listingId = createAndPublishListing(
                startup,
                "Concurrent Accept Listing",
                new BigDecimal("845678.90")
        );
        AuthenticatedClient firstInvestor = eligibleInvestor("Concurrent Investor One");
        AuthenticatedClient secondInvestor = eligibleInvestor("Concurrent Investor Two");
        Long firstBidId = submitBid(firstInvestor, listingId, new BigDecimal("830000.00"));
        Long secondBidId = submitBid(secondInvestor, listingId, new BigDecimal("831000.00"));

        List<Integer> statusCodes = acceptBidsConcurrently(startup, firstBidId, secondBidId);

        assertThat(statusCodes).containsExactlyInAnyOrder(200, 409);
        mockMvc.perform(get("/api/v1/funding-listings/{listingId}/accepted-bid", listingId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bidState").value("ACCEPTED"));

        String firstBidState = readText(mockMvc.perform(get("/api/v1/bids/{bidId}", firstBidId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andReturn(), "/data/bidState");
        String secondBidState = readText(mockMvc.perform(get("/api/v1/bids/{bidId}", secondBidId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andReturn(), "/data/bidState");
        assertThat(List.of(firstBidState, secondBidState)).containsExactlyInAnyOrder("ACCEPTED", "REJECTED");

        mockMvc.perform(get("/api/v1/startups/me/agreements")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1));
    }

    @Test
    void marketplaceMutationsRequireCsrfHeader() throws Exception {
        AuthenticatedClient startup = eligibleStartup("CSRF Marketplace Startup");

        mockMvc.perform(post("/api/v1/funding-listings")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createListingRequest("CSRF Listing", new BigDecimal("765432.10")))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("Request security validation failed"));
    }

    private AuthenticatedClient eligibleStartup(String publicDisplayName) throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);
        createCompleteStartupProfile(startup, publicDisplayName);
        addStartupClassification(startup, "SECTOR", "FINTECH");
        return startup;
    }

    private AuthenticatedClient eligibleInvestor(String publicDisplayName) throws Exception {
        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);
        createCompleteInvestorProfile(investor, publicDisplayName);
        addInvestorPreference(investor, "SECTOR", "FINTECH");
        return investor;
    }

    private Long createAndPublishListing(AuthenticatedClient startup,
                                         String title,
                                         BigDecimal requestedAmount) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/funding-listings")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createListingRequest(title, requestedAmount))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.listingState").value("DRAFT"))
                .andReturn();
        Long listingId = readLong(createResult, "/data/listingId");

        mockMvc.perform(post("/api/v1/funding-listings/{listingId}/actions/publish", listingId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.listingState").value("OPEN"));

        return listingId;
    }

    private Long submitBid(AuthenticatedClient investor, Long listingId, BigDecimal proposedAmount) throws Exception {
        MvcResult bidResult = mockMvc.perform(post("/api/v1/bids")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(submitBidRequest(listingId, proposedAmount))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bidState").value("SUBMITTED"))
                .andReturn();
        return readLong(bidResult, "/data/bidId");
    }

    private List<Integer> acceptBidsConcurrently(AuthenticatedClient startup,
                                                 Long firstBidId,
                                                 Long secondBidId) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(acceptBidTask(startup, firstBidId, ready, start));
            Future<Integer> second = executor.submit(acceptBidTask(startup, secondBidId, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(first.get(), second.get());
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Callable<Integer> acceptBidTask(AuthenticatedClient startup,
                                            Long bidId,
                                            CountDownLatch ready,
                                            CountDownLatch start) {
        return () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return mockMvc.perform(post("/api/v1/bids/{bidId}/actions/accept", bidId)
                            .session(startup.session())
                            .cookie(startup.xsrfCookie())
                            .header("X-CSRF-TOKEN", startup.csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("confirmation", "ACCEPT"))))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };
    }

    private Map<String, Object> createListingRequest(String title, BigDecimal requestedAmount) {
        return Map.of(
                "fundingModel", "DEBT",
                "title", title,
                "fundingPurposeDescription", "Funds needed for working capital and business expansion.",
                "debtTerms", listingDebtTerms(requestedAmount)
        );
    }

    private Map<String, Object> updateListingRequest(String title, BigDecimal requestedAmount) {
        return Map.of(
                "title", title,
                "fundingPurposeDescription", "Updated funds needed for working capital and business expansion.",
                "debtTerms", listingDebtTerms(requestedAmount)
        );
    }

    private Map<String, Object> listingDebtTerms(BigDecimal requestedAmount) {
        return Map.of(
                "requestedAmount", requestedAmount,
                "currencyCode", "INR",
                "minimumInterestRate", new BigDecimal("8.50"),
                "maximumInterestRate", new BigDecimal("12.75"),
                "requestedTenureMonths", 18,
                "repaymentPlanType", "INSTALLMENT_MONTHLY"
        );
    }

    private Map<String, Object> submitBidRequest(Long listingId, BigDecimal proposedAmount) {
        return Map.of(
                "listingId", listingId,
                "fundingModel", "DEBT",
                "debtTerms", Map.of(
                        "proposedAmount", proposedAmount,
                        "proposedInterestRate", new BigDecimal("10.25"),
                        "proposedTenureMonths", 18,
                        "repaymentPlanType", "INSTALLMENT_MONTHLY"
                ),
                "proposalMessage", "We are interested in funding this listing under the proposed debt terms."
        );
    }

    private Long readLong(MvcResult result, String jsonPointer) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString()).at(jsonPointer);
        return node.asLong();
    }

    private String readText(MvcResult result, String jsonPointer) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString()).at(jsonPointer);
        return node.asText();
    }

    private ResultMatcher[] marketplaceProblem(
            int expectedStatus,
            String code,
            String detail,
            String instance,
            String requestId
    ) {
        return new ResultMatcher[] {
                result -> assertThat(result.getResponse().getStatus())
                        .isEqualTo(expectedStatus),
                content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON),
                jsonPath("$.status").value(expectedStatus),
                jsonPath("$.code").value(code),
                jsonPath("$.detail").value(detail),
                jsonPath("$.instance").value(instance),
                jsonPath("$.requestId").value(requestId),
                jsonPath("$.timestamp").isString(),
                jsonPath("$.success").doesNotExist(),
                jsonPath("$.error").doesNotExist()
        };
    }

    private void assertBodyExcludes(
            MvcResult result,
            String... protectedValues
    ) throws Exception {
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(protectedValues);
    }
}

package com.project.optrabidz.marketplace.api;

import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarketplaceSecurityIT extends ApiIntegrationTestSupport {

    @Test
    void anonymousListingBrowseAndDetailRemainPublic() throws Exception {
        mockMvc.perform(get("/api/v1/funding-listings"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/funding-listings/{listingId}", Long.MAX_VALUE)
                        .header("X-Request-Id", "kan-28-public-detail"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("LISTING_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").value("kan-28-public-detail"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/funding-listings/recommended",
            "/api/v1/bids/1",
            "/api/v1/funding-listings/1/accepted-bid",
            "/api/v1/agreements/1"
    })
    void anonymousActorRequiredQueriesUseSharedAuthenticationBoundary(String path)
            throws Exception {
        mockMvc.perform(get(path).header("X-Request-Id", "kan-28-security"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.requestId").value("kan-28-security"));
    }

    @Test
    void anonymousListingCommandUsesSharedAuthenticationBoundary() throws Exception {
        Map<String, Object> request = Map.of(
                "fundingModel", "DEBT",
                "title", "Anonymous listing",
                "fundingPurposeDescription",
                "Funds needed for working capital and business expansion.",
                "debtTerms", Map.of(
                        "requestedAmount", new BigDecimal("500000.00"),
                        "currencyCode", "INR",
                        "minimumInterestRate", new BigDecimal("8.50"),
                        "maximumInterestRate", new BigDecimal("12.75"),
                        "requestedTenureMonths", 18,
                        "repaymentPlanType", "INSTALLMENT_MONTHLY"
                )
        );

        mockMvc.perform(post("/api/v1/funding-listings")
                        .with(csrf())
                        .header("X-Request-Id", "kan-28-security-command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.requestId").value(
                        "kan-28-security-command"));
    }
}

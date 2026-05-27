package com.project.optrabidz.audit.api;

import com.project.optrabidz.common.outbox.OutboxDispatcher;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BusinessAuditIT extends ApiIntegrationTestSupport {
    @Autowired
    private OutboxDispatcher outboxDispatcher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void businessOutboxAuditUsesPolicySummaryAndActorContext() throws Exception {
        String email = uniqueEmail("audit-business-startup");
        register(email, DEFAULT_PASSWORD, RoleType.STARTUP)
                .andExpect(status().isCreated());
        AuthenticatedClient startup = login(email, DEFAULT_PASSWORD);
        createCompleteStartupProfile(startup, "Audit Business Startup");
        addStartupClassification(startup, "INDUSTRY", "SAAS");

        outboxDispatcher.dispatchPending();

        Long accountId = jdbcTemplate.queryForObject("""
                select account_id
                from credential
                where lower(email) = lower(?)
                """, Long.class, email);

        Map<String, Object> accountAudit = jdbcTemplate.queryForMap("""
                select action, object_type, object_id, actor_account_id, actor_role, details::text as details
                from audit_record
                where event_type = 'AccountRegisteredEvent'
                  and action = 'ACCOUNT_REGISTERED'
                  and actor_account_id = ?
                order by audit_record_id desc
                limit 1
                """, accountId);

        assertThat(accountAudit.get("object_type")).isEqualTo("ACCOUNT");
        assertThat(accountAudit.get("object_id")).isEqualTo(String.valueOf(accountId));
        assertThat(accountAudit.get("actor_account_id")).isEqualTo(accountId);
        assertThat(accountAudit.get("actor_role")).isEqualTo("STARTUP");
        assertThat(accountAudit.get("details").toString()).contains("\"roleType\": \"STARTUP\"");
        assertThat(accountAudit.get("details").toString()).doesNotContain(DEFAULT_PASSWORD);
        assertThat(accountAudit.get("details").toString()).doesNotContain(email);

        long listingId = createListing(startup);
        publishListing(startup, listingId);
        outboxDispatcher.dispatchPending();

        Map<String, Object> listingAudit = jdbcTemplate.queryForMap("""
                select action, object_type, object_id, actor_account_id, actor_role, details::text as details
                from audit_record
                where event_type = 'ListingPublishedEvent'
                  and action = 'LISTING_PUBLISHED'
                  and object_id = ?
                order by audit_record_id desc
                limit 1
                """, String.valueOf(listingId));

        assertThat(listingAudit.get("object_type")).isEqualTo("FUNDING_LISTING");
        assertThat(listingAudit.get("actor_account_id")).isEqualTo(accountId);
        assertThat(listingAudit.get("actor_role")).isEqualTo("STARTUP");
        assertThat(listingAudit.get("details").toString()).contains("\"listingId\": " + listingId);
        assertThat(listingAudit.get("details").toString()).doesNotContain("occurredAt");
    }

    private long createListing(AuthenticatedClient startup) throws Exception {
        String response = mockMvc.perform(post("/api/v1/funding-listings")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fundingModel", "DEBT",
                                "title", "Audit policy listing",
                                "fundingPurposeDescription", "Listing used to verify audit policy behavior.",
                                "debtTerms", Map.of(
                                        "requestedAmount", new BigDecimal("250000.00"),
                                        "currencyCode", "INR",
                                        "minimumInterestRate", new BigDecimal("9.50"),
                                        "maximumInterestRate", new BigDecimal("13.50"),
                                        "requestedTenureMonths", 12,
                                        "repaymentPlanType", "INSTALLMENT_MONTHLY"
                                )
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("listingId").asLong();
    }

    private void publishListing(AuthenticatedClient startup, long listingId) throws Exception {
        mockMvc.perform(post("/api/v1/funding-listings/{listingId}/actions/publish", listingId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}

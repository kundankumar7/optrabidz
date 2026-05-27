package com.project.optrabidz.notification.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.optrabidz.common.outbox.OutboxDispatcher;
import com.project.optrabidz.notification.application.channel.NotificationDeliveryDispatcher;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.security.infrastructure.config.SecuritySessionConstants;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationApiIT extends ApiIntegrationTestSupport {
    @Autowired
    private OutboxDispatcher outboxDispatcher;

    @Autowired
    private NotificationDeliveryDispatcher notificationDeliveryDispatcher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void outboxDispatchCreatesNotificationAndAuditForRegisteredAccount() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);

        outboxDispatcher.dispatchPending();

        String feedJson = mockMvc.perform(get("/api/v1/notifications/me?page=1&size=20")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].notificationName").value("ACCOUNT_REGISTERED"))
                .andExpect(jsonPath("$.data.items[0].readStatus").value("UNREAD"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode firstNotification = objectMapper.readTree(feedJson).path("data").path("items").get(0);
        long recipientId = firstNotification.path("recipientId").asLong();
        long accountId = firstNotification.path("entityId").asLong();

        mockMvc.perform(get("/api/v1/notifications/me/summary")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        mockMvc.perform(patch("/api/v1/notifications/{recipientId}/read", recipientId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Notification marked as read"));

        mockMvc.perform(patch("/api/v1/notifications/me/read-all")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken()))
                .andExpect(status().isOk());

        String subscriptionJson = mockMvc.perform(post("/api/v1/notification-subscriptions")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "channelType", "PUSH",
                                "endpoint", "https://push.example.com/subscription/test",
                                "publicKey", "public-key",
                                "authSecret", "auth-secret"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Notification subscription saved"))
                .andExpect(jsonPath("$.data.subscriptionId").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long subscriptionId = objectMapper.readTree(subscriptionJson).path("data").path("subscriptionId").asLong();

        mockMvc.perform(delete("/api/v1/notification-subscriptions/{subscriptionId}", subscriptionId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Notification subscription revoked"));

        mockMvc.perform(delete("/api/v1/notifications/{recipientId}", recipientId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Notification deleted"));

        Integer auditRecords = jdbcTemplate.queryForObject("""
                select count(*)
                from audit_record
                where event_type = 'AccountRegisteredEvent'
                  and action = 'ACCOUNT_REGISTERED'
                  and actor_account_id = ?
                """, Integer.class, accountId);
        assertThat(auditRecords).isEqualTo(1);

        outboxDispatcher.dispatchPending();

        Integer duplicateNotificationCount = jdbcTemplate.queryForObject("""
                select count(*)
                from notification
                where event_type = 'AccountRegisteredEvent'
                  and notification_name = 'ACCOUNT_REGISTERED'
                  and entity_id = ?
                """, Integer.class, accountId);
        Integer duplicateAuditCount = jdbcTemplate.queryForObject("""
                select count(*)
                from audit_record
                where event_type = 'AccountRegisteredEvent'
                  and action = 'ACCOUNT_REGISTERED'
                  and actor_account_id = ?
                """, Integer.class, accountId);
        assertThat(duplicateNotificationCount).isEqualTo(1);
        assertThat(duplicateAuditCount).isEqualTo(1);
    }

    @Test
    void subscribedEmailAndPushChannelsAreDispatchedWithAttemptHistory() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);
        long accountId = accountIdBySessionClient(startup);
        outboxDispatcher.dispatchPending();

        createSubscription(startup, "EMAIL", "startup-channel-test@example.com", null, null);
        createSubscription(startup, "PUSH", "https://push.example.com/subscription/channel-test", "public-key", "auth-secret");
        createCompleteStartupProfile(startup, "Notification Channel Startup");

        outboxDispatcher.dispatchPending();
        int processed = notificationDeliveryDispatcher.dispatchReadyDeliveries();

        assertThat(processed).isEqualTo(2);

        Map<String, Object> deliveryStats = jdbcTemplate.queryForMap("""
                select
                    count(*) as delivery_count,
                    count(*) filter (
                        where d.channel_delivery_status = 'DELIVERED'::channel_delivery_status_enum
                    ) as delivered_count,
                    count(*) filter (
                        where d.channel_type = 'IN_APP'::channel_type_enum
                    ) as in_app_count,
                    count(*) filter (
                        where d.channel_type = 'EMAIL'::channel_type_enum
                    ) as email_count,
                    count(*) filter (
                        where d.channel_type = 'PUSH'::channel_type_enum
                    ) as push_count
                from notification_delivery d
                join notification_recipient r on r.recipient_id = d.recipient_id
                join notification n on n.notification_id = r.notification_id
                where r.account_id = ?
                  and n.event_type = 'ParticipationProfileChangedEvent'
                """, accountId);

        assertThat(deliveryStats.get("delivery_count")).isEqualTo(3L);
        assertThat(deliveryStats.get("delivered_count")).isEqualTo(3L);
        assertThat(deliveryStats.get("in_app_count")).isEqualTo(1L);
        assertThat(deliveryStats.get("email_count")).isEqualTo(1L);
        assertThat(deliveryStats.get("push_count")).isEqualTo(1L);

        Integer attemptCount = jdbcTemplate.queryForObject("""
                select count(*)
                from notification_delivery_attempt a
                join notification_delivery d on d.delivery_id = a.delivery_id
                join notification_recipient r on r.recipient_id = d.recipient_id
                join notification n on n.notification_id = r.notification_id
                where r.account_id = ?
                  and n.event_type = 'ParticipationProfileChangedEvent'
                  and a.attempt_status = 'DELIVERED'
                """, Integer.class, accountId);
        assertThat(attemptCount).isEqualTo(3);
    }

    @Test
    void marketplaceAndFinanceEventsCreateNotificationsAndAuditRecords() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);
        createCompleteStartupProfile(startup, "Notification Startup");
        addStartupClassification(startup, "INDUSTRY", "SAAS");

        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);
        createCompleteInvestorProfile(investor, "Notification Investor");
        addInvestorPreference(investor, "INDUSTRY", "SAAS");

        outboxDispatcher.dispatchPending();

        long listingId = createListing(startup);
        publishListing(startup, listingId);
        outboxDispatcher.dispatchPending();
        assertFeedContains(startup, "LISTING_PUBLISHED");
        assertNotificationAndAuditExist("ListingPublishedEvent", "LISTING_PUBLISHED");

        long bidId = submitBid(investor, listingId);
        outboxDispatcher.dispatchPending();
        assertFeedContains(startup, "BID_SUBMITTED");
        assertNotificationAndAuditExist("BidSubmittedEvent", "BID_SUBMITTED");

        acceptBid(startup, bidId);
        outboxDispatcher.dispatchPending();
        assertFeedContains(investor, "BID_ACCEPTED");
        assertFeedContains(startup, "AGREEMENT_CREATED");
        assertFeedContains(investor, "AGREEMENT_CREATED");
        assertNotificationAndAuditExist("BidAcceptedEvent", "BID_ACCEPTED");
        assertNotificationAndAuditExist("AgreementCreatedEvent", "AGREEMENT_CREATED");

        long settlementId = firstIdFromPagedItems(investor, "/api/v1/investors/me/settlements?page=1&size=20", "settlementId");
        long settlementPaymentIntentId = createPaymentIntent(investor, "/api/v1/settlements/" + settlementId + "/payment-intents");
        long settlementPaymentAttemptId = createLocalPaymentAttempt(investor, settlementPaymentIntentId);
        confirmLocalPaymentAttempt(investor, settlementPaymentAttemptId);
        outboxDispatcher.dispatchPending();
        assertFeedContains(startup, "SETTLEMENT_CONFIRMED");
        assertFeedContains(investor, "SETTLEMENT_CONFIRMED");
        assertNotificationAndAuditExist("SettlementConfirmedEvent", "SETTLEMENT_CONFIRMED");

        long installmentId = firstIdFromPagedItems(startup, "/api/v1/startups/me/repayment-installments?page=1&size=20", "repaymentInstallmentId");
        long repaymentPaymentIntentId = createPaymentIntent(startup, "/api/v1/repayment-installments/" + installmentId + "/payment-intents");
        long repaymentPaymentAttemptId = createLocalPaymentAttempt(startup, repaymentPaymentIntentId);
        confirmLocalPaymentAttempt(startup, repaymentPaymentAttemptId);
        outboxDispatcher.dispatchPending();
        assertFeedContains(startup, "REPAYMENT_INSTALLMENT_PAID");
        assertFeedContains(investor, "REPAYMENT_INSTALLMENT_PAID");
        assertNotificationAndAuditExist("RepaymentInstallmentPaidEvent", "REPAYMENT_INSTALLMENT_PAID");
    }

    private long createListing(AuthenticatedClient startup) throws Exception {
        String response = mockMvc.perform(post("/api/v1/funding-listings")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fundingModel", "DEBT",
                                "title", "Notification integration listing",
                                "fundingPurposeDescription", "Listing used to verify cross-module notification and audit events.",
                                "debtTerms", Map.of(
                                        "requestedAmount", new BigDecimal("500000.00"),
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

    private long submitBid(AuthenticatedClient investor, long listingId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/bids")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "listingId", listingId,
                                "fundingModel", "DEBT",
                                "debtTerms", Map.of(
                                        "proposedAmount", new BigDecimal("500000.00"),
                                        "proposedInterestRate", new BigDecimal("10.50"),
                                        "proposedTenureMonths", 12,
                                        "repaymentPlanType", "INSTALLMENT_MONTHLY"
                                ),
                                "proposalMessage", "Investor bid used for notification integration testing."
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("bidId").asLong();
    }

    private void acceptBid(AuthenticatedClient startup, long bidId) throws Exception {
        mockMvc.perform(post("/api/v1/bids/{bidId}/actions/accept", bidId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "reason", "Accepted for notification integration testing.",
                                "confirmation", "ACCEPT"
                        ))))
                .andExpect(status().isOk());
    }

    private long firstIdFromPagedItems(AuthenticatedClient client, String endpoint, String fieldName) throws Exception {
        String response = mockMvc.perform(get(endpoint)
                        .session(client.session())
                        .cookie(client.xsrfCookie()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode items = objectMapper.readTree(response).path("data").path("items");
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isGreaterThan(0);
        return items.get(0).path(fieldName).asLong();
    }

    private long createPaymentIntent(AuthenticatedClient client, String endpoint) throws Exception {
        String response = mockMvc.perform(post(endpoint)
                        .session(client.session())
                        .cookie(client.xsrfCookie())
                        .header("X-CSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("paymentIntentId").asLong();
    }

    private long createLocalPaymentAttempt(AuthenticatedClient client, long paymentIntentId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/payment-intents/{paymentIntentId}/attempts", paymentIntentId)
                        .session(client.session())
                        .cookie(client.xsrfCookie())
                        .header("X-CSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "providerCode", "LOCAL",
                                "methodType", "OTHER"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("paymentAttemptId").asLong();
    }

    private void confirmLocalPaymentAttempt(AuthenticatedClient client, long paymentAttemptId) throws Exception {
        mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm", paymentAttemptId)
                        .session(client.session())
                        .cookie(client.xsrfCookie())
                        .header("X-CSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    private void createSubscription(AuthenticatedClient client,
                                    String channelType,
                                    String endpoint,
                                    String publicKey,
                                    String authSecret) throws Exception {
        mockMvc.perform(post("/api/v1/notification-subscriptions")
                        .session(client.session())
                        .cookie(client.xsrfCookie())
                        .header("X-CSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "channelType", channelType,
                                "endpoint", endpoint,
                                "publicKey", publicKey == null ? "" : publicKey,
                                "authSecret", authSecret == null ? "" : authSecret
                        ))))
                .andExpect(status().isOk());
    }

    private long accountIdBySessionClient(AuthenticatedClient client) throws Exception {
        Object dbSessionId = client.session().getAttribute(SecuritySessionConstants.DB_SESSION_ID_ATTRIBUTE);
        assertThat(dbSessionId).isInstanceOf(Long.class);
        return jdbcTemplate.queryForObject("""
                select account_id
                from session
                where session_id = ?
                """, Long.class, dbSessionId);
    }

    private void assertFeedContains(AuthenticatedClient client, String notificationName) throws Exception {
        String response = mockMvc.perform(get("/api/v1/notifications/me?page=1&size=100")
                        .session(client.session())
                        .cookie(client.xsrfCookie()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> names = objectMapper.readTree(response).path("data").path("items").findValuesAsText("notificationName");
        assertThat(names).contains(notificationName);
    }

    private void assertNotificationAndAuditExist(String eventType, String actionOrNotificationName) {
        Integer notificationCount = jdbcTemplate.queryForObject("""
                select count(*)
                from notification
                where event_type = ?
                  and notification_name = ?
                """, Integer.class, eventType, actionOrNotificationName);
        Integer auditCount = jdbcTemplate.queryForObject("""
                select count(*)
                from audit_record
                where event_type = ?
                  and action = ?
                """, Integer.class, eventType, actionOrNotificationName);

        assertThat(notificationCount).isGreaterThan(0);
        assertThat(auditCount).isGreaterThan(0);
    }

}

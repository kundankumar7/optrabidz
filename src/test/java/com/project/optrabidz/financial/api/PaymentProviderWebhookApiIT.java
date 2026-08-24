package com.project.optrabidz.financial.api;

import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentProviderWebhookApiIT extends ApiIntegrationTestSupport {
    private static final String SECRET =
            "test-only-upi-webhook-secret-material-001";
    private static final String REQUEST_ID = "webhook-security-request-123";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void unauthenticatedWebhookFailuresShareOneDisclosureSafeContract()
            throws Exception {
        String validBody = validBody();
        String currentTimestamp = String.valueOf(Instant.now().getEpochSecond());
        String staleTimestamp = String.valueOf(
                Instant.now().minusSeconds(600).getEpochSecond()
        );
        String zeroSignature = "sha256=" + "0".repeat(64);

        List<MockHttpServletRequestBuilder> requests = List.of(
                webhook("UNLISTED", validBody)
                        .header("X-Payment-Timestamp", currentTimestamp)
                        .header("X-Payment-Signature", zeroSignature),
                webhook("UPI", validBody)
                        .header("X-Payment-Timestamp", currentTimestamp),
                webhook("UPI", validBody)
                        .header("X-Payment-Timestamp", currentTimestamp)
                        .header("X-Payment-Signature", "sha256=not-hex"),
                webhook("UPI", validBody)
                        .header("X-Payment-Timestamp", staleTimestamp)
                        .header("X-Payment-Signature", zeroSignature),
                webhook("UPI", validBody)
                        .header("X-Payment-Timestamp", currentTimestamp)
                        .header(
                                "X-Payment-Signature",
                                signature(currentTimestamp, validBody + " ")
                        ),
                webhook("UPI", "{malformed-json")
                        .header("X-Payment-Timestamp", currentTimestamp)
                        .header("X-Payment-Signature", zeroSignature)
        );

        Long attemptsBefore = paymentAttemptCount();
        for (MockHttpServletRequestBuilder request : requests) {
            MvcResult result = mockMvc.perform(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(
                            MediaType.APPLICATION_PROBLEM_JSON
                    ))
                    .andExpect(jsonPath("$.code").value(
                            "PAYMENT_WEBHOOK_REJECTED"
                    ))
                    .andExpect(jsonPath("$.detail").value(
                            "The webhook request was rejected"
                    ))
                    .andExpect(jsonPath("$.success").doesNotExist())
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andReturn();

            assertDisclosureSafe(result);
        }
        assertThat(paymentAttemptCount()).isEqualTo(attemptsBefore);
    }

    @Test
    void authenticatedInvalidPayloadsUseThePayloadInvalidContract()
            throws Exception {
        List<String> invalidBodies = List.of(
                """
                {"eventType":"PAYMENT_CONFIRMED","eventType":"PAYMENT_FAILED",
                 "paymentAttemptId":1001,"providerPaymentId":"UPI-1001",
                 "providerEventId":"evt-1001"}
                """,
                """
                {"eventType":"PAYMENT_CONFIRMED","paymentAttemptId":1001,
                 "providerPaymentId":"UPI-1001","providerEventId":"evt-1001",
                 "unexpected":"must-not-be-accepted"}
                """,
                """
                {"eventType":"PAYMENT_CONFIRMED","paymentAttemptId":"1001",
                 "providerPaymentId":"UPI-1001","providerEventId":"evt-1001"}
                """
        );

        Long attemptsBefore = paymentAttemptCount();
        for (String body : invalidBodies) {
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            MvcResult result = mockMvc.perform(
                            webhook("UPI", body)
                                    .header("X-Payment-Timestamp", timestamp)
                                    .header(
                                            "X-Payment-Signature",
                                            signature(timestamp, body)
                                    )
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(
                            MediaType.APPLICATION_PROBLEM_JSON
                    ))
                    .andExpect(jsonPath("$.code").value(
                            "PAYMENT_WEBHOOK_PAYLOAD_INVALID"
                    ))
                    .andExpect(jsonPath("$.detail").value(
                            "The webhook payload is invalid"
                    ))
                    .andExpect(jsonPath("$.success").doesNotExist())
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andReturn();

            assertDisclosureSafe(result);
        }
        assertThat(paymentAttemptCount()).isEqualTo(attemptsBefore);
    }

    private MockHttpServletRequestBuilder webhook(String providerCode,
                                                  String body) {
        return post("/api/v1/payment-providers/{providerCode}/webhooks", providerCode)
                .header("X-Request-Id", REQUEST_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String signature(String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        byte[] digest = mac.doFinal(
                (timestamp + "." + body).getBytes(StandardCharsets.UTF_8)
        );
        return "sha256=" + HexFormat.of().formatHex(digest);
    }

    private void assertDisclosureSafe(MvcResult result) throws Exception {
        String response = result.getResponse().getContentAsString();
        assertThat(response)
                .doesNotContain(SECRET)
                .doesNotContain("not-hex")
                .doesNotContain("must-not-be-accepted")
                .doesNotContain("SIGNATURE_INVALID")
                .doesNotContain("TIMESTAMP_STALE")
                .doesNotContain("PROVIDER_UNAVAILABLE")
                .doesNotContain("PaymentWebhook")
                .doesNotContain("Exception")
                .doesNotContain("stackTrace");
    }

    private Long paymentAttemptCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from payment_attempt",
                Long.class
        );
    }

    private String validBody() {
        return """
                {"eventType":"PAYMENT_CONFIRMED","paymentAttemptId":1001,
                 "providerPaymentId":"UPI-1001","providerEventId":"evt-1001"}
                """;
    }
}

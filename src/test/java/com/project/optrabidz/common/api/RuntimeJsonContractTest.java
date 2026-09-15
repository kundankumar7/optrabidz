package com.project.optrabidz.common.api;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.project.optrabidz.financial.application.dto.response.PaymentIntentResponse;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;
import com.project.optrabidz.financial.domain.model.PaymentState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class RuntimeJsonContractTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void preservesPublicDateEnumNullNumericAndPropertyOrderBehavior()
            throws Exception {
        PaymentIntentResponse response = new PaymentIntentResponse(
                42L,
                PaymentPurpose.SETTLEMENT,
                11L,
                null,
                7L,
                9L,
                new BigDecimal("1234.50"),
                "INR",
                PaymentState.PAYMENT_PENDING,
                Instant.parse("2026-08-25T00:00:00Z"),
                Instant.parse("2026-08-25T00:15:00Z"),
                null,
                null,
                null,
                null,
                null,
                null
        );

        String json = objectMapper.writeValueAsString(response);
        JsonNode body = objectMapper.readTree(json);

        assertThat(body.propertyNames())
                .containsExactly(
                        "paymentIntentId",
                        "paymentPurpose",
                        "settlementId",
                        "repaymentInstallmentId",
                        "payerAccountId",
                        "payeeAccountId",
                        "amount",
                        "currencyCode",
                        "paymentState",
                        "createdAt",
                        "expiresAt",
                        "confirmedAt",
                        "failedAt",
                        "expiredAt",
                        "cancelledAt",
                        "failureCode",
                        "failureMessage"
                );
        assertThat(body.path("paymentPurpose").asText())
                .isEqualTo("SETTLEMENT");
        assertThat(body.path("paymentState").asText())
                .isEqualTo("PAYMENT_PENDING");
        assertThat(body.path("createdAt").asText())
                .isEqualTo("2026-08-25T00:00:00Z");
        assertThat(body.path("expiresAt").asText())
                .isEqualTo("2026-08-25T00:15:00Z");
        assertThat(body.path("amount").isNumber()).isTrue();
        assertThat(body.path("amount").decimalValue())
                .isEqualByComparingTo("1234.50");
        assertThat(json).contains("\"amount\":1234.50");
        assertThat(body.path("repaymentInstallmentId").isNull()).isTrue();
        assertThat(body.path("confirmedAt").isNull()).isTrue();
        assertThat(body.path("failureCode").isNull()).isTrue();
    }
}

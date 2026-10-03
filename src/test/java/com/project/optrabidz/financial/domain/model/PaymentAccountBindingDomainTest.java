package com.project.optrabidz.financial.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentAccountBindingDomainTest {
    private static final Instant CREATED_AT = Instant.parse("2026-10-01T09:00:00Z");

    @Test
    void pendingBindingBecomesReadyOnlyAfterVerification() {
        PaymentAccountBinding binding = pendingBinding();

        assertThat(binding.getBindingStatus()).isEqualTo(PaymentAccountBindingStatus.PENDING_VERIFICATION);
        assertThat(binding.isReady()).isFalse();

        binding.verify(CREATED_AT.plusSeconds(30));

        assertThat(binding.getBindingStatus()).isEqualTo(PaymentAccountBindingStatus.VERIFIED);
        assertThat(binding.getVerifiedAt()).isEqualTo(CREATED_AT.plusSeconds(30));
        assertThat(binding.isReady()).isTrue();
    }

    @Test
    void deactivationIsTerminalAndRetainsVerificationHistory() {
        PaymentAccountBinding binding = pendingBinding();
        binding.verify(CREATED_AT.plusSeconds(30));

        binding.deactivate(CREATED_AT.plusSeconds(60));

        assertThat(binding.getBindingStatus()).isEqualTo(PaymentAccountBindingStatus.DEACTIVATED);
        assertThat(binding.getVerifiedAt()).isEqualTo(CREATED_AT.plusSeconds(30));
        assertThat(binding.getDeactivatedAt()).isEqualTo(CREATED_AT.plusSeconds(60));
        assertThat(binding.isReady()).isFalse();
        assertThatThrownBy(() -> binding.verify(CREATED_AT.plusSeconds(90)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static PaymentAccountBinding pendingBinding() {
        return PaymentAccountBinding.createPending(
                41L,
                "DEMO",
                "binding-command-1",
                null,
                "demo-recipient-41",
                "1234",
                "Demonstration Bank",
                CREATED_AT
        );
    }
}

package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEnvelope;
import com.project.optrabidz.financial.application.dto.request.DemoPaymentOutcomeRequest;
import com.project.optrabidz.financial.application.dto.response.DemoCheckoutResponse;
import com.project.optrabidz.financial.application.dto.response.DemoPaymentOutcomeResponse;
import com.project.optrabidz.financial.application.exception.DemoPaymentOutcomeIdempotencyConflictException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookReplayCollisionException;
import com.project.optrabidz.financial.application.exception.ReceivingAccountNotReadyException;
import com.project.optrabidz.financial.domain.model.DemoPaymentOutcome;
import com.project.optrabidz.financial.domain.model.PaymentAccountBinding;
import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.model.PaymentAttemptState;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.repository.PaymentAccountBindingRepository;
import com.project.optrabidz.financial.domain.repository.PaymentAttemptRepository;
import com.project.optrabidz.financial.domain.repository.PaymentIntentRepository;
import com.project.optrabidz.financial.infrastructure.provider.webhook.PaymentWebhookHmac;
import com.project.optrabidz.financial.infrastructure.provider.webhook.PaymentWebhookProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoPaymentServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");
    private static final String SECRET = "demo-webhook-secret-material-0001";

    @Mock PaymentAttemptRepository attemptRepository;
    @Mock PaymentIntentRepository intentRepository;
    @Mock PaymentAccountBindingRepository bindingRepository;
    @Mock PaymentProviderWebhookIngressService ingressService;

    private DemoPaymentService service;

    @BeforeEach
    void setUp() {
        service = new DemoPaymentService(
                attemptRepository,
                intentRepository,
                bindingRepository,
                ingressService,
                new PaymentWebhookHmac(),
                properties(),
                JsonMapper.builder().build(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void checkoutReturnsOnlyPersistedSafeDemonstrationFields() {
        when(attemptRepository.findByIdForPayer(10L, 40L)).thenReturn(Optional.of(attempt(PaymentAttemptState.INITIATED)));
        when(intentRepository.findById(20L)).thenReturn(Optional.of(intent(PaymentState.PAYMENT_PENDING)));

        DemoCheckoutResponse response = service.getCheckout(40L, 10L);

        assertThat(response.paymentAttemptId()).isEqualTo(10L);
        assertThat(response.paymentIntentId()).isEqualTo(20L);
        assertThat(response.providerCode()).isEqualTo("DEMO");
        assertThat(response.amount()).isEqualByComparingTo("100.00");
        assertThat(response.environmentLabel()).isEqualTo("DEMONSTRATION");
        assertThat(response.warning()).isEqualTo(
                "Demonstration payment environment. No real financial transaction will occur.");
    }

    @Test
    void successRevalidatesBindingSignsCanonicalPayloadAndUsesExistingIngress() {
        when(attemptRepository.findByIdForPayer(10L, 40L))
                .thenReturn(Optional.of(attempt(PaymentAttemptState.INITIATED)),
                        Optional.of(attempt(PaymentAttemptState.CONFIRMED)));
        when(intentRepository.findById(20L))
                .thenReturn(Optional.of(intent(PaymentState.PAYMENT_PENDING)),
                        Optional.of(intent(PaymentState.PAYMENT_CONFIRMED)));
        when(bindingRepository.findVerifiedByAccountIdAndProviderCode(50L, "DEMO"))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(PaymentAccountBinding.class)));

        DemoPaymentOutcomeResponse response = service.processOutcome(
                40L, 10L, new DemoPaymentOutcomeRequest(DemoPaymentOutcome.SUCCESS, "outcome-key"));

        ArgumentCaptor<PaymentProviderWebhookEnvelope> envelope =
                ArgumentCaptor.forClass(PaymentProviderWebhookEnvelope.class);
        verify(ingressService).handle(envelope.capture());
        assertThat(envelope.getValue().signature()).isEqualTo(
                new PaymentWebhookHmac().sign(
                        envelope.getValue().timestamp(), envelope.getValue().rawBody(), SECRET));
        assertThat(new String(envelope.getValue().rawBody()))
                .contains("\"eventType\":\"PAYMENT_CONFIRMED\"")
                .contains("\"paymentAttemptId\":10")
                .contains("\"providerPaymentId\":\"DEMO-PAYMENT-10\"");
        assertThat(response.attemptState()).isEqualTo(PaymentAttemptState.CONFIRMED);
        assertThat(response.paymentState()).isEqualTo(PaymentState.PAYMENT_CONFIRMED);
        assertThat(response.processedAt()).isEqualTo(NOW);
    }

    @Test
    void successWithoutCurrentBindingLeavesCollectionUntouched() {
        when(attemptRepository.findByIdForPayer(10L, 40L)).thenReturn(Optional.of(attempt(PaymentAttemptState.INITIATED)));
        when(intentRepository.findById(20L)).thenReturn(Optional.of(intent(PaymentState.PAYMENT_PENDING)));
        when(bindingRepository.findVerifiedByAccountIdAndProviderCode(50L, "DEMO"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processOutcome(
                40L, 10L, new DemoPaymentOutcomeRequest(DemoPaymentOutcome.SUCCESS, "outcome-key")))
                .isInstanceOf(ReceivingAccountNotReadyException.class);
        verify(ingressService, never()).handle(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void successfulReplayDoesNotDependOnBindingAfterOriginalSuccess() {
        when(attemptRepository.findByIdForPayer(10L, 40L))
                .thenReturn(Optional.of(attempt(PaymentAttemptState.CONFIRMED)),
                        Optional.of(attempt(PaymentAttemptState.CONFIRMED)));
        when(intentRepository.findById(20L))
                .thenReturn(Optional.of(intent(PaymentState.PAYMENT_CONFIRMED)),
                        Optional.of(intent(PaymentState.PAYMENT_CONFIRMED)));

        DemoPaymentOutcomeResponse response = service.processOutcome(
                40L, 10L, new DemoPaymentOutcomeRequest(DemoPaymentOutcome.SUCCESS, "outcome-key"));

        verify(bindingRepository, never())
                .findVerifiedByAccountIdAndProviderCode(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        verify(ingressService).handle(org.mockito.ArgumentMatchers.any());
        assertThat(response.attemptState()).isEqualTo(PaymentAttemptState.CONFIRMED);
        assertThat(response.paymentState()).isEqualTo(PaymentState.PAYMENT_CONFIRMED);
    }

    @Test
    void ingressReplayCollisionBecomesDemoOutcomeConflict() {
        when(attemptRepository.findByIdForPayer(10L, 40L)).thenReturn(Optional.of(attempt(PaymentAttemptState.INITIATED)));
        when(intentRepository.findById(20L)).thenReturn(Optional.of(intent(PaymentState.PAYMENT_PENDING)));
        doThrow(new PaymentWebhookReplayCollisionException()).when(ingressService)
                .handle(org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> service.processOutcome(
                40L, 10L, new DemoPaymentOutcomeRequest(DemoPaymentOutcome.FAILURE, "reused-key")))
                .isInstanceOf(DemoPaymentOutcomeIdempotencyConflictException.class);
    }

    private static PaymentAttempt attempt(PaymentAttemptState state) {
        return PaymentAttempt.builder()
                .paymentAttemptId(10L).paymentIntentId(20L).providerCode("DEMO")
                .methodType(PaymentMethodType.UPI).attemptState(state).createdAt(NOW.minusSeconds(30))
                .initiatedAt(NOW.minusSeconds(20))
                .confirmedAt(state == PaymentAttemptState.CONFIRMED ? NOW : null)
                .build();
    }

    private static PaymentIntent intent(PaymentState state) {
        return PaymentIntent.builder()
                .paymentIntentId(20L).paymentPurpose(com.project.optrabidz.financial.domain.model.PaymentPurpose.SETTLEMENT)
                .settlementId(30L).payerAccountId(40L).payeeAccountId(50L)
                .amount(new BigDecimal("100.00")).currencyCode("INR")
                .paymentState(state).idempotencyKey("intent-key")
                .createdAt(NOW.minusSeconds(60)).expiresAt(NOW.plusSeconds(600))
                .confirmedAt(state == PaymentState.PAYMENT_CONFIRMED ? NOW : null)
                .build();
    }

    private static PaymentWebhookProperties properties() {
        PaymentWebhookProperties.ProviderConfiguration provider = new PaymentWebhookProperties.ProviderConfiguration();
        provider.setEnabled(true);
        provider.setActiveSecret(SECRET);
        PaymentWebhookProperties properties = new PaymentWebhookProperties();
        properties.setTimestampTolerance(Duration.ofMinutes(5));
        properties.setProviders(Map.of("DEMO", provider));
        return properties;
    }
}

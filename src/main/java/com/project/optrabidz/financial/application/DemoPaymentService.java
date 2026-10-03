package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEnvelope;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import com.project.optrabidz.financial.application.dto.request.DemoPaymentOutcomeRequest;
import com.project.optrabidz.financial.application.dto.request.PaymentProviderWebhookRequest;
import com.project.optrabidz.financial.application.dto.response.DemoCheckoutResponse;
import com.project.optrabidz.financial.application.dto.response.DemoPaymentOutcomeResponse;
import com.project.optrabidz.financial.application.exception.DemoPaymentOutcomeIdempotencyConflictException;
import com.project.optrabidz.financial.application.exception.PaymentAttemptNotFoundException;
import com.project.optrabidz.financial.application.exception.PaymentProviderMismatchException;
import com.project.optrabidz.financial.application.exception.PaymentStateConflictException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookReplayCollisionException;
import com.project.optrabidz.financial.application.exception.ReceivingAccountNotReadyException;
import com.project.optrabidz.financial.domain.model.DemoPaymentOutcome;
import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.model.PaymentAttemptState;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.repository.PaymentAccountBindingRepository;
import com.project.optrabidz.financial.domain.repository.PaymentAttemptRepository;
import com.project.optrabidz.financial.domain.repository.PaymentIntentRepository;
import com.project.optrabidz.financial.infrastructure.provider.webhook.PaymentWebhookHmac;
import com.project.optrabidz.financial.infrastructure.provider.webhook.PaymentWebhookProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;

@Service
@Profile("demo")
@ConditionalOnProperty(name = "optrabidz.financial.demo-provider.enabled", havingValue = "true")
public class DemoPaymentService {
    private static final String PROVIDER_CODE = "DEMO";
    private static final String ENVIRONMENT_LABEL = "DEMONSTRATION";
    private static final String WARNING =
            "Demonstration payment environment. No real financial transaction will occur.";

    private final PaymentAttemptRepository attemptRepository;
    private final PaymentIntentRepository intentRepository;
    private final PaymentAccountBindingRepository bindingRepository;
    private final PaymentProviderWebhookIngressService ingressService;
    private final PaymentWebhookHmac hmac;
    private final PaymentWebhookProperties webhookProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public DemoPaymentService(PaymentAttemptRepository attemptRepository,
                              PaymentIntentRepository intentRepository,
                              PaymentAccountBindingRepository bindingRepository,
                              PaymentProviderWebhookIngressService ingressService,
                              PaymentWebhookHmac hmac,
                              PaymentWebhookProperties webhookProperties,
                              ObjectMapper objectMapper) {
        this(attemptRepository, intentRepository, bindingRepository, ingressService,
                hmac, webhookProperties, objectMapper, Clock.systemUTC());
    }

    DemoPaymentService(PaymentAttemptRepository attemptRepository,
                       PaymentIntentRepository intentRepository,
                       PaymentAccountBindingRepository bindingRepository,
                       PaymentProviderWebhookIngressService ingressService,
                       PaymentWebhookHmac hmac,
                       PaymentWebhookProperties webhookProperties,
                       ObjectMapper objectMapper,
                       Clock clock) {
        this.attemptRepository = attemptRepository;
        this.intentRepository = intentRepository;
        this.bindingRepository = bindingRepository;
        this.ingressService = ingressService;
        this.hmac = hmac;
        this.webhookProperties = webhookProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public DemoCheckoutResponse getCheckout(Long accountId, Long paymentAttemptId) {
        PaymentAttempt attempt = ownedDemoAttempt(accountId, paymentAttemptId);
        ensureActive(attempt);
        PaymentIntent intent = paymentIntent(attempt.getPaymentIntentId());
        return new DemoCheckoutResponse(
                attempt.getPaymentAttemptId(), intent.getPaymentIntentId(), PROVIDER_CODE,
                intent.getAmount(), intent.getCurrencyCode(), intent.getPaymentPurpose(),
                attempt.getMethodType(), attempt.getAttemptState(), ENVIRONMENT_LABEL, WARNING);
    }

    public DemoPaymentOutcomeResponse processOutcome(Long accountId,
                                                     Long paymentAttemptId,
                                                     DemoPaymentOutcomeRequest request) {
        PaymentAttempt attempt = ownedDemoAttempt(accountId, paymentAttemptId);
        PaymentIntent intent = paymentIntent(attempt.getPaymentIntentId());
        if (request.outcome() == DemoPaymentOutcome.SUCCESS && isActive(attempt)) {
            requireVerifiedPayee(intent.getPayeeAccountId());
        }

        PaymentProviderWebhookRequest payload = payload(attempt, request);
        byte[] body = serialize(payload);
        String timestamp = Long.toString(clock.instant().getEpochSecond());
        String secret = webhookProperties.enabledProvider(PROVIDER_CODE)
                .orElseThrow(() -> new IllegalStateException("DEMO webhook provider is unavailable"))
                .getActiveSecret();
        try {
            ingressService.handle(new PaymentProviderWebhookEnvelope(
                    PROVIDER_CODE, body, timestamp, hmac.sign(timestamp, body, secret)));
        } catch (PaymentWebhookReplayCollisionException exception) {
            throw new DemoPaymentOutcomeIdempotencyConflictException();
        }

        PaymentAttempt processedAttempt = ownedDemoAttempt(accountId, paymentAttemptId);
        PaymentIntent processedIntent = paymentIntent(processedAttempt.getPaymentIntentId());
        return new DemoPaymentOutcomeResponse(
                processedAttempt.getPaymentAttemptId(), processedIntent.getPaymentIntentId(),
                processedAttempt.getAttemptState(), processedIntent.getPaymentState(),
                processedAt(processedAttempt));
    }

    private PaymentProviderWebhookRequest payload(PaymentAttempt attempt,
                                                  DemoPaymentOutcomeRequest request) {
        String eventId = sha256(attempt.getPaymentAttemptId() + ":" + request.idempotencyKey());
        return switch (request.outcome()) {
            case SUCCESS -> new PaymentProviderWebhookRequest(
                    PaymentProviderWebhookEventType.PAYMENT_CONFIRMED,
                    attempt.getPaymentAttemptId(),
                    "DEMO-PAYMENT-" + attempt.getPaymentAttemptId(),
                    null, null, eventId);
            case FAILURE -> new PaymentProviderWebhookRequest(
                    PaymentProviderWebhookEventType.PAYMENT_FAILED,
                    attempt.getPaymentAttemptId(), null,
                    "DEMO_DECLINED", "Demonstration payment was declined", eventId);
            case CANCELLATION -> new PaymentProviderWebhookRequest(
                    PaymentProviderWebhookEventType.PAYMENT_CANCELLED,
                    attempt.getPaymentAttemptId(), null, null, null, eventId);
        };
    }

    private byte[] serialize(PaymentProviderWebhookRequest payload) {
        try {
            return objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Demo payment outcome could not be serialized", exception);
        }
    }

    private PaymentAttempt ownedDemoAttempt(Long accountId, Long paymentAttemptId) {
        PaymentAttempt attempt = attemptRepository.findByIdForPayer(paymentAttemptId, accountId)
                .orElseThrow(() -> new PaymentAttemptNotFoundException(
                        "Payment attempt unavailable for payer lookup"));
        if (!PROVIDER_CODE.equalsIgnoreCase(attempt.getProviderCode())) {
            throw new PaymentProviderMismatchException("Demo endpoint requires a DEMO payment attempt");
        }
        return attempt;
    }

    private PaymentIntent paymentIntent(Long paymentIntentId) {
        return intentRepository.findById(paymentIntentId)
                .orElseThrow(() -> new IllegalStateException("Payment intent is missing for payment attempt"));
    }

    private void ensureActive(PaymentAttempt attempt) {
        if (!isActive(attempt)) {
            throw new PaymentStateConflictException("Demo checkout requires an active payment attempt");
        }
    }

    private boolean isActive(PaymentAttempt attempt) {
        return attempt.getAttemptState() == PaymentAttemptState.CREATED
                || attempt.getAttemptState() == PaymentAttemptState.INITIATED
                || attempt.getAttemptState() == PaymentAttemptState.REQUIRES_ACTION;
    }

    private void requireVerifiedPayee(Long payeeAccountId) {
        if (bindingRepository.findVerifiedByAccountIdAndProviderCode(payeeAccountId, PROVIDER_CODE).isEmpty()) {
            throw new ReceivingAccountNotReadyException(
                    "Payee account " + payeeAccountId + " has no verified DEMO receiving binding");
        }
    }

    private Instant processedAt(PaymentAttempt attempt) {
        return switch (attempt.getAttemptState()) {
            case CONFIRMED -> attempt.getConfirmedAt();
            case FAILED -> attempt.getFailedAt();
            case CANCELLED -> attempt.getCancelledAt();
            default -> throw new PaymentStateConflictException(
                    "Demo outcome did not produce a terminal payment attempt");
        };
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

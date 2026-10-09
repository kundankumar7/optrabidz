package com.project.optrabidz.financial.application;

import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.financial.application.dto.request.CreatePaymentAccountBindingRequest;
import com.project.optrabidz.financial.application.dto.request.ReplacePaymentAccountBindingRequest;
import com.project.optrabidz.financial.application.dto.response.PaymentAccountBindingResponse;
import com.project.optrabidz.financial.application.dto.response.PaymentAccountBindingReadinessResponse;
import com.project.optrabidz.financial.application.event.PaymentAccountBindingCreatedEvent;
import com.project.optrabidz.financial.application.event.PaymentAccountBindingDeactivatedEvent;
import com.project.optrabidz.financial.application.event.PaymentAccountBindingReplacedEvent;
import com.project.optrabidz.financial.application.event.PaymentAccountBindingVerifiedEvent;
import com.project.optrabidz.financial.application.exception.FinancialOperationNotAllowedException;
import com.project.optrabidz.financial.application.exception.PaymentAccountBindingAlreadyExistsException;
import com.project.optrabidz.financial.application.exception.PaymentAccountBindingIdempotencyConflictException;
import com.project.optrabidz.financial.application.exception.PaymentAccountBindingNotFoundException;
import com.project.optrabidz.financial.application.exception.PaymentAccountBindingStateConflictException;
import com.project.optrabidz.financial.domain.model.PaymentAccountBinding;
import com.project.optrabidz.financial.domain.repository.PaymentAccountBindingRepository;
import com.project.optrabidz.identity.domain.model.RoleType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Profile("demo")
@ConditionalOnProperty(name = "optrabidz.financial.demo-provider.enabled", havingValue = "true")
public class PaymentAccountBindingService {
    private static final String DEMO_PROVIDER = "DEMO";

    private final PaymentAccountBindingRepository repository;
    private final EventPublisher eventPublisher;

    public PaymentAccountBindingService(PaymentAccountBindingRepository repository,
                                        EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PaymentAccountBindingResponse create(Long accountId,
                                                RoleType roleType,
                                                CreatePaymentAccountBindingRequest request) {
        requireParticipant(roleType);
        lockAccount(accountId);
        PaymentAccountBinding replay = repository
                .findByAccountIdAndProviderCodeAndCommandIdempotencyKey(
                        accountId,
                        DEMO_PROVIDER,
                        request.idempotencyKey()
                )
                .orElse(null);
        if (replay != null) {
            if (replay.getReplacesBindingId() != null) {
                throw idempotencyConflict(accountId, request.idempotencyKey(), null,
                        replay.getReplacesBindingId());
            }
            return toResponse(replay);
        }
        if (repository.findByAccountIdAndProviderCodeAndActive(accountId, DEMO_PROVIDER).isPresent()) {
            throw new PaymentAccountBindingAlreadyExistsException(
                    "Active binding already exists for accountId=" + accountId
            );
        }

        Instant now = Instant.now();
        PaymentAccountBinding saved = repository.save(newPendingBinding(
                accountId, request.idempotencyKey(), null, now));
        eventPublisher.publish(new PaymentAccountBindingCreatedEvent(
                saved.getPaymentAccountBindingId(),
                accountId,
                roleType,
                now
        ));
        return toResponse(saved);
    }

    @Transactional
    public PaymentAccountBindingResponse replace(Long accountId,
                                                 RoleType roleType,
                                                 Long bindingId,
                                                 ReplacePaymentAccountBindingRequest request) {
        requireParticipant(roleType);
        lockAccount(accountId);
        PaymentAccountBinding current = ownedBinding(accountId, bindingId);
        PaymentAccountBinding replay = repository
                .findByAccountIdAndProviderCodeAndCommandIdempotencyKey(
                        accountId,
                        DEMO_PROVIDER,
                        request.idempotencyKey()
                )
                .orElse(null);
        if (replay != null) {
            if (!bindingId.equals(replay.getReplacesBindingId())) {
                throw idempotencyConflict(accountId, request.idempotencyKey(), bindingId,
                        replay.getReplacesBindingId());
            }
            return toResponse(replay);
        }

        PaymentAccountBinding active = repository
                .findByAccountIdAndProviderCodeAndActive(accountId, DEMO_PROVIDER)
                .orElseThrow(() -> stateConflict(bindingId, "No active binding exists"));
        if (!bindingId.equals(active.getPaymentAccountBindingId())) {
            throw stateConflict(bindingId, "Requested binding is not the current active binding");
        }

        Instant now = Instant.now();
        try {
            current.deactivate(now);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw stateConflict(bindingId, exception.getMessage());
        }
        repository.save(current);
        PaymentAccountBinding replacement = repository.save(newPendingBinding(
                accountId, request.idempotencyKey(), bindingId, now));
        eventPublisher.publish(new PaymentAccountBindingReplacedEvent(
                bindingId,
                replacement.getPaymentAccountBindingId(),
                accountId,
                roleType,
                now
        ));
        return toResponse(replacement);
    }

    @Transactional(readOnly = true)
    public PaymentAccountBindingReadinessResponse getCurrent(Long accountId, RoleType roleType) {
        requireParticipant(roleType);
        PaymentAccountBindingResponse binding = repository
                .findByAccountIdAndProviderCodeAndActive(accountId, DEMO_PROVIDER)
                .map(this::toResponse)
                .orElse(null);
        return new PaymentAccountBindingReadinessResponse(
                binding != null && binding.ready(),
                binding
        );
    }

    @Transactional(readOnly = true)
    public PaymentAccountBindingResponse getById(Long accountId,
                                                 RoleType roleType,
                                                 Long bindingId) {
        requireParticipant(roleType);
        return toResponse(ownedBinding(accountId, bindingId));
    }

    @Transactional
    public PaymentAccountBindingResponse verify(Long accountId,
                                                RoleType roleType,
                                                Long bindingId) {
        requireParticipant(roleType);
        lockAccount(accountId);
        PaymentAccountBinding binding = ownedBinding(accountId, bindingId);
        Instant now = Instant.now();
        try {
            binding.verify(now);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw stateConflict(bindingId, exception.getMessage());
        }
        PaymentAccountBinding saved = repository.save(binding);
        eventPublisher.publish(new PaymentAccountBindingVerifiedEvent(
                bindingId, accountId, roleType, now));
        return toResponse(saved);
    }

    @Transactional
    public PaymentAccountBindingResponse deactivate(Long accountId,
                                                    RoleType roleType,
                                                    Long bindingId) {
        requireParticipant(roleType);
        lockAccount(accountId);
        PaymentAccountBinding binding = ownedBinding(accountId, bindingId);
        Instant now = Instant.now();
        try {
            binding.deactivate(now);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw stateConflict(bindingId, exception.getMessage());
        }
        PaymentAccountBinding saved = repository.save(binding);
        eventPublisher.publish(new PaymentAccountBindingDeactivatedEvent(
                bindingId, accountId, roleType, now));
        return toResponse(saved);
    }

    private void requireParticipant(RoleType roleType) {
        if (roleType != RoleType.STARTUP && roleType != RoleType.INVESTOR) {
            throw new FinancialOperationNotAllowedException(
                    "Payment account binding requires STARTUP or INVESTOR; actorRole=" + roleType
            );
        }
    }

    private void lockAccount(Long accountId) {
        if (!repository.lockAccountForBindingChange(accountId)) {
            throw new PaymentAccountBindingNotFoundException(
                    "Authenticated account row missing for accountId=" + accountId
            );
        }
    }

    private PaymentAccountBinding ownedBinding(Long accountId, Long bindingId) {
        return repository.findById(bindingId)
                .filter(binding -> accountId.equals(binding.getAccountId()))
                .filter(binding -> DEMO_PROVIDER.equals(binding.getProviderCode()))
                .orElseThrow(() -> new PaymentAccountBindingNotFoundException(
                        "Owned DEMO binding not found: bindingId=" + bindingId
                                + " accountId=" + accountId
                ));
    }

    private PaymentAccountBinding newPendingBinding(Long accountId,
                                                    String idempotencyKey,
                                                    Long replacesBindingId,
                                                    Instant now) {
        String generatedToken = UUID.randomUUID().toString().replace("-", "");
        return PaymentAccountBinding.createPending(
                accountId,
                DEMO_PROVIDER,
                idempotencyKey,
                replacesBindingId,
                "demo-recipient-" + generatedToken,
                generatedToken.substring(generatedToken.length() - 4),
                "Demonstration Bank",
                now
        );
    }

    private PaymentAccountBindingIdempotencyConflictException idempotencyConflict(
            Long accountId,
            String idempotencyKey,
            Long requestedReplacement,
            Long originalReplacement
    ) {
        return new PaymentAccountBindingIdempotencyConflictException(
                "Binding command collision for accountId=" + accountId
                        + " key=" + idempotencyKey
                        + " requestedReplacement=" + requestedReplacement
                        + " originalReplacement=" + originalReplacement
        );
    }

    private PaymentAccountBindingStateConflictException stateConflict(Long bindingId, String reason) {
        return new PaymentAccountBindingStateConflictException(
                "Binding state conflict for bindingId=" + bindingId + ": " + reason
        );
    }

    private PaymentAccountBindingResponse toResponse(PaymentAccountBinding binding) {
        return new PaymentAccountBindingResponse(
                binding.getPaymentAccountBindingId(),
                binding.getProviderCode(),
                binding.getBindingStatus(),
                "•••• " + binding.getMaskedAccountSuffix(),
                binding.getDemoBankLabel(),
                binding.isReady(),
                binding.getCreatedAt(),
                binding.getVerifiedAt(),
                binding.getDeactivatedAt()
        );
    }
}

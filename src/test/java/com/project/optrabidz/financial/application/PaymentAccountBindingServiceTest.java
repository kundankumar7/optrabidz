package com.project.optrabidz.financial.application;

import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.financial.application.dto.request.CreatePaymentAccountBindingRequest;
import com.project.optrabidz.financial.application.dto.request.ReplacePaymentAccountBindingRequest;
import com.project.optrabidz.financial.application.dto.response.PaymentAccountBindingResponse;
import com.project.optrabidz.financial.application.dto.response.PaymentAccountBindingReadinessResponse;
import com.project.optrabidz.financial.application.exception.PaymentAccountBindingAlreadyExistsException;
import com.project.optrabidz.financial.application.exception.PaymentAccountBindingIdempotencyConflictException;
import com.project.optrabidz.financial.application.exception.PaymentAccountBindingNotFoundException;
import com.project.optrabidz.financial.domain.model.PaymentAccountBinding;
import com.project.optrabidz.financial.domain.model.PaymentAccountBindingStatus;
import com.project.optrabidz.financial.domain.repository.PaymentAccountBindingRepository;
import com.project.optrabidz.identity.domain.model.RoleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentAccountBindingServiceTest {
    private static final Long ACCOUNT_ID = 41L;
    private static final Long ORIGINAL_BINDING_ID = 101L;
    private static final Long REPLACEMENT_BINDING_ID = 102L;
    private static final String REPLACEMENT_KEY = "replace-command-1";
    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");

    @Mock
    private PaymentAccountBindingRepository repository;

    @Mock
    private EventPublisher eventPublisher;

    @Test
    void createRejectsANewCommandWhenAnActiveBindingAlreadyExists() {
        PaymentAccountBinding active = binding(
                ORIGINAL_BINDING_ID,
                "original-command",
                null,
                PaymentAccountBindingStatus.VERIFIED
        );
        when(repository.lockAccountForBindingChange(ACCOUNT_ID)).thenReturn(true);
        when(repository.findByAccountIdAndProviderCodeAndCommandIdempotencyKey(
                ACCOUNT_ID, "DEMO", "new-command"
        )).thenReturn(Optional.empty());
        when(repository.findByAccountIdAndProviderCodeAndActive(ACCOUNT_ID, "DEMO"))
                .thenReturn(Optional.of(active));

        PaymentAccountBindingService service = service();

        assertThatThrownBy(() -> service.create(
                ACCOUNT_ID,
                RoleType.STARTUP,
                new CreatePaymentAccountBindingRequest("new-command")
        )).isInstanceOf(PaymentAccountBindingAlreadyExistsException.class);

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void createRejectsAKeyPreviouslyUsedForReplacement() {
        PaymentAccountBinding replacement = binding(
                REPLACEMENT_BINDING_ID,
                REPLACEMENT_KEY,
                ORIGINAL_BINDING_ID,
                PaymentAccountBindingStatus.PENDING_VERIFICATION
        );
        when(repository.lockAccountForBindingChange(ACCOUNT_ID)).thenReturn(true);
        when(repository.findByAccountIdAndProviderCodeAndCommandIdempotencyKey(
                ACCOUNT_ID, "DEMO", REPLACEMENT_KEY
        )).thenReturn(Optional.of(replacement));

        PaymentAccountBindingService service = service();

        assertThatThrownBy(() -> service.create(
                ACCOUNT_ID,
                RoleType.INVESTOR,
                new CreatePaymentAccountBindingRequest(REPLACEMENT_KEY)
        )).isInstanceOf(PaymentAccountBindingIdempotencyConflictException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void replacementAtomicallyDeactivatesCurrentBindingAndCreatesPendingReplacement() {
        PaymentAccountBinding original = binding(
                ORIGINAL_BINDING_ID,
                "original-command",
                null,
                PaymentAccountBindingStatus.VERIFIED
        );
        when(repository.lockAccountForBindingChange(ACCOUNT_ID)).thenReturn(true);
        when(repository.findByAccountIdAndProviderCodeAndCommandIdempotencyKey(
                ACCOUNT_ID, "DEMO", REPLACEMENT_KEY
        )).thenReturn(Optional.empty());
        when(repository.findById(ORIGINAL_BINDING_ID)).thenReturn(Optional.of(original));
        when(repository.findByAccountIdAndProviderCodeAndActive(ACCOUNT_ID, "DEMO"))
                .thenReturn(Optional.of(original));
        when(repository.save(any(PaymentAccountBinding.class))).thenAnswer(invocation -> {
            PaymentAccountBinding candidate = invocation.getArgument(0);
            if (candidate.getBindingStatus() == PaymentAccountBindingStatus.DEACTIVATED) {
                return candidate;
            }
            return binding(
                    REPLACEMENT_BINDING_ID,
                    candidate.getCommandIdempotencyKey(),
                    candidate.getReplacesBindingId(),
                    candidate.getBindingStatus()
            );
        });

        PaymentAccountBindingResponse response = service().replace(
                ACCOUNT_ID,
                RoleType.STARTUP,
                ORIGINAL_BINDING_ID,
                new ReplacePaymentAccountBindingRequest(REPLACEMENT_KEY)
        );

        assertThat(original.getBindingStatus()).isEqualTo(PaymentAccountBindingStatus.DEACTIVATED);
        assertThat(response.paymentAccountBindingId()).isEqualTo(REPLACEMENT_BINDING_ID);
        assertThat(response.status()).isEqualTo(PaymentAccountBindingStatus.PENDING_VERIFICATION);
        assertThat(response.ready()).isFalse();
        ArgumentCaptor<PaymentAccountBinding> saved = ArgumentCaptor.forClass(PaymentAccountBinding.class);
        verify(repository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(1).getReplacesBindingId())
                .isEqualTo(ORIGINAL_BINDING_ID);
        verify(eventPublisher).publish(any());
    }

    @Test
    void crossAccountLookupIsReportedAsNotFound() {
        PaymentAccountBinding foreign = PaymentAccountBinding.builder()
                .paymentAccountBindingId(ORIGINAL_BINDING_ID)
                .accountId(999L)
                .providerCode("DEMO")
                .commandIdempotencyKey("foreign-command")
                .externalRecipientReference("demo-recipient-foreign")
                .bindingStatus(PaymentAccountBindingStatus.PENDING_VERIFICATION)
                .maskedAccountSuffix("9876")
                .demoBankLabel("Demonstration Bank")
                .createdAt(NOW.minusSeconds(60))
                .lockVersion(0L)
                .build();
        when(repository.findById(ORIGINAL_BINDING_ID)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service().getById(
                ACCOUNT_ID,
                RoleType.INVESTOR,
                ORIGINAL_BINDING_ID
        )).isInstanceOf(PaymentAccountBindingNotFoundException.class);
    }

    @Test
    void verificationMakesCurrentBindingReadyAndDeactivationRemovesCurrentBinding() {
        PaymentAccountBinding pending = binding(
                ORIGINAL_BINDING_ID,
                "create-command",
                null,
                PaymentAccountBindingStatus.PENDING_VERIFICATION
        );
        when(repository.lockAccountForBindingChange(ACCOUNT_ID)).thenReturn(true);
        when(repository.findById(ORIGINAL_BINDING_ID)).thenReturn(Optional.of(pending));
        when(repository.save(pending)).thenReturn(pending);

        PaymentAccountBindingService service = service();
        PaymentAccountBindingResponse verified = service.verify(
                ACCOUNT_ID,
                RoleType.INVESTOR,
                ORIGINAL_BINDING_ID
        );

        assertThat(verified.status()).isEqualTo(PaymentAccountBindingStatus.VERIFIED);
        assertThat(verified.ready()).isTrue();

        when(repository.findByAccountIdAndProviderCodeAndActive(ACCOUNT_ID, "DEMO"))
                .thenReturn(Optional.of(pending), Optional.empty());
        PaymentAccountBindingReadinessResponse ready = service.getCurrent(
                ACCOUNT_ID,
                RoleType.INVESTOR
        );
        assertThat(ready.ready()).isTrue();
        assertThat(ready.binding()).isEqualTo(verified);

        PaymentAccountBindingResponse deactivated = service.deactivate(
                ACCOUNT_ID,
                RoleType.INVESTOR,
                ORIGINAL_BINDING_ID
        );
        assertThat(deactivated.status()).isEqualTo(PaymentAccountBindingStatus.DEACTIVATED);
        assertThat(deactivated.ready()).isFalse();

        PaymentAccountBindingReadinessResponse absent = service.getCurrent(
                ACCOUNT_ID,
                RoleType.INVESTOR
        );
        assertThat(absent.ready()).isFalse();
        assertThat(absent.binding()).isNull();
    }

    @Test
    void replayingCreateReturnsTheOriginalBindingWithoutCreatingAnotherRow() {
        String commandKey = "create-command-1";
        PaymentAccountBinding saved = binding(
                ORIGINAL_BINDING_ID,
                commandKey,
                null,
                PaymentAccountBindingStatus.PENDING_VERIFICATION
        );
        when(repository.lockAccountForBindingChange(ACCOUNT_ID)).thenReturn(true);
        when(repository.findByAccountIdAndProviderCodeAndCommandIdempotencyKey(
                ACCOUNT_ID,
                "DEMO",
                commandKey
        )).thenReturn(Optional.empty(), Optional.of(saved));
        when(repository.findByAccountIdAndProviderCodeAndActive(ACCOUNT_ID, "DEMO"))
                .thenReturn(Optional.empty());
        when(repository.save(any(PaymentAccountBinding.class))).thenReturn(saved);

        PaymentAccountBindingService service = service();
        CreatePaymentAccountBindingRequest request =
                new CreatePaymentAccountBindingRequest(commandKey);

        PaymentAccountBindingResponse first = service.create(
                ACCOUNT_ID,
                RoleType.INVESTOR,
                request
        );
        PaymentAccountBindingResponse replay = service.create(
                ACCOUNT_ID,
                RoleType.INVESTOR,
                request
        );

        assertThat(first).isEqualTo(replay);
        assertThat(first.paymentAccountBindingId()).isEqualTo(ORIGINAL_BINDING_ID);
        assertThat(first.ready()).isFalse();
        verify(repository, times(1)).save(any(PaymentAccountBinding.class));
        verify(eventPublisher, times(1)).publish(any());
    }

    @Test
    void replayingReplacementReturnsOriginalReplacementWithoutDeactivatingIt() {
        PaymentAccountBinding original = binding(
                ORIGINAL_BINDING_ID,
                "original-command",
                null,
                PaymentAccountBindingStatus.VERIFIED
        );
        PaymentAccountBinding replacement = binding(
                REPLACEMENT_BINDING_ID,
                REPLACEMENT_KEY,
                ORIGINAL_BINDING_ID,
                PaymentAccountBindingStatus.PENDING_VERIFICATION
        );
        when(repository.lockAccountForBindingChange(ACCOUNT_ID)).thenReturn(true);
        when(repository.findById(ORIGINAL_BINDING_ID)).thenReturn(Optional.of(original));
        when(repository.findByAccountIdAndProviderCodeAndCommandIdempotencyKey(
                ACCOUNT_ID,
                "DEMO",
                REPLACEMENT_KEY
        )).thenReturn(Optional.of(replacement));

        PaymentAccountBindingService service = service();

        PaymentAccountBindingResponse response = service.replace(
                ACCOUNT_ID,
                RoleType.STARTUP,
                ORIGINAL_BINDING_ID,
                new ReplacePaymentAccountBindingRequest(REPLACEMENT_KEY)
        );

        assertThat(response.paymentAccountBindingId()).isEqualTo(REPLACEMENT_BINDING_ID);
        assertThat(response.status()).isEqualTo(PaymentAccountBindingStatus.PENDING_VERIFICATION);
        assertThat(replacement.getBindingStatus())
                .isEqualTo(PaymentAccountBindingStatus.PENDING_VERIFICATION);
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    private static PaymentAccountBinding binding(Long bindingId,
                                                  String commandKey,
                                                  Long replacesBindingId,
                                                  PaymentAccountBindingStatus status) {
        Instant verifiedAt = status == PaymentAccountBindingStatus.VERIFIED
                ? NOW.minusSeconds(30)
                : null;
        Instant deactivatedAt = status == PaymentAccountBindingStatus.DEACTIVATED
                ? NOW
                : null;
        return PaymentAccountBinding.builder()
                .paymentAccountBindingId(bindingId)
                .accountId(ACCOUNT_ID)
                .providerCode("DEMO")
                .commandIdempotencyKey(commandKey)
                .replacesBindingId(replacesBindingId)
                .externalRecipientReference("demo-recipient-" + bindingId)
                .bindingStatus(status)
                .maskedAccountSuffix("1234")
                .demoBankLabel("Demonstration Bank")
                .createdAt(NOW.minusSeconds(60))
                .verifiedAt(verifiedAt)
                .deactivatedAt(deactivatedAt)
                .lockVersion(0L)
                .build();
    }

    private PaymentAccountBindingService service() {
        return new PaymentAccountBindingService(repository, eventPublisher);
    }
}

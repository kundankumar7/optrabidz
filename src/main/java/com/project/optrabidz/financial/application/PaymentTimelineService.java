package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.dto.response.PaymentTimelineEntryResponse;
import com.project.optrabidz.financial.application.dto.response.PaymentTimelineResponse;
import com.project.optrabidz.financial.application.dto.response.PaymentTimelineStage;
import com.project.optrabidz.financial.application.dto.response.PaymentTimelineStatus;
import com.project.optrabidz.financial.application.exception.PaymentIntentNotFoundException;
import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.model.PayoutTransfer;
import com.project.optrabidz.financial.domain.model.PayoutTransferStatus;
import com.project.optrabidz.financial.domain.model.RepaymentInstallment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
import com.project.optrabidz.financial.domain.model.Settlement;
import com.project.optrabidz.financial.domain.model.SettlementState;
import com.project.optrabidz.financial.domain.repository.PaymentAttemptRepository;
import com.project.optrabidz.financial.domain.repository.PaymentIntentRepository;
import com.project.optrabidz.financial.domain.repository.PayoutTransferRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentInstallmentRepository;
import com.project.optrabidz.financial.domain.repository.SettlementRepository;
import com.project.optrabidz.identity.domain.model.RoleType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentTimelineService {
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PayoutTransferRepository payoutTransferRepository;
    private final SettlementRepository settlementRepository;
    private final RepaymentInstallmentRepository installmentRepository;

    public PaymentTimelineService(PaymentIntentRepository paymentIntentRepository,
                                  PaymentAttemptRepository paymentAttemptRepository,
                                  PayoutTransferRepository payoutTransferRepository,
                                  SettlementRepository settlementRepository,
                                  RepaymentInstallmentRepository installmentRepository) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.payoutTransferRepository = payoutTransferRepository;
        this.settlementRepository = settlementRepository;
        this.installmentRepository = installmentRepository;
    }

    @Transactional(readOnly = true)
    public PaymentTimelineResponse getTimeline(Long accountId,
                                               RoleType roleType,
                                               Long paymentIntentId) {
        PaymentIntent intent = paymentIntentRepository
                .findByIdForParticipant(paymentIntentId, accountId)
                .orElseThrow(() -> new PaymentIntentNotFoundException(
                        "Payment intent not found for timeline"));
        Optional<PaymentAttempt> latestAttempt = paymentAttemptRepository
                .findLatestByPaymentIntentId(paymentIntentId);
        Optional<PayoutTransfer> transfer = payoutTransferRepository
                .findByPaymentIntentId(paymentIntentId);

        List<PaymentTimelineEntryResponse> entries = new ArrayList<>();
        add(entries, PaymentTimelineStage.PAYMENT_INTENT_CREATED,
                PaymentTimelineStatus.COMPLETED,
                "Demonstration payment intent created", intent.getCreatedAt(), null);
        latestAttempt.ifPresent(attempt -> addAttemptEntries(entries, attempt));
        addCollectionEntries(entries, intent);
        addPayoutEntries(entries, intent, transfer);
        entries.sort(Comparator.comparing(PaymentTimelineEntryResponse::occurredAt));
        boolean demonstration = latestAttempt
                .map(attempt -> "DEMO".equalsIgnoreCase(attempt.getProviderCode()))
                .orElseGet(() -> transfer
                        .map(value -> "DEMO".equalsIgnoreCase(value.getProviderCode()))
                        .orElse(false));
        return new PaymentTimelineResponse(paymentIntentId, demonstration, List.copyOf(entries));
    }

    private void addAttemptEntries(List<PaymentTimelineEntryResponse> entries,
                                   PaymentAttempt attempt) {
        add(entries, PaymentTimelineStage.PAYMENT_ATTEMPT_CREATED,
                PaymentTimelineStatus.COMPLETED,
                "Demonstration payment attempt created", attempt.getCreatedAt(), null);
        if ("DEMO".equalsIgnoreCase(attempt.getProviderCode())) {
            add(entries, PaymentTimelineStage.DEMO_CHECKOUT_AVAILABLE,
                    PaymentTimelineStatus.COMPLETED,
                    "Demonstration checkout available",
                    attempt.getInitiatedAt() == null
                            ? attempt.getCreatedAt() : attempt.getInitiatedAt(),
                    null);
        }
    }

    private void addCollectionEntries(List<PaymentTimelineEntryResponse> entries,
                                      PaymentIntent intent) {
        if (intent.getPaymentState() == PaymentState.PAYMENT_CONFIRMED) {
            add(entries, PaymentTimelineStage.COLLECTION_CONFIRMED,
                    PaymentTimelineStatus.COMPLETED,
                    "Demonstration collection confirmed", intent.getConfirmedAt(), null);
            add(entries, PaymentTimelineStage.SIMULATED_ESCROW_CREDIT,
                    PaymentTimelineStatus.COMPLETED,
                    "Simulated escrow credited", intent.getConfirmedAt(), null);
        } else if (intent.getPaymentState() == PaymentState.PAYMENT_FAILED) {
            add(entries, PaymentTimelineStage.COLLECTION_FAILED,
                    PaymentTimelineStatus.FAILED,
                    "Demonstration collection failed", intent.getFailedAt(), null);
        } else if (intent.getPaymentState() == PaymentState.PAYMENT_CANCELLED) {
            add(entries, PaymentTimelineStage.COLLECTION_CANCELLED,
                    PaymentTimelineStatus.CANCELLED,
                    "Demonstration collection cancelled", intent.getCancelledAt(), null);
        }
    }

    private void addPayoutEntries(List<PaymentTimelineEntryResponse> entries,
                                  PaymentIntent intent,
                                  Optional<PayoutTransfer> transfer) {
        if (intent.getPaymentState() != PaymentState.PAYMENT_CONFIRMED) {
            return;
        }
        if (transfer.isEmpty()) {
            if (isBusinessPayoutPending(intent)) {
                add(entries, PaymentTimelineStage.PAYOUT_WAITING_FOR_DESTINATION,
                        PaymentTimelineStatus.PENDING,
                        "Demonstration payout waiting for a verified destination",
                        intent.getConfirmedAt(), null);
            }
            return;
        }

        PayoutTransfer payout = transfer.get();
        add(entries, PaymentTimelineStage.PAYOUT_PENDING,
                payout.getTransferStatus() == PayoutTransferStatus.PENDING
                        ? PaymentTimelineStatus.PENDING : PaymentTimelineStatus.COMPLETED,
                "Demonstration payout pending", payout.getCreatedAt(), payout.getAttemptCount());
        if (payout.getTransferStatus() == PayoutTransferStatus.PROCESSING) {
            add(entries, PaymentTimelineStage.PAYOUT_PROCESSING,
                    PaymentTimelineStatus.PENDING,
                    "Demonstration payout processing",
                    payout.getLastAttemptAt(), payout.getAttemptCount());
        } else if (payout.getTransferStatus() == PayoutTransferStatus.FAILED) {
            add(entries, PaymentTimelineStage.PAYOUT_FAILED,
                    PaymentTimelineStatus.FAILED,
                    "Demonstration payout failed",
                    payout.getLatestFailureAt(), payout.getAttemptCount());
        } else if (payout.getTransferStatus() == PayoutTransferStatus.CONFIRMED) {
            add(entries, PaymentTimelineStage.PAYOUT_CONFIRMED,
                    PaymentTimelineStatus.COMPLETED,
                    "Demonstration payout confirmed",
                    payout.getConfirmedAt(), payout.getAttemptCount());
            addFinalBusinessEntry(entries, intent);
        }
    }

    private boolean isBusinessPayoutPending(PaymentIntent intent) {
        if (intent.getPaymentPurpose() == PaymentPurpose.SETTLEMENT) {
            return settlementRepository.findById(intent.getSettlementId())
                    .map(Settlement::getSettlementState)
                    .filter(state -> state == SettlementState.SETTLEMENT_PAYOUT_PENDING)
                    .isPresent();
        }
        return installmentRepository.findById(intent.getRepaymentInstallmentId())
                .map(RepaymentInstallment::getInstallmentState)
                .filter(state -> state == RepaymentInstallmentState.PAYOUT_PENDING)
                .isPresent();
    }

    private void addFinalBusinessEntry(List<PaymentTimelineEntryResponse> entries,
                                       PaymentIntent intent) {
        if (intent.getPaymentPurpose() == PaymentPurpose.SETTLEMENT) {
            settlementRepository.findById(intent.getSettlementId())
                    .filter(value -> value.getSettlementState()
                            == SettlementState.SETTLEMENT_CONFIRMED)
                    .ifPresent(value -> add(entries,
                            PaymentTimelineStage.SETTLEMENT_CONFIRMED,
                            PaymentTimelineStatus.COMPLETED,
                            "Demonstration settlement confirmed",
                            value.getConfirmedAt(), null));
            return;
        }
        installmentRepository.findById(intent.getRepaymentInstallmentId())
                .filter(value -> value.getInstallmentState()
                        == RepaymentInstallmentState.PAID)
                .ifPresent(value -> add(entries,
                        PaymentTimelineStage.INSTALLMENT_PAID,
                        PaymentTimelineStatus.COMPLETED,
                        "Demonstration installment paid",
                        value.getPaidAt(), null));
    }

    private void add(List<PaymentTimelineEntryResponse> entries,
                     PaymentTimelineStage stage,
                     PaymentTimelineStatus status,
                     String label,
                     Instant occurredAt,
                     Integer attemptCount) {
        if (occurredAt != null) {
            entries.add(new PaymentTimelineEntryResponse(
                    stage, status, label, occurredAt, attemptCount));
        }
    }
}

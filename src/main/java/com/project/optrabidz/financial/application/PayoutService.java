package com.project.optrabidz.financial.application;

import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.financial.application.event.PayoutTransferConfirmedEvent;
import com.project.optrabidz.financial.application.event.PayoutTransferCreatedEvent;
import com.project.optrabidz.financial.application.event.PayoutTransferFailedEvent;
import com.project.optrabidz.financial.application.event.PayoutTransferRetryStartedEvent;
import com.project.optrabidz.financial.application.event.RepaymentInstallmentPaidEvent;
import com.project.optrabidz.financial.application.event.SettlementConfirmedEvent;
import com.project.optrabidz.financial.application.dto.response.PayoutTransferResponse;
import com.project.optrabidz.financial.application.exception.PaymentIntentNotFoundException;
import com.project.optrabidz.financial.application.exception.PayoutDestinationNotReadyException;
import com.project.optrabidz.financial.application.exception.PayoutTransferNotFoundException;
import com.project.optrabidz.financial.application.exception.PayoutTransferStateConflictException;
import com.project.optrabidz.financial.application.payout.PayoutInstruction;
import com.project.optrabidz.financial.application.payout.PayoutOutcome;
import com.project.optrabidz.financial.application.payout.PayoutProvider;
import com.project.optrabidz.financial.application.payout.PayoutResult;
import com.project.optrabidz.financial.domain.model.PaymentAccountBinding;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.model.PayoutTransfer;
import com.project.optrabidz.financial.domain.model.PayoutTransferStatus;
import com.project.optrabidz.financial.domain.model.Repayment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
import com.project.optrabidz.financial.domain.model.Settlement;
import com.project.optrabidz.financial.domain.model.SettlementState;
import com.project.optrabidz.financial.domain.repository.PaymentAccountBindingRepository;
import com.project.optrabidz.financial.domain.repository.PaymentIntentRepository;
import com.project.optrabidz.financial.domain.repository.PayoutTransferRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentInstallmentRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentRepository;
import com.project.optrabidz.financial.domain.repository.SettlementRepository;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.domain.model.Agreement;
import com.project.optrabidz.marketplace.domain.model.AgreementDebtTerms;
import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;
import com.project.optrabidz.marketplace.domain.repository.AgreementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PayoutService {
    private static final String DEMO_PROVIDER = "DEMO";

    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentAccountBindingRepository bindingRepository;
    private final PayoutTransferRepository payoutTransferRepository;
    private final SettlementRepository settlementRepository;
    private final RepaymentInstallmentRepository installmentRepository;
    private final RepaymentRepository repaymentRepository;
    private final AgreementRepository agreementRepository;
    private final EventPublisher eventPublisher;
    private final Optional<PayoutProvider> payoutProvider;

    public PayoutService(PaymentIntentRepository paymentIntentRepository,
                         PaymentAccountBindingRepository bindingRepository,
                         PayoutTransferRepository payoutTransferRepository,
                         SettlementRepository settlementRepository,
                         RepaymentInstallmentRepository installmentRepository,
                         RepaymentRepository repaymentRepository,
                         AgreementRepository agreementRepository,
                         EventPublisher eventPublisher,
                         Optional<PayoutProvider> payoutProvider) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.bindingRepository = bindingRepository;
        this.payoutTransferRepository = payoutTransferRepository;
        this.settlementRepository = settlementRepository;
        this.installmentRepository = installmentRepository;
        this.repaymentRepository = repaymentRepository;
        this.agreementRepository = agreementRepository;
        this.eventPublisher = eventPublisher;
        this.payoutProvider = payoutProvider;
    }

    @Transactional
    public PayoutTransferEnsureResult ensureTransfer(Long paymentIntentId) {
        PayoutTransfer existing = payoutTransferRepository.findByPaymentIntentId(paymentIntentId)
                .orElse(null);
        if (existing != null) {
            return new PayoutTransferEnsureResult(existing, false);
        }

        PaymentIntent paymentIntent = getPaymentIntent(paymentIntentId);
        ensureCollectionConfirmed(paymentIntent);
        PaymentAccountBinding binding = bindingRepository
                .findVerifiedByAccountIdAndProviderCode(
                        paymentIntent.getPayeeAccountId(), DEMO_PROVIDER)
                .orElseThrow(() -> new PayoutDestinationNotReadyException(
                        "Verified payout destination is not ready for accountId="
                                + paymentIntent.getPayeeAccountId()));

        PayoutTransfer candidate = PayoutTransfer.create(
                paymentIntentId,
                binding.getPaymentAccountBindingId(),
                binding.getProviderCode(),
                "PAYOUT-PAYMENT-INTENT-" + paymentIntentId,
                paymentIntent.getAmount(),
                paymentIntent.getCurrencyCode(),
                Instant.now()
        );
        boolean created = payoutTransferRepository.insertIfAbsent(candidate);
        PayoutTransfer winner = payoutTransferRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new IllegalStateException(
                        "Payout transfer insert completed without a canonical transfer"));
        if (created) {
            eventPublisher.publish(new PayoutTransferCreatedEvent(
                    winner.getPayoutTransferId(),
                    winner.getPaymentIntentId(),
                    winner.getPaymentAccountBindingId(),
                    winner.getCreatedAt()
            ));
        }
        return new PayoutTransferEnsureResult(winner, created);
    }

    @Transactional
    public PayoutTransfer executeInitial(Long payoutTransferId) {
        int claimed = payoutTransferRepository.claimForProcessing(
                payoutTransferId, PayoutTransferStatus.PENDING, Instant.now());
        if (claimed == 0) {
            return resolveUnclaimedTransfer(payoutTransferId);
        }
        return executeClaimed(payoutTransferId, null, null);
    }

    @Transactional
    public PayoutTransfer retryFailed(Long accountId,
                                      RoleType roleType,
                                      Long payoutTransferId) {
        PayoutTransfer transfer = getPayoutTransfer(payoutTransferId);
        if (roleType != RoleType.STARTUP && roleType != RoleType.INVESTOR) {
            throw payoutTransferNotFound(payoutTransferId);
        }
        paymentIntentRepository.findByIdForParticipant(transfer.getPaymentIntentId(), accountId)
                .orElseThrow(() -> payoutTransferNotFound(payoutTransferId));

        int claimed = payoutTransferRepository.claimForProcessing(
                payoutTransferId, PayoutTransferStatus.FAILED, Instant.now());
        if (claimed == 0) {
            PayoutTransfer current = getPayoutTransfer(payoutTransferId);
            throw stateConflict(payoutTransferId, current.getTransferStatus());
        }
        return executeClaimed(payoutTransferId, accountId, roleType);
    }

    @Transactional(readOnly = true)
    public PayoutTransferResponse getById(Long accountId,
                                          RoleType roleType,
                                          Long payoutTransferId) {
        ensureParticipantRole(roleType, payoutTransferId);
        PayoutTransfer transfer = getPayoutTransfer(payoutTransferId);
        paymentIntentRepository.findByIdForParticipant(transfer.getPaymentIntentId(), accountId)
                .orElseThrow(() -> payoutTransferNotFound(payoutTransferId));
        return toResponse(transfer);
    }

    @Transactional(readOnly = true)
    public PayoutTransferResponse getByPaymentIntentId(Long accountId,
                                                       RoleType roleType,
                                                       Long paymentIntentId) {
        ensureParticipantRole(roleType, paymentIntentId);
        paymentIntentRepository.findByIdForParticipant(paymentIntentId, accountId)
                .orElseThrow(() -> new PayoutTransferNotFoundException(
                        "Payout transfer not found for paymentIntentId=" + paymentIntentId));
        PayoutTransfer transfer = payoutTransferRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new PayoutTransferNotFoundException(
                        "Payout transfer not found for paymentIntentId=" + paymentIntentId));
        return toResponse(transfer);
    }

    @Transactional
    public PayoutTransferResponse retryFailedForParticipant(Long accountId,
                                                            RoleType roleType,
                                                            Long payoutTransferId) {
        return toResponse(retryFailed(accountId, roleType, payoutTransferId));
    }

    private PayoutTransfer executeClaimed(Long payoutTransferId,
                                          Long retryActorAccountId,
                                          RoleType retryActorRole) {
        PayoutTransfer processingTransfer = getPayoutTransfer(payoutTransferId);
        if (processingTransfer.getTransferStatus() != PayoutTransferStatus.PROCESSING) {
            throw stateConflict(payoutTransferId, processingTransfer.getTransferStatus());
        }
        if (retryActorAccountId != null) {
            eventPublisher.publish(new PayoutTransferRetryStartedEvent(
                    processingTransfer.getPayoutTransferId(),
                    processingTransfer.getPaymentIntentId(),
                    processingTransfer.getAttemptCount(),
                    retryActorAccountId,
                    retryActorRole,
                    processingTransfer.getLastAttemptAt()
            ));
        }

        PaymentAccountBinding capturedBinding = bindingRepository
                .findById(processingTransfer.getPaymentAccountBindingId())
                .orElseThrow(() -> new IllegalStateException(
                        "Captured payout destination is missing for transferId="
                                + payoutTransferId));
        PaymentIntent paymentIntent = getPaymentIntent(processingTransfer.getPaymentIntentId());
        PayoutProvider provider = payoutProvider.orElseThrow(() -> new IllegalStateException(
                "No payout provider is available for transfer execution"));
        PayoutResult result = provider.execute(new PayoutInstruction(
                processingTransfer.getPayoutTransferId(),
                processingTransfer.getIdempotencyKey(),
                capturedBinding.getExternalRecipientReference(),
                processingTransfer.getAmount(),
                processingTransfer.getCurrencyCode(),
                processingTransfer.getAttemptCount()
        ));

        Instant now = Instant.now();
        if (result.outcome() == PayoutOutcome.CONFIRMED) {
            int updated = payoutTransferRepository.markConfirmed(
                    payoutTransferId, result.providerReference(), now);
            ensureTransferUpdated(updated, payoutTransferId);
            PayoutTransfer confirmed = getPayoutTransfer(payoutTransferId);
            finalizeBusinessOperation(paymentIntent, confirmed, now);
            return confirmed;
        }

        int updated = payoutTransferRepository.markFailed(
                payoutTransferId, result.failureCode(), result.failureMessage(), now);
        ensureTransferUpdated(updated, payoutTransferId);
        PayoutTransfer failed = getPayoutTransfer(payoutTransferId);
        eventPublisher.publish(new PayoutTransferFailedEvent(
                failed.getPayoutTransferId(),
                failed.getPaymentIntentId(),
                paymentIntent.getPayerAccountId(),
                paymentIntent.getPayeeAccountId(),
                failed.getAttemptCount(),
                failed.getLatestFailureCode(),
                failed.getLatestFailureMessage(),
                now
        ));
        return failed;
    }

    private void finalizeBusinessOperation(PaymentIntent paymentIntent,
                                           PayoutTransfer confirmedTransfer,
                                           Instant now) {
        if (paymentIntent.getPaymentPurpose() == PaymentPurpose.SETTLEMENT) {
            finalizeSettlement(paymentIntent, confirmedTransfer, now);
        } else {
            finalizeRepaymentInstallment(paymentIntent, confirmedTransfer, now);
        }
    }

    private void finalizeSettlement(PaymentIntent paymentIntent,
                                    PayoutTransfer confirmedTransfer,
                                    Instant now) {
        Settlement settlement = settlementRepository.findById(paymentIntent.getSettlementId())
                .orElseThrow(() -> new IllegalStateException(
                        "Settlement is missing for confirmed payout"));
        int confirmed = settlementRepository.confirmPayoutPending(
                settlement.getSettlementId(), paymentIntent.getPaymentIntentId(), now);
        if (confirmed == 0) {
            throw new PayoutTransferStateConflictException(
                    "Settlement could not be finalized for payoutTransferId="
                            + confirmedTransfer.getPayoutTransferId());
        }

        createRepaymentScheduleIfMissing(settlement, now);
        publishPayoutConfirmed(confirmedTransfer, paymentIntent, now);
        eventPublisher.publish(new SettlementConfirmedEvent(
                settlement.getSettlementId(),
                settlement.getAgreementId(),
                settlement.getStartupId(),
                settlement.getInvestorId(),
                paymentIntent.getPaymentIntentId(),
                paymentIntent.getPayerAccountId(),
                now
        ));
    }

    private void finalizeRepaymentInstallment(PaymentIntent paymentIntent,
                                              PayoutTransfer confirmedTransfer,
                                              Instant now) {
        RepaymentInstallment installment = installmentRepository
                .findById(paymentIntent.getRepaymentInstallmentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Repayment installment is missing for confirmed payout"));
        int confirmed = installmentRepository.confirmPayoutPending(
                installment.getRepaymentInstallmentId(),
                paymentIntent.getPaymentIntentId(),
                now
        );
        if (confirmed == 0) {
            throw new PayoutTransferStateConflictException(
                    "Repayment installment could not be finalized for payoutTransferId="
                            + confirmedTransfer.getPayoutTransferId());
        }

        Repayment repayment = repaymentRepository.findById(installment.getRepaymentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Repayment is missing for confirmed payout"));
        repaymentRepository.refreshStatus(repayment.getRepaymentId(), now);
        publishPayoutConfirmed(confirmedTransfer, paymentIntent, now);
        eventPublisher.publish(new RepaymentInstallmentPaidEvent(
                installment.getRepaymentInstallmentId(),
                repayment.getRepaymentId(),
                repayment.getAgreementId(),
                repayment.getStartupId(),
                repayment.getInvestorId(),
                paymentIntent.getPaymentIntentId(),
                paymentIntent.getPayerAccountId(),
                now
        ));
    }

    private void publishPayoutConfirmed(PayoutTransfer transfer,
                                        PaymentIntent paymentIntent,
                                        Instant now) {
        eventPublisher.publish(new PayoutTransferConfirmedEvent(
                transfer.getPayoutTransferId(),
                transfer.getPaymentIntentId(),
                paymentIntent.getPayerAccountId(),
                paymentIntent.getPayeeAccountId(),
                transfer.getProviderTransferReference(),
                now
        ));
    }

    private void createRepaymentScheduleIfMissing(Settlement settlement, Instant now) {
        if (repaymentRepository.findByAgreementId(settlement.getAgreementId()).isPresent()) {
            return;
        }
        Agreement agreement = agreementRepository.findById(settlement.getAgreementId())
                .orElseThrow(() -> new IllegalStateException(
                        "Agreement is missing for confirmed settlement"));
        AgreementDebtTerms debtTerms = agreement.getDebtTerms();
        int installmentCount = installmentCount(debtTerms);
        BigDecimal totalRepaymentAmount = totalRepaymentAmount(debtTerms);
        Instant finalDueAt = dueAtFor(debtTerms, installmentCount, now);
        Repayment repayment = Repayment.create(
                settlement.getAgreementId(),
                settlement.getStartupId(),
                settlement.getInvestorId(),
                totalRepaymentAmount,
                settlement.getCurrencyCode(),
                installmentCount,
                debtTerms.getRepaymentPlanType(),
                now,
                finalDueAt,
                now
        );
        Repayment savedRepayment = repaymentRepository.save(repayment);

        BigDecimal installmentAmount = totalRepaymentAmount.divide(
                BigDecimal.valueOf(installmentCount), 2, RoundingMode.HALF_UP);
        BigDecimal allocatedAmount = BigDecimal.ZERO;
        List<RepaymentInstallment> installments = new ArrayList<>();
        for (int installmentNumber = 1; installmentNumber <= installmentCount; installmentNumber++) {
            BigDecimal amount = installmentNumber == installmentCount
                    ? totalRepaymentAmount.subtract(allocatedAmount)
                    .setScale(2, RoundingMode.HALF_UP)
                    : installmentAmount;
            allocatedAmount = allocatedAmount.add(amount);
            installments.add(RepaymentInstallment.create(
                    savedRepayment.getRepaymentId(),
                    installmentNumber,
                    amount,
                    settlement.getCurrencyCode(),
                    dueAtFor(debtTerms, installmentNumber, now),
                    now
            ));
        }
        installmentRepository.saveAll(installments);
    }

    private int installmentCount(AgreementDebtTerms debtTerms) {
        return switch (debtTerms.getRepaymentPlanType()) {
            case INSTALLMENT_MONTHLY -> debtTerms.getTenureMonths();
            case INSTALLMENT_QUARTERLY -> (debtTerms.getTenureMonths() + 2) / 3;
            case ONE_TIME -> 1;
        };
    }

    private BigDecimal totalRepaymentAmount(AgreementDebtTerms debtTerms) {
        int interestMonths = debtTerms.getRepaymentPlanType() == RepaymentPlanType.ONE_TIME
                ? debtTerms.getOneTimeRepaymentDueAfterMonths()
                : debtTerms.getTenureMonths();
        BigDecimal principal = debtTerms.getPrincipalAmount();
        BigDecimal annualInterestRate = debtTerms.getInterestRate()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal interestPeriodInYears = BigDecimal.valueOf(interestMonths)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        return principal.add(principal.multiply(annualInterestRate).multiply(interestPeriodInYears))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Instant dueAtFor(AgreementDebtTerms debtTerms,
                             int repaymentNumber,
                             Instant scheduleStart) {
        long months = switch (debtTerms.getRepaymentPlanType()) {
            case INSTALLMENT_MONTHLY -> repaymentNumber;
            case INSTALLMENT_QUARTERLY -> Math.min(
                    repaymentNumber * 3L, debtTerms.getTenureMonths());
            case ONE_TIME -> debtTerms.getOneTimeRepaymentDueAfterMonths();
        };
        return scheduleStart.atZone(ZoneOffset.UTC).plusMonths(months).toInstant();
    }

    private PayoutTransfer resolveUnclaimedTransfer(Long payoutTransferId) {
        PayoutTransfer transfer = getPayoutTransfer(payoutTransferId);
        if (transfer.getTransferStatus() == PayoutTransferStatus.CONFIRMED) {
            return transfer;
        }
        throw stateConflict(payoutTransferId, transfer.getTransferStatus());
    }

    private void ensureTransferUpdated(int updated, Long payoutTransferId) {
        if (updated == 0) {
            throw new PayoutTransferStateConflictException(
                    "Payout result could not be recorded for payoutTransferId="
                            + payoutTransferId);
        }
    }

    private PayoutTransferResponse toResponse(PayoutTransfer transfer) {
        PaymentAccountBinding binding = bindingRepository
                .findById(transfer.getPaymentAccountBindingId())
                .orElseThrow(() -> new IllegalStateException(
                        "Captured payout destination is missing for transferId="
                                + transfer.getPayoutTransferId()));
        return new PayoutTransferResponse(
                transfer.getPayoutTransferId(),
                transfer.getPaymentIntentId(),
                transfer.getProviderCode(),
                transfer.getTransferStatus(),
                transfer.getAmount(),
                transfer.getCurrencyCode(),
                "•••• " + binding.getMaskedAccountSuffix(),
                transfer.getAttemptCount(),
                transfer.getCreatedAt(),
                transfer.getLastAttemptAt(),
                transfer.getConfirmedAt(),
                transfer.getLatestFailureAt(),
                transfer.getLatestFailureCode()
        );
    }

    private void ensureParticipantRole(RoleType roleType, Long hiddenResourceId) {
        if (roleType != RoleType.STARTUP && roleType != RoleType.INVESTOR) {
            throw payoutTransferNotFound(hiddenResourceId);
        }
    }

    private PaymentIntent getPaymentIntent(Long paymentIntentId) {
        return paymentIntentRepository.findById(paymentIntentId)
                .orElseThrow(() -> new PaymentIntentNotFoundException(
                        "Payment intent not found for payout handoff"));
    }

    private PayoutTransfer getPayoutTransfer(Long payoutTransferId) {
        return payoutTransferRepository.findById(payoutTransferId)
                .orElseThrow(() -> payoutTransferNotFound(payoutTransferId));
    }

    private PayoutTransferNotFoundException payoutTransferNotFound(Long payoutTransferId) {
        return new PayoutTransferNotFoundException(
                "Payout transfer not found for payoutTransferId=" + payoutTransferId);
    }

    private PayoutTransferStateConflictException stateConflict(
            Long payoutTransferId,
            PayoutTransferStatus status) {
        return new PayoutTransferStateConflictException(
                "Payout transfer cannot be claimed from status=" + status
                        + " for payoutTransferId=" + payoutTransferId);
    }

    private void ensureCollectionConfirmed(PaymentIntent paymentIntent) {
        if (paymentIntent.getPaymentState() != PaymentState.PAYMENT_CONFIRMED) {
            throw new IllegalStateException("Payout requires a confirmed payment intent");
        }
        if (paymentIntent.getPaymentPurpose() == PaymentPurpose.SETTLEMENT) {
            Settlement settlement = settlementRepository.findById(paymentIntent.getSettlementId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Settlement is missing for confirmed payment intent"));
            if (settlement.getSettlementState() != SettlementState.SETTLEMENT_PAYOUT_PENDING
                    || !paymentIntent.getPaymentIntentId()
                    .equals(settlement.getConfirmedPaymentIntentId())) {
                throw new IllegalStateException(
                        "Settlement is not awaiting payout for this payment intent");
            }
            return;
        }

        RepaymentInstallment installment = installmentRepository
                .findById(paymentIntent.getRepaymentInstallmentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Repayment installment is missing for confirmed payment intent"));
        if (installment.getInstallmentState() != RepaymentInstallmentState.PAYOUT_PENDING
                || !paymentIntent.getPaymentIntentId()
                .equals(installment.getConfirmedPaymentIntentId())) {
            throw new IllegalStateException(
                    "Repayment installment is not awaiting payout for this payment intent");
        }
    }
}

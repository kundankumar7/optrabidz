package com.project.optrabidz.financial.application;

import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.financial.application.command.PaymentAttemptConfirmationCommand;
import com.project.optrabidz.financial.application.command.PaymentAttemptFailureCommand;
import com.project.optrabidz.financial.application.dto.request.CreatePaymentAttemptRequest;
import com.project.optrabidz.financial.application.dto.response.DebtTermsResponse;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.financial.application.dto.response.PaymentIntentResponse;
import com.project.optrabidz.financial.application.dto.response.RepaymentInstallmentResponse;
import com.project.optrabidz.financial.application.dto.response.RepaymentProgressResponse;
import com.project.optrabidz.financial.application.dto.response.RepaymentResponse;
import com.project.optrabidz.financial.application.dto.response.SettlementResponse;
import com.project.optrabidz.financial.application.exception.FinancialAccessException;
import com.project.optrabidz.financial.application.exception.InvalidPaymentStateException;
import com.project.optrabidz.financial.application.exception.InvalidRepaymentStateException;
import com.project.optrabidz.financial.application.exception.InvalidSettlementStateException;
import com.project.optrabidz.financial.application.exception.PaymentAlreadyConfirmedException;
import com.project.optrabidz.financial.application.exception.PaymentAttemptNotFoundException;
import com.project.optrabidz.financial.application.exception.PaymentIntentExpiredException;
import com.project.optrabidz.financial.application.exception.PaymentIntentNotFoundException;
import com.project.optrabidz.financial.application.exception.PaymentIntentNotActiveException;
import com.project.optrabidz.financial.application.exception.RepaymentInstallmentNotFoundException;
import com.project.optrabidz.financial.application.exception.RepaymentInstallmentNotPayableException;
import com.project.optrabidz.financial.application.exception.RepaymentNotFoundException;
import com.project.optrabidz.financial.application.exception.SettlementNotFoundException;
import com.project.optrabidz.financial.application.exception.SettlementNotPayableException;
import com.project.optrabidz.financial.application.exception.UnsupportedPaymentMethodException;
import com.project.optrabidz.financial.application.event.RepaymentInstallmentPaidEvent;
import com.project.optrabidz.financial.application.event.RepaymentInstallmentPaymentFailedEvent;
import com.project.optrabidz.financial.application.event.SettlementConfirmedEvent;
import com.project.optrabidz.financial.application.strategy.LocalPaymentStrategy;
import com.project.optrabidz.financial.application.strategy.PaymentMethodStrategy;
import com.project.optrabidz.financial.application.strategy.PaymentMethodStrategyRegistry;
import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.model.PaymentAttemptState;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.model.Repayment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentPaymentView;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
import com.project.optrabidz.financial.domain.model.RepaymentProgress;
import com.project.optrabidz.financial.domain.model.RepaymentState;
import com.project.optrabidz.financial.domain.model.Settlement;
import com.project.optrabidz.financial.domain.model.SettlementState;
import com.project.optrabidz.financial.domain.repository.PaymentAttemptRepository;
import com.project.optrabidz.financial.domain.repository.PaymentIntentRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentInstallmentRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentRepository;
import com.project.optrabidz.financial.domain.repository.SettlementRepository;
import com.project.optrabidz.financial.infrastructure.repository.JpaPaymentProviderMethodRepository;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.domain.model.Agreement;
import com.project.optrabidz.marketplace.domain.model.AgreementDebtTerms;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;
import com.project.optrabidz.marketplace.domain.repository.AgreementRepository;
import com.project.optrabidz.marketplace.domain.repository.FundingListingRepository;
import com.project.optrabidz.participation.application.exception.ParticipationNotFoundException;
import com.project.optrabidz.participation.domain.model.Investor;
import com.project.optrabidz.participation.domain.model.Startup;
import com.project.optrabidz.participation.domain.repository.InvestorRepository;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.UUID;

@Service
public class FinancialService {
    private final SettlementRepository settlementRepository;
    private final RepaymentRepository repaymentRepository;
    private final RepaymentInstallmentRepository repaymentInstallmentRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final StartupRepository startupRepository;
    private final InvestorRepository investorRepository;
    private final AgreementRepository agreementRepository;
    private final FundingListingRepository fundingListingRepository;
    private final JpaPaymentProviderMethodRepository paymentProviderMethodRepository;
    private final PaymentMethodStrategyRegistry paymentMethodStrategyRegistry;
    private final EventPublisher eventPublisher;
    private final long settlementExpiryMinutes;
    private final long paymentIntentExpiryMinutes;

    public FinancialService(SettlementRepository settlementRepository,
                            RepaymentRepository repaymentRepository,
                            RepaymentInstallmentRepository repaymentInstallmentRepository,
                            PaymentIntentRepository paymentIntentRepository,
                            PaymentAttemptRepository paymentAttemptRepository,
                            StartupRepository startupRepository,
                            InvestorRepository investorRepository,
                            AgreementRepository agreementRepository,
                            FundingListingRepository fundingListingRepository,
                            JpaPaymentProviderMethodRepository paymentProviderMethodRepository,
                            PaymentMethodStrategyRegistry paymentMethodStrategyRegistry,
                            EventPublisher eventPublisher,
                            @Value("${optrabidz.financial.settlement.expiry-minutes:30}") long settlementExpiryMinutes,
                            @Value("${optrabidz.financial.payment-intent.expiry-minutes:15}") long paymentIntentExpiryMinutes) {
        this.settlementRepository = settlementRepository;
        this.repaymentRepository = repaymentRepository;
        this.repaymentInstallmentRepository = repaymentInstallmentRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.startupRepository = startupRepository;
        this.investorRepository = investorRepository;
        this.agreementRepository = agreementRepository;
        this.fundingListingRepository = fundingListingRepository;
        this.paymentProviderMethodRepository = paymentProviderMethodRepository;
        this.paymentMethodStrategyRegistry = paymentMethodStrategyRegistry;
        this.eventPublisher = eventPublisher;
        this.settlementExpiryMinutes = settlementExpiryMinutes;
        this.paymentIntentExpiryMinutes = paymentIntentExpiryMinutes;
    }

    @Transactional
    public SettlementResponse createSettlementForAgreement(Agreement agreement) {
        return settlementRepository.findByAgreementId(agreement.getAgreementId())
                .map(settlement -> toSettlementResponse(settlement, agreement))
                .orElseGet(() -> toSettlementResponse(createNewSettlement(agreement), agreement));
    }

    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(Long accountId, RoleType roleType, Long settlementId) {
        Settlement settlement = getSettlement(settlementId);
        ensureSettlementVisible(accountId, roleType, settlement);
        return toSettlementResponse(settlement);
    }

    @Transactional(readOnly = true)
    public PageResponse<SettlementResponse> getMyInvestorSettlements(Long accountId, RoleType roleType, int page, int size) {
        ensureRole(roleType, RoleType.INVESTOR);
        Investor investor = getInvestorByAccount(accountId);
        Page<SettlementResponse> settlements = settlementRepository
                .findByInvestorId(investor.getInvestorId(), pageRequest(page, size))
                .map(this::toSettlementResponse);
        return toPageResponse(settlements, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<SettlementResponse> getMyStartupSettlements(Long accountId, RoleType roleType, int page, int size) {
        ensureRole(roleType, RoleType.STARTUP);
        Startup startup = getStartupByAccount(accountId);
        Page<SettlementResponse> settlements = settlementRepository
                .findByStartupId(startup.getStartupId(), pageRequest(page, size))
                .map(this::toSettlementResponse);
        return toPageResponse(settlements, page, size);
    }

    @Transactional
    public PaymentIntentResponse createSettlementPaymentIntent(Long accountId, RoleType roleType, Long settlementId) {
        ensureRole(roleType, RoleType.INVESTOR);
        Settlement settlement = getSettlement(settlementId);
        Investor investor = getInvestorByAccount(accountId);
        if (!settlement.getInvestorId().equals(investor.getInvestorId())) {
            throw new FinancialAccessException("Investor can pay only own settlement");
        }
        ensureSettlementPayable(settlement);

        return paymentIntentRepository.findActiveBySettlementId(settlementId)
                .map(this::toPaymentIntentResponse)
                .orElseGet(() -> toPaymentIntentResponse(createSettlementIntent(settlement)));
    }

    @Transactional(readOnly = true)
    public RepaymentResponse getRepayment(Long accountId, RoleType roleType, Long repaymentId) {
        Repayment repayment = getRepayment(repaymentId);
        ensureRepaymentVisible(accountId, roleType, repayment);
        return toRepaymentResponse(repayment);
    }

    @Transactional(readOnly = true)
    public PageResponse<RepaymentInstallmentResponse> getRepaymentInstallments(Long accountId,
                                                                               RoleType roleType,
                                                                               Long repaymentId,
                                                                               RepaymentInstallmentState installmentState,
                                                                               RepaymentInstallmentPaymentView paymentView,
                                                                               int page,
                                                                               int size) {
        Repayment repayment = getRepayment(repaymentId);
        ensureRepaymentVisible(accountId, roleType, repayment);
        Collection<RepaymentInstallmentState> states = resolveInstallmentStates(installmentState, paymentView);
        Page<RepaymentInstallmentResponse> installments = repaymentInstallmentRepository
                .findByRepaymentIdAndStates(repaymentId, states, installmentPageRequest(page, size))
                .map(this::toRepaymentInstallmentResponse);
        return toPageResponse(installments, page, size);
    }

    @Transactional(readOnly = true)
    public RepaymentInstallmentResponse getRepaymentInstallment(Long accountId,
                                                               RoleType roleType,
                                                               Long installmentId) {
        RepaymentInstallment installment = getRepaymentInstallment(installmentId);
        Repayment repayment = getRepayment(installment.getRepaymentId());
        ensureRepaymentVisible(accountId, roleType, repayment);
        return toRepaymentInstallmentResponse(installment);
    }

    @Transactional(readOnly = true)
    public RepaymentProgressResponse getRepaymentProgress(Long accountId, RoleType roleType, Long agreementId) {
        Agreement agreement = getAgreement(agreementId);
        ensureAgreementVisible(accountId, roleType, agreement);
        return repaymentRepository.getProgressByAgreementId(agreementId)
                .map(progress -> toRepaymentProgressResponse(agreement, progress))
                .orElseGet(() -> emptyRepaymentProgressResponse(agreement));
    }

    @Transactional(readOnly = true)
    public PageResponse<RepaymentResponse> getMyInvestorRepayments(Long accountId, RoleType roleType, int page, int size) {
        ensureRole(roleType, RoleType.INVESTOR);
        Investor investor = getInvestorByAccount(accountId);
        Page<RepaymentResponse> repayments = repaymentRepository
                .findByInvestorId(investor.getInvestorId(), pageRequest(page, size))
                .map(this::toRepaymentResponse);
        return toPageResponse(repayments, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<RepaymentInstallmentResponse> getMyInvestorRepaymentInstallments(Long accountId,
                                                                                         RoleType roleType,
                                                                                         RepaymentInstallmentState installmentState,
                                                                                         RepaymentInstallmentPaymentView paymentView,
                                                                                         int page,
                                                                                         int size) {
        ensureRole(roleType, RoleType.INVESTOR);
        Investor investor = getInvestorByAccount(accountId);
        Collection<RepaymentInstallmentState> states = resolveInstallmentStates(installmentState, paymentView);
        Page<RepaymentInstallmentResponse> installments = repaymentInstallmentRepository
                .findByInvestorIdAndStates(investor.getInvestorId(), states, installmentDuePageRequest(page, size))
                .map(this::toRepaymentInstallmentResponse);
        return toPageResponse(installments, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<RepaymentResponse> getMyStartupRepayments(Long accountId, RoleType roleType, int page, int size) {
        ensureRole(roleType, RoleType.STARTUP);
        Startup startup = getStartupByAccount(accountId);
        Page<RepaymentResponse> repayments = repaymentRepository
                .findByStartupId(startup.getStartupId(), pageRequest(page, size))
                .map(this::toRepaymentResponse);
        return toPageResponse(repayments, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<RepaymentInstallmentResponse> getMyStartupRepaymentInstallments(Long accountId,
                                                                                        RoleType roleType,
                                                                                        RepaymentInstallmentState installmentState,
                                                                                        RepaymentInstallmentPaymentView paymentView,
                                                                                        int page,
                                                                                        int size) {
        ensureRole(roleType, RoleType.STARTUP);
        Startup startup = getStartupByAccount(accountId);
        Collection<RepaymentInstallmentState> states = resolveInstallmentStates(installmentState, paymentView);
        Page<RepaymentInstallmentResponse> installments = repaymentInstallmentRepository
                .findByStartupIdAndStates(startup.getStartupId(), states, installmentDuePageRequest(page, size))
                .map(this::toRepaymentInstallmentResponse);
        return toPageResponse(installments, page, size);
    }

    @Transactional
    public PaymentIntentResponse createRepaymentInstallmentPaymentIntent(Long accountId,
                                                                        RoleType roleType,
                                                                        Long installmentId) {
        ensureRole(roleType, RoleType.STARTUP);
        RepaymentInstallment installment = getRepaymentInstallment(installmentId);
        Repayment repayment = getRepayment(installment.getRepaymentId());
        Startup startup = getStartupByAccount(accountId);
        if (!repayment.getStartupId().equals(startup.getStartupId())) {
            throw new FinancialAccessException("Startup can pay only own repayment");
        }

        return paymentIntentRepository.findActiveByRepaymentInstallmentId(installmentId)
                .map(this::toPaymentIntentResponse)
                .orElseGet(() -> {
                    ensureRepaymentInstallmentPayable(installment);
                    Instant now = Instant.now();
                    PaymentIntent intent = createRepaymentInstallmentIntent(repayment, installment);
                    repaymentInstallmentRepository.markPaymentInProgress(installmentId, now);
                    repaymentRepository.refreshStatus(repayment.getRepaymentId(), now);
                    return toPaymentIntentResponse(intent);
                });
    }

    @Transactional
    public PaymentIntentResponse createRepaymentPaymentIntent(Long accountId, RoleType roleType, Long repaymentId) {
        Repayment repayment = getRepayment(repaymentId);
        ensureRepaymentVisible(accountId, roleType, repayment);
        RepaymentInstallment nextInstallment = repaymentInstallmentRepository.findNextPayableByRepaymentId(repaymentId)
                .orElseThrow(() -> new RepaymentInstallmentNotPayableException("No payable repayment installment found"));
        return createRepaymentInstallmentPaymentIntent(
                accountId,
                roleType,
                nextInstallment.getRepaymentInstallmentId()
        );
    }

    @Transactional(readOnly = true)
    public PaymentIntentResponse getPaymentIntent(Long accountId, RoleType roleType, Long paymentIntentId) {
        PaymentIntent paymentIntent = getPaymentIntent(paymentIntentId);
        ensurePaymentIntentVisible(accountId, roleType, paymentIntent);
        return toPaymentIntentResponse(paymentIntent);
    }

    @Transactional
    public PaymentAttemptResponse createPaymentAttempt(Long accountId,
                                                       RoleType roleType,
                                                       Long paymentIntentId,
                                                       CreatePaymentAttemptRequest request) {
        PaymentIntent paymentIntent = getPaymentIntent(paymentIntentId);
        ensurePaymentActor(accountId, roleType, paymentIntent);
        ensurePaymentIntentActive(paymentIntent);

        String providerCode = normalizeProviderCode(request == null ? null : request.providerCode());
        PaymentMethodType methodType = request == null || request.methodType() == null
                ? PaymentMethodType.OTHER
                : request.methodType();
        ensureProviderMethodEnabled(providerCode, methodType, paymentIntent.getCurrencyCode());

        Instant now = Instant.now();
        PaymentAttempt attempt = PaymentAttempt.create(paymentIntentId, providerCode, methodType, now);
        PaymentAttempt savedAttempt = paymentAttemptRepository.save(attempt);
        PaymentMethodStrategy strategy = paymentMethodStrategyRegistry.resolve(providerCode, methodType);
        PaymentAttempt initiatedAttempt = applyPaymentAttemptTransition(
                () -> strategy.initiate(paymentIntent, savedAttempt, now)
        );
        PaymentAttempt finalAttempt = paymentAttemptRepository.save(initiatedAttempt);
        applyPaymentIntentTransition(paymentIntent::markPending);
        paymentIntentRepository.save(paymentIntent);
        return toPaymentAttemptResponse(finalAttempt);
    }

    @Transactional
    public PaymentAttemptResponse confirmLocalPaymentAttempt(Long accountId, RoleType roleType, Long paymentAttemptId) {
        return confirmPaymentAttempt(PaymentAttemptConfirmationCommand.authenticatedLocal(
                accountId,
                roleType,
                paymentAttemptId
        ));
    }

    @Transactional
    public PaymentAttemptResponse confirmProviderPaymentAttempt(String providerCode,
                                                               Long paymentAttemptId,
                                                               String providerPaymentId) {
        return confirmPaymentAttempt(PaymentAttemptConfirmationCommand.providerCallback(
                providerCode,
                paymentAttemptId,
                providerPaymentId
        ));
    }

    private PaymentAttemptResponse confirmPaymentAttempt(PaymentAttemptConfirmationCommand command) {
        PaymentAttempt attempt = getPaymentAttempt(command.paymentAttemptId());
        ensureProviderAttempt(attempt, command.providerCode());
        PaymentIntent paymentIntent = getPaymentIntent(attempt.getPaymentIntentId());
        if (command.authenticatedActorRequired()) {
            ensurePaymentActor(command.actorAccountId(), command.actorRole(), paymentIntent);
        }
        Instant now = Instant.now();
        int confirmedAttemptCount = paymentAttemptRepository.confirmActive(
                command.paymentAttemptId(),
                command.providerPaymentId(),
                now
        );
        PaymentAttempt confirmedAttempt = getPaymentAttempt(command.paymentAttemptId());
        if (confirmedAttemptCount == 0) {
            if (confirmedAttempt.getAttemptState() == PaymentAttemptState.CONFIRMED) {
                return toPaymentAttemptResponse(confirmedAttempt);
            }
            throw new InvalidPaymentStateException("Payment attempt is not active");
        }

        int confirmedIntentCount = paymentIntentRepository.confirmActive(paymentIntent.getPaymentIntentId(), now);
        if (confirmedIntentCount == 0) {
            throw paymentIntentNotActiveException(paymentIntent.getPaymentIntentId());
        }

        PaymentIntent confirmedPaymentIntent = getPaymentIntent(paymentIntent.getPaymentIntentId());
        applyBusinessConfirmation(confirmedPaymentIntent, now);
        return toPaymentAttemptResponse(confirmedAttempt);
    }

    @Transactional
    public PaymentAttemptResponse failLocalPaymentAttempt(Long accountId, RoleType roleType, Long paymentAttemptId) {
        return failPaymentAttempt(PaymentAttemptFailureCommand.authenticatedLocal(
                accountId,
                roleType,
                paymentAttemptId
        ));
    }

    @Transactional
    public PaymentAttemptResponse failProviderPaymentAttempt(String providerCode,
                                                            Long paymentAttemptId,
                                                            String failureCode,
                                                            String failureMessage) {
        return failPaymentAttempt(PaymentAttemptFailureCommand.providerCallback(
                providerCode,
                paymentAttemptId,
                failureCode,
                failureMessage
        ));
    }

    private PaymentAttemptResponse failPaymentAttempt(PaymentAttemptFailureCommand command) {
        PaymentAttempt attempt = getPaymentAttempt(command.paymentAttemptId());
        ensureProviderAttempt(attempt, command.providerCode());
        PaymentIntent paymentIntent = getPaymentIntent(attempt.getPaymentIntentId());
        if (command.authenticatedActorRequired()) {
            ensurePaymentActor(command.actorAccountId(), command.actorRole(), paymentIntent);
        }
        Instant now = Instant.now();
        int failedAttemptCount = paymentAttemptRepository.failActive(
                command.paymentAttemptId(),
                command.failureCode(),
                command.failureMessage(),
                now
        );
        PaymentAttempt failedAttempt = getPaymentAttempt(command.paymentAttemptId());
        if (failedAttemptCount == 0) {
            if (failedAttempt.getAttemptState() == PaymentAttemptState.FAILED) {
                return toPaymentAttemptResponse(failedAttempt);
            }
            throw new InvalidPaymentStateException("Payment attempt is not active");
        }

        int failedIntentCount = paymentIntentRepository.failActive(
                paymentIntent.getPaymentIntentId(),
                command.failureCode(),
                command.failureMessage(),
                now
        );
        if (failedIntentCount == 0) {
            throw paymentIntentNotActiveException(paymentIntent.getPaymentIntentId());
        }
        PaymentIntent failedPaymentIntent = getPaymentIntent(paymentIntent.getPaymentIntentId());
        applyBusinessFailure(failedPaymentIntent, command.failureMessage(), now);
        return toPaymentAttemptResponse(failedAttempt);
    }

    private void applyBusinessFailure(PaymentIntent paymentIntent, String reason, Instant now) {
        if (paymentIntent.getPaymentPurpose() != PaymentPurpose.REPAYMENT) {
            return;
        }
        RepaymentInstallment installment = getRepaymentInstallment(paymentIntent.getRepaymentInstallmentId());
        Repayment repayment = getRepayment(installment.getRepaymentId());
        repaymentInstallmentRepository.markPaymentFailed(
                installment.getRepaymentInstallmentId(),
                reason == null || reason.isBlank() ? "Payment failed" : reason,
                now
        );
        repaymentRepository.refreshStatus(installment.getRepaymentId(), now);
        eventPublisher.publish(new RepaymentInstallmentPaymentFailedEvent(
                installment.getRepaymentInstallmentId(),
                repayment.getRepaymentId(),
                repayment.getAgreementId(),
                repayment.getStartupId(),
                repayment.getInvestorId(),
                paymentIntent.getPaymentIntentId(),
                paymentIntent.getPayerAccountId(),
                reason == null || reason.isBlank() ? "Payment failed" : reason,
                now
        ));
    }

    @Transactional
    public int expirePendingPaymentIntents(Instant now, int batchSize) {
        java.util.List<Long> affectedInstallmentIds =
                paymentIntentRepository.findExpiredActiveRepaymentInstallmentIds(now, batchSize);
        int expiredCount = paymentIntentRepository.expireExpiredActive(now, batchSize);
        for (Long installmentId : affectedInstallmentIds) {
            RepaymentInstallment installment = getRepaymentInstallment(installmentId);
            repaymentInstallmentRepository.markPaymentFailed(installmentId, "Payment intent expired", now);
            repaymentRepository.refreshStatus(installment.getRepaymentId(), now);
        }
        return expiredCount;
    }

    @Transactional
    public int expirePendingSettlements(Instant now, int batchSize) {
        return settlementRepository.expireExpiredPending(now, batchSize);
    }

    @Transactional
    public int markOverdueRepaymentInstallments(Instant now, int batchSize) {
        java.util.List<Long> installmentIds = repaymentInstallmentRepository.findOverdueEligibleIds(now, batchSize);
        if (installmentIds.isEmpty()) {
            return 0;
        }
        java.util.List<Long> repaymentIds = repaymentInstallmentRepository.findRepaymentIdsByInstallmentIds(installmentIds);
        int changedCount = repaymentInstallmentRepository.markOverdue(installmentIds, now);
        repaymentIds.forEach(repaymentId -> repaymentRepository.refreshStatus(repaymentId, now));
        return changedCount;
    }

    private Settlement createNewSettlement(Agreement agreement) {
        FundingListing listing = fundingListingRepository.findById(agreement.getListingId())
                .orElseThrow(() -> new IllegalStateException("Funding listing not found for agreement"));
        Instant now = Instant.now();
        Settlement settlement = Settlement.create(
                agreement.getAgreementId(),
                agreement.getStartupId(),
                agreement.getInvestorId(),
                agreement.getDebtTerms().getPrincipalAmount(),
                listing.getDebtTerms().getCurrencyCode(),
                now,
                now.plus(Duration.ofMinutes(settlementExpiryMinutes))
        );
        try {
            return settlementRepository.save(settlement);
        } catch (DataIntegrityViolationException exception) {
            return settlementRepository.findByAgreementId(agreement.getAgreementId())
                    .orElseThrow(() -> exception);
        }
    }

    private PaymentIntent createSettlementIntent(Settlement settlement) {
        Startup startup = getStartupById(settlement.getStartupId());
        Investor investor = getInvestorById(settlement.getInvestorId());
        Instant now = Instant.now();
        PaymentIntent paymentIntent = PaymentIntent.forSettlement(
                settlement.getSettlementId(),
                investor.getAccountId(),
                startup.getAccountId(),
                settlement.getAmount(),
                settlement.getCurrencyCode(),
                "SETTLEMENT-" + settlement.getSettlementId() + "-" + UUID.randomUUID(),
                now,
                now.plus(Duration.ofMinutes(paymentIntentExpiryMinutes))
        );
        return paymentIntentRepository.saveNewOrFindActiveBySettlement(paymentIntent);
    }

    private PaymentIntent createRepaymentInstallmentIntent(Repayment repayment, RepaymentInstallment installment) {
        Startup startup = getStartupById(repayment.getStartupId());
        Investor investor = getInvestorById(repayment.getInvestorId());
        Instant now = Instant.now();
        PaymentIntent paymentIntent = PaymentIntent.forRepaymentInstallment(
                installment.getRepaymentInstallmentId(),
                startup.getAccountId(),
                investor.getAccountId(),
                installment.getAmount(),
                installment.getCurrencyCode(),
                "REPAYMENT-INSTALLMENT-" + installment.getRepaymentInstallmentId() + "-" + UUID.randomUUID(),
                now,
                now.plus(Duration.ofMinutes(paymentIntentExpiryMinutes))
        );
        return paymentIntentRepository.saveNewOrFindActiveByRepaymentInstallment(paymentIntent);
    }

    private void applyBusinessConfirmation(PaymentIntent paymentIntent, Instant now) {
        if (paymentIntent.getPaymentPurpose() == PaymentPurpose.SETTLEMENT) {
            Settlement settlement = getSettlement(paymentIntent.getSettlementId());
            int confirmedCount = settlementRepository.confirmPending(
                    settlement.getSettlementId(),
                    paymentIntent.getPaymentIntentId(),
                    now
            );
            if (confirmedCount == 0) {
                ensureAlreadyConfirmedBySameIntent(settlement, paymentIntent.getPaymentIntentId());
                return;
            }
            createRepaymentScheduleIfMissing(settlement, now);
            eventPublisher.publish(new SettlementConfirmedEvent(
                    settlement.getSettlementId(),
                    settlement.getAgreementId(),
                    settlement.getStartupId(),
                    settlement.getInvestorId(),
                    paymentIntent.getPaymentIntentId(),
                    paymentIntent.getPayerAccountId(),
                    now
            ));
            return;
        }

        RepaymentInstallment installment = getRepaymentInstallment(paymentIntent.getRepaymentInstallmentId());
        Repayment repayment = getRepayment(installment.getRepaymentId());
        int confirmedCount = repaymentInstallmentRepository.markPaid(
                installment.getRepaymentInstallmentId(),
                paymentIntent.getPaymentIntentId(),
                now
        );
        if (confirmedCount == 0) {
            ensureAlreadyConfirmedBySameIntent(installment, paymentIntent.getPaymentIntentId());
        }
        repaymentRepository.refreshStatus(installment.getRepaymentId(), now);
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

    private void createRepaymentScheduleIfMissing(Settlement settlement, Instant now) {
        if (repaymentRepository.findByAgreementId(settlement.getAgreementId()).isPresent()) {
            return;
        }
        Agreement agreement = getAgreement(settlement.getAgreementId());
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
                BigDecimal.valueOf(installmentCount),
                2,
                RoundingMode.HALF_UP
        );
        BigDecimal allocatedAmount = BigDecimal.ZERO;
        java.util.List<RepaymentInstallment> installments = new java.util.ArrayList<>();

        for (int installmentNumber = 1; installmentNumber <= installmentCount; installmentNumber++) {
            BigDecimal amount = installmentNumber == installmentCount
                    ? totalRepaymentAmount.subtract(allocatedAmount).setScale(2, RoundingMode.HALF_UP)
                    : installmentAmount;
            allocatedAmount = allocatedAmount.add(amount);
            Instant dueAt = dueAtFor(debtTerms, installmentNumber, now);
            installments.add(RepaymentInstallment.create(
                    savedRepayment.getRepaymentId(),
                    installmentNumber,
                    amount,
                    settlement.getCurrencyCode(),
                    dueAt,
                    now
            ));
        }
        repaymentInstallmentRepository.saveAll(installments);
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

    private Instant dueAtFor(AgreementDebtTerms debtTerms, int repaymentNumber, Instant scheduleStart) {
        long months = switch (debtTerms.getRepaymentPlanType()) {
            case INSTALLMENT_MONTHLY -> repaymentNumber;
            case INSTALLMENT_QUARTERLY -> repaymentNumber * 3L;
            case ONE_TIME -> debtTerms.getOneTimeRepaymentDueAfterMonths();
        };
        return scheduleStart.atZone(ZoneOffset.UTC)
                .plusMonths(months)
                .toInstant();
    }

    private void ensureSettlementVisible(Long accountId, RoleType roleType, Settlement settlement) {
        if (roleType == RoleType.ADMIN) {
            return;
        }
        if (roleType == RoleType.STARTUP && getStartupByAccount(accountId).getStartupId().equals(settlement.getStartupId())) {
            return;
        }
        if (roleType == RoleType.INVESTOR && getInvestorByAccount(accountId).getInvestorId().equals(settlement.getInvestorId())) {
            return;
        }
        throw new FinancialAccessException("You are not authorized to view this settlement");
    }

    private void ensureRepaymentVisible(Long accountId, RoleType roleType, Repayment repayment) {
        if (roleType == RoleType.ADMIN) {
            return;
        }
        if (roleType == RoleType.STARTUP && getStartupByAccount(accountId).getStartupId().equals(repayment.getStartupId())) {
            return;
        }
        if (roleType == RoleType.INVESTOR && getInvestorByAccount(accountId).getInvestorId().equals(repayment.getInvestorId())) {
            return;
        }
        throw new FinancialAccessException("You are not authorized to view this repayment");
    }

    private void ensureAgreementVisible(Long accountId, RoleType roleType, Agreement agreement) {
        if (roleType == RoleType.ADMIN) {
            return;
        }
        if (roleType == RoleType.STARTUP && getStartupByAccount(accountId).getStartupId().equals(agreement.getStartupId())) {
            return;
        }
        if (roleType == RoleType.INVESTOR && getInvestorByAccount(accountId).getInvestorId().equals(agreement.getInvestorId())) {
            return;
        }
        throw new FinancialAccessException("You are not authorized to view this agreement repayment progress");
    }

    private void ensurePaymentIntentVisible(Long accountId, RoleType roleType, PaymentIntent paymentIntent) {
        if (roleType == RoleType.ADMIN || paymentIntent.getPayerAccountId().equals(accountId)
                || paymentIntent.getPayeeAccountId().equals(accountId)) {
            return;
        }
        throw new FinancialAccessException("You are not authorized to view this payment intent");
    }

    private void ensurePaymentActor(Long accountId, RoleType roleType, PaymentIntent paymentIntent) {
        if (roleType == RoleType.ADMIN || paymentIntent.getPayerAccountId().equals(accountId)) {
            return;
        }
        throw new FinancialAccessException("Only payer can perform this payment action");
    }

    private void ensureSettlementPayable(Settlement settlement) {
        if (settlement.getSettlementState() != SettlementState.SETTLEMENT_PENDING) {
            throw new SettlementNotPayableException("Settlement is not pending");
        }
        if (!settlement.getExpiresAt().isAfter(Instant.now())) {
            throw new SettlementNotPayableException("Settlement is expired");
        }
    }

    private void ensureRepaymentInstallmentPayable(RepaymentInstallment installment) {
        if (installment.getInstallmentState() != RepaymentInstallmentState.NOT_STARTED
                && installment.getInstallmentState() != RepaymentInstallmentState.PAYMENT_FAILED
                && installment.getInstallmentState() != RepaymentInstallmentState.OVERDUE) {
            throw new RepaymentInstallmentNotPayableException("Repayment installment is not payable");
        }
    }

    private void ensurePaymentIntentActive(PaymentIntent paymentIntent) {
        if (paymentIntent.getPaymentState() != PaymentState.CREATED
                && paymentIntent.getPaymentState() != PaymentState.PAYMENT_PENDING) {
            throw paymentIntentNotActiveException(paymentIntent);
        }
        if (!paymentIntent.getExpiresAt().isAfter(Instant.now())) {
            throw new PaymentIntentExpiredException("Payment intent is expired");
        }
    }

    private ApiException paymentIntentNotActiveException(Long paymentIntentId) {
        return paymentIntentNotActiveException(getPaymentIntent(paymentIntentId));
    }

    private ApiException paymentIntentNotActiveException(PaymentIntent paymentIntent) {
        if (paymentIntent.getPaymentState() == PaymentState.PAYMENT_CONFIRMED) {
            return new PaymentAlreadyConfirmedException("Payment is already confirmed");
        }
        if (paymentIntent.getPaymentState() == PaymentState.PAYMENT_EXPIRED
                || !paymentIntent.getExpiresAt().isAfter(Instant.now())) {
            return new PaymentIntentExpiredException("Payment intent is expired");
        }
        return new PaymentIntentNotActiveException("Payment intent is not active");
    }

    private void ensureProviderMethodEnabled(String providerCode, PaymentMethodType methodType, String currencyCode) {
        if (!paymentProviderMethodRepository.existsByProviderCodeAndMethodTypeAndCurrencyCodeAndEnabledTrue(
                providerCode,
                methodType,
                currencyCode
        )) {
            throw new UnsupportedPaymentMethodException("Provider does not support this method/currency combination");
        }
    }

    private void ensureLocalAttempt(PaymentAttempt attempt) {
        ensureProviderAttempt(attempt, LocalPaymentStrategy.PROVIDER_CODE);
    }

    private void ensureProviderAttempt(PaymentAttempt attempt, String providerCode) {
        if (!LocalPaymentStrategy.PROVIDER_CODE.equalsIgnoreCase(attempt.getProviderCode())) {
            if (LocalPaymentStrategy.PROVIDER_CODE.equalsIgnoreCase(providerCode)) {
                throw new UnsupportedPaymentMethodException("Only LOCAL payment attempts can use this local endpoint");
            }
        }
        if (!attempt.getProviderCode().equalsIgnoreCase(providerCode)) {
            throw new UnsupportedPaymentMethodException("Payment attempt does not belong to this provider");
        }
    }

    private void ensureAlreadyConfirmedBySameIntent(Settlement settlement, Long paymentIntentId) {
        Settlement latestSettlement = getSettlement(settlement.getSettlementId());
        if (latestSettlement.getSettlementState() == SettlementState.SETTLEMENT_CONFIRMED
                && paymentIntentId.equals(latestSettlement.getConfirmedPaymentIntentId())) {
            return;
        }
        throw new SettlementNotPayableException("Settlement is not pending");
    }

    private void ensureAlreadyConfirmedBySameIntent(RepaymentInstallment installment, Long paymentIntentId) {
        RepaymentInstallment latestInstallment = getRepaymentInstallment(installment.getRepaymentInstallmentId());
        if (latestInstallment.getInstallmentState() == RepaymentInstallmentState.PAID
                && paymentIntentId.equals(latestInstallment.getConfirmedPaymentIntentId())) {
            return;
        }
        throw new RepaymentInstallmentNotPayableException("Repayment installment is not payable");
    }

    private String normalizeProviderCode(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            return LocalPaymentStrategy.PROVIDER_CODE;
        }
        return providerCode.trim().toUpperCase();
    }

    private Settlement getSettlement(Long settlementId) {
        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new SettlementNotFoundException("Settlement not found"));
    }

    private Repayment getRepayment(Long repaymentId) {
        return repaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new RepaymentNotFoundException("Repayment not found"));
    }

    private RepaymentInstallment getRepaymentInstallment(Long installmentId) {
        return repaymentInstallmentRepository.findById(installmentId)
                .orElseThrow(() -> new RepaymentInstallmentNotFoundException("Repayment installment not found"));
    }

    private PaymentIntent getPaymentIntent(Long paymentIntentId) {
        return paymentIntentRepository.findById(paymentIntentId)
                .orElseThrow(() -> new PaymentIntentNotFoundException("Payment intent not found"));
    }

    private PaymentAttempt getPaymentAttempt(Long paymentAttemptId) {
        return paymentAttemptRepository.findById(paymentAttemptId)
                .orElseThrow(() -> new PaymentAttemptNotFoundException("Payment attempt not found"));
    }

    private Agreement getAgreement(Long agreementId) {
        return agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalStateException("Agreement not found for finance record"));
    }

    private Startup getStartupByAccount(Long accountId) {
        return startupRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ParticipationNotFoundException("Startup not found for this account"));
    }

    private Startup getStartupById(Long startupId) {
        return startupRepository.findById(startupId)
                .orElseThrow(() -> new ParticipationNotFoundException("Startup not found"));
    }

    private Investor getInvestorByAccount(Long accountId) {
        return investorRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ParticipationNotFoundException("Investor not found for this account"));
    }

    private Investor getInvestorById(Long investorId) {
        return investorRepository.findById(investorId)
                .orElseThrow(() -> new ParticipationNotFoundException("Investor not found"));
    }

    private void ensureRole(RoleType actualRole, RoleType expectedRole) {
        if (actualRole != expectedRole) {
            throw new FinancialAccessException("Role is not allowed to perform this finance operation");
        }
    }

    private void applySettlementTransition(Runnable transition) {
        try {
            transition.run();
        } catch (IllegalStateException exception) {
            throw new InvalidSettlementStateException(exception.getMessage());
        }
    }

    private void applyRepaymentTransition(Runnable transition) {
        try {
            transition.run();
        } catch (IllegalStateException exception) {
            throw new InvalidRepaymentStateException(exception.getMessage());
        }
    }

    private void applyPaymentIntentTransition(Runnable transition) {
        try {
            transition.run();
        } catch (IllegalStateException exception) {
            throw new InvalidPaymentStateException(exception.getMessage());
        }
    }

    private PaymentAttempt applyPaymentAttemptTransition(PaymentAttemptTransition transition) {
        try {
            return transition.apply();
        } catch (IllegalStateException exception) {
            throw new InvalidPaymentStateException(exception.getMessage());
        }
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 1) - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Pageable installmentPageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 1) - 1, size, Sort.by(Sort.Direction.ASC, "installmentNumber"));
    }

    private Pageable installmentDuePageRequest(int page, int size) {
        return PageRequest.of(
                Math.max(page, 1) - 1,
                size,
                Sort.by(Sort.Direction.ASC, "dueAt")
                        .and(Sort.by(Sort.Direction.ASC, "repaymentInstallmentId"))
        );
    }

    private Collection<RepaymentInstallmentState> resolveInstallmentStates(RepaymentInstallmentState installmentState,
                                                                           RepaymentInstallmentPaymentView paymentView) {
        if (installmentState != null && paymentView != null) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Use either installmentState or paymentView, not both"
            );
        }
        if (installmentState != null) {
            return java.util.List.of(installmentState);
        }
        if (paymentView != null) {
            return paymentView.states();
        }
        return java.util.List.of();
    }

    private <T> PageResponse<T> toPageResponse(Page<T> pageData, int page, int size) {
        return new PageResponse<>(
                pageData.getContent(),
                Math.max(page, 1),
                size,
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
    }

    private SettlementResponse toSettlementResponse(Settlement settlement) {
        return toSettlementResponse(settlement, getAgreement(settlement.getAgreementId()));
    }

    private SettlementResponse toSettlementResponse(Settlement settlement, Agreement agreement) {
        return new SettlementResponse(
                settlement.getSettlementId(),
                settlement.getAgreementId(),
                settlement.getStartupId(),
                settlement.getInvestorId(),
                settlement.getAmount(),
                settlement.getCurrencyCode(),
                toDebtTermsResponse(agreement.getDebtTerms()),
                settlement.getSettlementState(),
                settlement.getCreatedAt(),
                settlement.getExpiresAt(),
                settlement.getConfirmedAt(),
                settlement.getFailedAt(),
                settlement.getExpiredAt(),
                settlement.getCancelledAt(),
                settlement.getFailureReason(),
                settlement.getConfirmedPaymentIntentId()
        );
    }

    private RepaymentResponse toRepaymentResponse(Repayment repayment) {
        Agreement agreement = getAgreement(repayment.getAgreementId());
        return new RepaymentResponse(
                repayment.getRepaymentId(),
                repayment.getAgreementId(),
                repayment.getStartupId(),
                repayment.getInvestorId(),
                repayment.getTotalRepayableAmount(),
                repayment.getCurrencyCode(),
                repayment.getTotalInstallments(),
                repayment.getRepaymentPlanType(),
                toDebtTermsResponse(agreement.getDebtTerms()),
                repayment.getRepaymentState(),
                repayment.getStartedAt(),
                repayment.getFinalDueAt(),
                repayment.getCreatedAt(),
                repayment.getCancelledAt(),
                repayment.getCompletedAt(),
                repayment.getUpdatedAt()
        );
    }

    private RepaymentInstallmentResponse toRepaymentInstallmentResponse(RepaymentInstallment installment) {
        return new RepaymentInstallmentResponse(
                installment.getRepaymentInstallmentId(),
                installment.getRepaymentId(),
                installment.getInstallmentNumber(),
                installment.getAmount(),
                installment.getCurrencyCode(),
                installment.getDueAt(),
                installment.getInstallmentState(),
                installment.getPaymentStartedAt(),
                installment.getPaidAt(),
                installment.getFailedAt(),
                installment.getOverdueAt(),
                installment.getCancelledAt(),
                installment.getFailureReason(),
                installment.getConfirmedPaymentIntentId(),
                installment.getCreatedAt(),
                installment.getUpdatedAt()
        );
    }

    private RepaymentProgressResponse toRepaymentProgressResponse(Agreement agreement, RepaymentProgress progress) {
        return new RepaymentProgressResponse(
                agreement.getAgreementId(),
                progress.repaymentId(),
                agreement.getStartupId(),
                agreement.getInvestorId(),
                toInteger(progress.totalInstallments()),
                toInteger(progress.paidInstallments()),
                toInteger(progress.unpaidInstallments()),
                toInteger(progress.failedInstallments()),
                toInteger(progress.overdueInstallments()),
                toInteger(progress.cancelledInstallments()),
                progress.totalAmount(),
                progress.paidAmount(),
                progress.remainingAmount(),
                progress.currencyCode(),
                progress.repaymentState(),
                toDebtTermsResponse(agreement.getDebtTerms()),
                progress.nextInstallmentId(),
                progress.nextInstallmentNumber(),
                progress.nextDueAt()
        );
    }

    private RepaymentProgressResponse emptyRepaymentProgressResponse(Agreement agreement) {
        String currencyCode = settlementRepository.findByAgreementId(agreement.getAgreementId())
                .map(Settlement::getCurrencyCode)
                .orElse(null);
        return new RepaymentProgressResponse(
                agreement.getAgreementId(),
                null,
                agreement.getStartupId(),
                agreement.getInvestorId(),
                0,
                0,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                currencyCode,
                RepaymentState.NOT_STARTED,
                toDebtTermsResponse(agreement.getDebtTerms()),
                null,
                null,
                null
        );
    }

    private int toInteger(long value) {
        return Math.toIntExact(value);
    }

    private DebtTermsResponse toDebtTermsResponse(AgreementDebtTerms debtTerms) {
        return new DebtTermsResponse(
                debtTerms.getPrincipalAmount(),
                debtTerms.getInterestRate(),
                debtTerms.getTenureMonths(),
                debtTerms.getRepaymentPlanType(),
                debtTerms.getOneTimeRepaymentDueAfterMonths()
        );
    }

    private PaymentIntentResponse toPaymentIntentResponse(PaymentIntent paymentIntent) {
        return new PaymentIntentResponse(
                paymentIntent.getPaymentIntentId(),
                paymentIntent.getPaymentPurpose(),
                paymentIntent.getSettlementId(),
                paymentIntent.getRepaymentInstallmentId(),
                paymentIntent.getPayerAccountId(),
                paymentIntent.getPayeeAccountId(),
                paymentIntent.getAmount(),
                paymentIntent.getCurrencyCode(),
                paymentIntent.getPaymentState(),
                paymentIntent.getCreatedAt(),
                paymentIntent.getExpiresAt(),
                paymentIntent.getConfirmedAt(),
                paymentIntent.getFailedAt(),
                paymentIntent.getExpiredAt(),
                paymentIntent.getCancelledAt(),
                paymentIntent.getFailureCode(),
                paymentIntent.getFailureMessage()
        );
    }

    private PaymentAttemptResponse toPaymentAttemptResponse(PaymentAttempt paymentAttempt) {
        return new PaymentAttemptResponse(
                paymentAttempt.getPaymentAttemptId(),
                paymentAttempt.getPaymentIntentId(),
                paymentAttempt.getProviderCode(),
                paymentAttempt.getMethodType(),
                paymentAttempt.getProviderOrderId(),
                paymentAttempt.getProviderPaymentId(),
                paymentAttempt.getProviderReferenceId(),
                paymentAttempt.getAttemptState(),
                paymentAttempt.getCreatedAt(),
                paymentAttempt.getInitiatedAt(),
                paymentAttempt.getConfirmedAt(),
                paymentAttempt.getFailedAt(),
                paymentAttempt.getFailureCode(),
                paymentAttempt.getFailureMessage(),
                paymentAttempt.getProviderPayload()
        );
    }

    @FunctionalInterface
    private interface PaymentAttemptTransition {
        PaymentAttempt apply();
    }
}

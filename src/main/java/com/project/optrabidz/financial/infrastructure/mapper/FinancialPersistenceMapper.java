package com.project.optrabidz.financial.infrastructure.mapper;

import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.Repayment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallment;
import com.project.optrabidz.financial.domain.model.Settlement;
import org.springframework.stereotype.Component;

@Component
public class FinancialPersistenceMapper {
    public com.project.optrabidz.financial.infrastructure.entity.Settlement toEntity(Settlement settlement) {
        com.project.optrabidz.financial.infrastructure.entity.Settlement entity =
                new com.project.optrabidz.financial.infrastructure.entity.Settlement();
        entity.setSettlementId(settlement.getSettlementId());
        entity.setAgreementId(settlement.getAgreementId());
        entity.setStartupId(settlement.getStartupId());
        entity.setInvestorId(settlement.getInvestorId());
        entity.setAmount(settlement.getAmount());
        entity.setCurrencyCode(settlement.getCurrencyCode());
        entity.setSettlementState(settlement.getSettlementState());
        entity.setCreatedAt(settlement.getCreatedAt());
        entity.setExpiresAt(settlement.getExpiresAt());
        entity.setConfirmedAt(settlement.getConfirmedAt());
        entity.setFailedAt(settlement.getFailedAt());
        entity.setExpiredAt(settlement.getExpiredAt());
        entity.setCancelledAt(settlement.getCancelledAt());
        entity.setFailureReason(settlement.getFailureReason());
        entity.setConfirmedPaymentIntentId(settlement.getConfirmedPaymentIntentId());
        entity.setPspReferenceId(settlement.getPspReferenceId());
        return entity;
    }

    public Settlement toDomain(com.project.optrabidz.financial.infrastructure.entity.Settlement entity) {
        return Settlement.builder()
                .settlementId(entity.getSettlementId())
                .agreementId(entity.getAgreementId())
                .startupId(entity.getStartupId())
                .investorId(entity.getInvestorId())
                .amount(entity.getAmount())
                .currencyCode(entity.getCurrencyCode())
                .settlementState(entity.getSettlementState())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .confirmedAt(entity.getConfirmedAt())
                .failedAt(entity.getFailedAt())
                .expiredAt(entity.getExpiredAt())
                .cancelledAt(entity.getCancelledAt())
                .failureReason(entity.getFailureReason())
                .confirmedPaymentIntentId(entity.getConfirmedPaymentIntentId())
                .pspReferenceId(entity.getPspReferenceId())
                .build();
    }

    public com.project.optrabidz.financial.infrastructure.entity.Repayment toEntity(Repayment repayment) {
        com.project.optrabidz.financial.infrastructure.entity.Repayment entity =
                new com.project.optrabidz.financial.infrastructure.entity.Repayment();
        entity.setRepaymentId(repayment.getRepaymentId());
        entity.setAgreementId(repayment.getAgreementId());
        entity.setStartupId(repayment.getStartupId());
        entity.setInvestorId(repayment.getInvestorId());
        entity.setTotalRepayableAmount(repayment.getTotalRepayableAmount());
        entity.setCurrencyCode(repayment.getCurrencyCode());
        entity.setTotalInstallments(repayment.getTotalInstallments());
        entity.setRepaymentPlanType(repayment.getRepaymentPlanType());
        entity.setStartedAt(repayment.getStartedAt());
        entity.setFinalDueAt(repayment.getFinalDueAt());
        entity.setCreatedAt(repayment.getCreatedAt());
        entity.setRepaymentState(repayment.getRepaymentState());
        entity.setCompletedAt(repayment.getCompletedAt());
        entity.setCancelledAt(repayment.getCancelledAt());
        entity.setUpdatedAt(repayment.getUpdatedAt());
        return entity;
    }

    public Repayment toDomain(com.project.optrabidz.financial.infrastructure.entity.Repayment entity) {
        return Repayment.builder()
                .repaymentId(entity.getRepaymentId())
                .agreementId(entity.getAgreementId())
                .startupId(entity.getStartupId())
                .investorId(entity.getInvestorId())
                .totalRepayableAmount(entity.getTotalRepayableAmount())
                .currencyCode(entity.getCurrencyCode())
                .totalInstallments(entity.getTotalInstallments())
                .repaymentPlanType(entity.getRepaymentPlanType())
                .startedAt(entity.getStartedAt())
                .finalDueAt(entity.getFinalDueAt())
                .createdAt(entity.getCreatedAt())
                .repaymentState(entity.getRepaymentState())
                .completedAt(entity.getCompletedAt())
                .cancelledAt(entity.getCancelledAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public com.project.optrabidz.financial.infrastructure.entity.RepaymentInstallment toEntity(RepaymentInstallment installment) {
        com.project.optrabidz.financial.infrastructure.entity.RepaymentInstallment entity =
                new com.project.optrabidz.financial.infrastructure.entity.RepaymentInstallment();
        entity.setRepaymentInstallmentId(installment.getRepaymentInstallmentId());
        entity.setRepaymentId(installment.getRepaymentId());
        entity.setInstallmentNumber(installment.getInstallmentNumber());
        entity.setInstallmentState(installment.getInstallmentState());
        entity.setAmount(installment.getAmount());
        entity.setCurrencyCode(installment.getCurrencyCode());
        entity.setDueAt(installment.getDueAt());
        entity.setPaymentStartedAt(installment.getPaymentStartedAt());
        entity.setPaidAt(installment.getPaidAt());
        entity.setFailedAt(installment.getFailedAt());
        entity.setOverdueAt(installment.getOverdueAt());
        entity.setCancelledAt(installment.getCancelledAt());
        entity.setFailureReason(installment.getFailureReason());
        entity.setConfirmedPaymentIntentId(installment.getConfirmedPaymentIntentId());
        entity.setCreatedAt(installment.getCreatedAt());
        entity.setUpdatedAt(installment.getUpdatedAt());
        return entity;
    }

    public RepaymentInstallment toDomain(com.project.optrabidz.financial.infrastructure.entity.RepaymentInstallment entity) {
        return RepaymentInstallment.builder()
                .repaymentInstallmentId(entity.getRepaymentInstallmentId())
                .repaymentId(entity.getRepaymentId())
                .installmentNumber(entity.getInstallmentNumber())
                .installmentState(entity.getInstallmentState())
                .amount(entity.getAmount())
                .currencyCode(entity.getCurrencyCode())
                .dueAt(entity.getDueAt())
                .paymentStartedAt(entity.getPaymentStartedAt())
                .paidAt(entity.getPaidAt())
                .failedAt(entity.getFailedAt())
                .overdueAt(entity.getOverdueAt())
                .cancelledAt(entity.getCancelledAt())
                .failureReason(entity.getFailureReason())
                .confirmedPaymentIntentId(entity.getConfirmedPaymentIntentId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public com.project.optrabidz.financial.infrastructure.entity.PaymentIntent toEntity(PaymentIntent paymentIntent) {
        com.project.optrabidz.financial.infrastructure.entity.PaymentIntent entity =
                new com.project.optrabidz.financial.infrastructure.entity.PaymentIntent();
        entity.setPaymentIntentId(paymentIntent.getPaymentIntentId());
        entity.setPaymentPurpose(paymentIntent.getPaymentPurpose());
        entity.setSettlementId(paymentIntent.getSettlementId());
        entity.setRepaymentInstallmentId(paymentIntent.getRepaymentInstallmentId());
        entity.setPayerAccountId(paymentIntent.getPayerAccountId());
        entity.setPayeeAccountId(paymentIntent.getPayeeAccountId());
        entity.setAmount(paymentIntent.getAmount());
        entity.setCurrencyCode(paymentIntent.getCurrencyCode());
        entity.setPaymentState(paymentIntent.getPaymentState());
        entity.setIdempotencyKey(paymentIntent.getIdempotencyKey());
        entity.setCreatedAt(paymentIntent.getCreatedAt());
        entity.setExpiresAt(paymentIntent.getExpiresAt());
        entity.setConfirmedAt(paymentIntent.getConfirmedAt());
        entity.setFailedAt(paymentIntent.getFailedAt());
        entity.setExpiredAt(paymentIntent.getExpiredAt());
        entity.setCancelledAt(paymentIntent.getCancelledAt());
        entity.setFailureCode(paymentIntent.getFailureCode());
        entity.setFailureMessage(paymentIntent.getFailureMessage());
        return entity;
    }

    public PaymentIntent toDomain(com.project.optrabidz.financial.infrastructure.entity.PaymentIntent entity) {
        return PaymentIntent.builder()
                .paymentIntentId(entity.getPaymentIntentId())
                .paymentPurpose(entity.getPaymentPurpose())
                .settlementId(entity.getSettlementId())
                .repaymentInstallmentId(entity.getRepaymentInstallmentId())
                .payerAccountId(entity.getPayerAccountId())
                .payeeAccountId(entity.getPayeeAccountId())
                .amount(entity.getAmount())
                .currencyCode(entity.getCurrencyCode())
                .paymentState(entity.getPaymentState())
                .idempotencyKey(entity.getIdempotencyKey())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .confirmedAt(entity.getConfirmedAt())
                .failedAt(entity.getFailedAt())
                .expiredAt(entity.getExpiredAt())
                .cancelledAt(entity.getCancelledAt())
                .failureCode(entity.getFailureCode())
                .failureMessage(entity.getFailureMessage())
                .build();
    }

    public com.project.optrabidz.financial.infrastructure.entity.PaymentAttempt toEntity(PaymentAttempt paymentAttempt) {
        com.project.optrabidz.financial.infrastructure.entity.PaymentAttempt entity =
                new com.project.optrabidz.financial.infrastructure.entity.PaymentAttempt();
        entity.setPaymentAttemptId(paymentAttempt.getPaymentAttemptId());
        entity.setPaymentIntentId(paymentAttempt.getPaymentIntentId());
        entity.setProviderCode(paymentAttempt.getProviderCode());
        entity.setMethodType(paymentAttempt.getMethodType());
        entity.setProviderOrderId(paymentAttempt.getProviderOrderId());
        entity.setProviderPaymentId(paymentAttempt.getProviderPaymentId());
        entity.setProviderReferenceId(paymentAttempt.getProviderReferenceId());
        entity.setAttemptState(paymentAttempt.getAttemptState());
        entity.setCreatedAt(paymentAttempt.getCreatedAt());
        entity.setInitiatedAt(paymentAttempt.getInitiatedAt());
        entity.setConfirmedAt(paymentAttempt.getConfirmedAt());
        entity.setFailedAt(paymentAttempt.getFailedAt());
        entity.setExpiredAt(paymentAttempt.getExpiredAt());
        entity.setCancelledAt(paymentAttempt.getCancelledAt());
        entity.setFailureCode(paymentAttempt.getFailureCode());
        entity.setFailureMessage(paymentAttempt.getFailureMessage());
        entity.setProviderPayload(paymentAttempt.getProviderPayload());
        return entity;
    }

    public PaymentAttempt toDomain(com.project.optrabidz.financial.infrastructure.entity.PaymentAttempt entity) {
        return PaymentAttempt.builder()
                .paymentAttemptId(entity.getPaymentAttemptId())
                .paymentIntentId(entity.getPaymentIntentId())
                .providerCode(entity.getProviderCode())
                .methodType(entity.getMethodType())
                .providerOrderId(entity.getProviderOrderId())
                .providerPaymentId(entity.getProviderPaymentId())
                .providerReferenceId(entity.getProviderReferenceId())
                .attemptState(entity.getAttemptState())
                .createdAt(entity.getCreatedAt())
                .initiatedAt(entity.getInitiatedAt())
                .confirmedAt(entity.getConfirmedAt())
                .failedAt(entity.getFailedAt())
                .expiredAt(entity.getExpiredAt())
                .cancelledAt(entity.getCancelledAt())
                .failureCode(entity.getFailureCode())
                .failureMessage(entity.getFailureMessage())
                .providerPayload(entity.getProviderPayload())
                .build();
    }
}

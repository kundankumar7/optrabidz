package com.project.optrabidz.financial.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;

import java.util.List;

public final class FinancialErrors {
    public static final ErrorDescriptor FINANCIAL_OPERATION_NOT_ALLOWED = new ErrorDescriptor(
            "FINANCIAL_OPERATION_NOT_ALLOWED",
            ErrorCategory.AUTHORIZATION,
            "This financial operation is not allowed"
    );

    public static final ErrorDescriptor SETTLEMENT_NOT_FOUND = new ErrorDescriptor(
            "SETTLEMENT_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The requested settlement was not found"
    );

    public static final ErrorDescriptor SETTLEMENT_NOT_PAYABLE = new ErrorDescriptor(
            "SETTLEMENT_NOT_PAYABLE",
            ErrorCategory.CONFLICT,
            "The settlement cannot be paid in its current state"
    );

    public static final ErrorDescriptor SETTLEMENT_STATE_CONFLICT = new ErrorDescriptor(
            "SETTLEMENT_STATE_CONFLICT",
            ErrorCategory.CONFLICT,
            "The settlement state no longer permits this operation"
    );

    public static final ErrorDescriptor REPAYMENT_NOT_FOUND = new ErrorDescriptor(
            "REPAYMENT_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The requested repayment was not found"
    );

    public static final ErrorDescriptor REPAYMENT_INSTALLMENT_NOT_FOUND = new ErrorDescriptor(
            "REPAYMENT_INSTALLMENT_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The requested repayment installment was not found"
    );

    public static final ErrorDescriptor REPAYMENT_INSTALLMENT_NOT_PAYABLE = new ErrorDescriptor(
            "REPAYMENT_INSTALLMENT_NOT_PAYABLE",
            ErrorCategory.CONFLICT,
            "The repayment installment cannot be paid in its current state"
    );

    public static final ErrorDescriptor REPAYMENT_STATE_CONFLICT = new ErrorDescriptor(
            "REPAYMENT_STATE_CONFLICT",
            ErrorCategory.CONFLICT,
            "The repayment state no longer permits this operation"
    );

    public static final ErrorDescriptor PAYMENT_INTENT_NOT_FOUND = new ErrorDescriptor(
            "PAYMENT_INTENT_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The requested payment intent was not found"
    );

    public static final ErrorDescriptor PAYMENT_ATTEMPT_NOT_FOUND = new ErrorDescriptor(
            "PAYMENT_ATTEMPT_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The requested payment attempt was not found"
    );

    public static final ErrorDescriptor PAYMENT_INTENT_EXPIRED = new ErrorDescriptor(
            "PAYMENT_INTENT_EXPIRED",
            ErrorCategory.CONFLICT,
            "The payment intent has expired"
    );

    public static final ErrorDescriptor PAYMENT_INTENT_NOT_ACTIVE = new ErrorDescriptor(
            "PAYMENT_INTENT_NOT_ACTIVE",
            ErrorCategory.CONFLICT,
            "The payment intent is not active"
    );

    public static final ErrorDescriptor PAYMENT_ALREADY_CONFIRMED = new ErrorDescriptor(
            "PAYMENT_ALREADY_CONFIRMED",
            ErrorCategory.CONFLICT,
            "The payment has already been confirmed"
    );

    public static final ErrorDescriptor PAYMENT_STATE_CONFLICT = new ErrorDescriptor(
            "PAYMENT_STATE_CONFLICT",
            ErrorCategory.CONFLICT,
            "The payment state no longer permits this operation"
    );

    public static final ErrorDescriptor PAYMENT_METHOD_UNSUPPORTED = new ErrorDescriptor(
            "PAYMENT_METHOD_UNSUPPORTED",
            ErrorCategory.BUSINESS_RULE,
            "The selected payment method is not supported"
    );

    public static final ErrorDescriptor PAYMENT_PROVIDER_MISMATCH = new ErrorDescriptor(
            "PAYMENT_PROVIDER_MISMATCH",
            ErrorCategory.BUSINESS_RULE,
            "The payment attempt cannot be handled by this provider"
    );

    public static final ErrorDescriptor PAYMENT_WEBHOOK_REJECTED = new ErrorDescriptor(
            "PAYMENT_WEBHOOK_REJECTED",
            ErrorCategory.VALIDATION,
            "The webhook request was rejected"
    );

    public static final ErrorDescriptor PAYMENT_WEBHOOK_PAYLOAD_INVALID = new ErrorDescriptor(
            "PAYMENT_WEBHOOK_PAYLOAD_INVALID",
            ErrorCategory.VALIDATION,
            "The webhook payload is invalid"
    );

    public static final ErrorDescriptor PAYMENT_WEBHOOK_PROCESSING_FAILED = new ErrorDescriptor(
            "PAYMENT_WEBHOOK_PROCESSING_FAILED",
            ErrorCategory.INTERNAL,
            "The webhook could not be processed"
    );

    public static List<ErrorDescriptor> descriptors() {
        return List.of(
                FINANCIAL_OPERATION_NOT_ALLOWED,
                SETTLEMENT_NOT_FOUND,
                SETTLEMENT_NOT_PAYABLE,
                SETTLEMENT_STATE_CONFLICT,
                REPAYMENT_NOT_FOUND,
                REPAYMENT_INSTALLMENT_NOT_FOUND,
                REPAYMENT_INSTALLMENT_NOT_PAYABLE,
                REPAYMENT_STATE_CONFLICT,
                PAYMENT_INTENT_NOT_FOUND,
                PAYMENT_ATTEMPT_NOT_FOUND,
                PAYMENT_INTENT_EXPIRED,
                PAYMENT_INTENT_NOT_ACTIVE,
                PAYMENT_ALREADY_CONFIRMED,
                PAYMENT_STATE_CONFLICT,
                PAYMENT_METHOD_UNSUPPORTED,
                PAYMENT_PROVIDER_MISMATCH,
                PAYMENT_WEBHOOK_REJECTED,
                PAYMENT_WEBHOOK_PAYLOAD_INVALID,
                PAYMENT_WEBHOOK_PROCESSING_FAILED
        );
    }

    private FinancialErrors() {
    }
}

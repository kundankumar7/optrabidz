package com.project.optrabidz.financial.application.command;

import com.project.optrabidz.financial.application.strategy.LocalPaymentStrategy;
import com.project.optrabidz.identity.domain.model.RoleType;
import org.springframework.util.Assert;

public record PaymentAttemptFailureCommand(
        Long paymentAttemptId,
        String providerCode,
        String failureCode,
        String failureMessage,
        Long actorAccountId,
        RoleType actorRole,
        boolean authenticatedActorRequired
) {
    public PaymentAttemptFailureCommand {
        Assert.notNull(paymentAttemptId, "paymentAttemptId must not be null");
        Assert.hasText(providerCode, "providerCode must not be blank");
        Assert.hasText(failureCode, "failureCode must not be blank");
        Assert.hasText(failureMessage, "failureMessage must not be blank");
        providerCode = providerCode.trim().toUpperCase();
        failureCode = failureCode.trim().toUpperCase();
        failureMessage = failureMessage.trim();
        if (authenticatedActorRequired) {
            Assert.notNull(actorAccountId, "actorAccountId must not be null");
            Assert.notNull(actorRole, "actorRole must not be null");
        }
    }

    public static PaymentAttemptFailureCommand authenticatedLocal(Long actorAccountId,
                                                                  RoleType actorRole,
                                                                  Long paymentAttemptId) {
        return new PaymentAttemptFailureCommand(
                paymentAttemptId,
                LocalPaymentStrategy.PROVIDER_CODE,
                "LOCAL_FAILURE",
                "Local payment failure was simulated",
                actorAccountId,
                actorRole,
                true
        );
    }

    public static PaymentAttemptFailureCommand providerCallback(String providerCode,
                                                                Long paymentAttemptId,
                                                                String failureCode,
                                                                String failureMessage) {
        return new PaymentAttemptFailureCommand(
                paymentAttemptId,
                providerCode,
                failureCode,
                failureMessage,
                null,
                null,
                false
        );
    }
}

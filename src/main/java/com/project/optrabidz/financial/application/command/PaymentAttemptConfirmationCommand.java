package com.project.optrabidz.financial.application.command;

import com.project.optrabidz.financial.application.strategy.LocalPaymentStrategy;
import com.project.optrabidz.identity.domain.model.RoleType;
import org.springframework.util.Assert;

public record PaymentAttemptConfirmationCommand(
        Long paymentAttemptId,
        String providerCode,
        String providerPaymentId,
        Long actorAccountId,
        RoleType actorRole,
        boolean authenticatedActorRequired
) {
    public PaymentAttemptConfirmationCommand {
        Assert.notNull(paymentAttemptId, "paymentAttemptId must not be null");
        Assert.hasText(providerCode, "providerCode must not be blank");
        Assert.hasText(providerPaymentId, "providerPaymentId must not be blank");
        if (authenticatedActorRequired) {
            Assert.notNull(actorAccountId, "actorAccountId must not be null");
            Assert.notNull(actorRole, "actorRole must not be null");
        }
    }

    public static PaymentAttemptConfirmationCommand authenticatedLocal(Long actorAccountId,
                                                                       RoleType actorRole,
                                                                       Long paymentAttemptId) {
        return new PaymentAttemptConfirmationCommand(
                paymentAttemptId,
                LocalPaymentStrategy.PROVIDER_CODE,
                "LOCAL-PAYMENT-" + paymentAttemptId,
                actorAccountId,
                actorRole,
                true
        );
    }

    public static PaymentAttemptConfirmationCommand providerCallback(String providerCode,
                                                                     Long paymentAttemptId,
                                                                     String providerPaymentId) {
        return new PaymentAttemptConfirmationCommand(
                paymentAttemptId,
                providerCode,
                providerPaymentId,
                null,
                null,
                false
        );
    }
}

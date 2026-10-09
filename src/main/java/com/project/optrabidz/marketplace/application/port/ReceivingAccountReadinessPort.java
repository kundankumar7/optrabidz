package com.project.optrabidz.marketplace.application.port;

public interface ReceivingAccountReadinessPort {
    boolean requiresVerifiedBinding();

    boolean hasVerifiedBinding(Long accountId);
}

package com.project.optrabidz.marketplace.application.port;

public interface ReceivingAccountReadinessPort {
    boolean hasVerifiedBinding(Long accountId);
}

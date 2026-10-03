package com.project.optrabidz.financial.domain.repository;

import com.project.optrabidz.financial.domain.model.PayoutTransfer;
import com.project.optrabidz.financial.domain.model.PayoutTransferStatus;

import java.time.Instant;
import java.util.Optional;

public interface PayoutTransferRepository {
    PayoutTransfer save(PayoutTransfer payoutTransfer);

    boolean insertIfAbsent(PayoutTransfer payoutTransfer);

    Optional<PayoutTransfer> findById(Long payoutTransferId);

    Optional<PayoutTransfer> findByPaymentIntentId(Long paymentIntentId);

    int claimForProcessing(Long payoutTransferId, PayoutTransferStatus expectedStatus, Instant now);

    int markConfirmed(Long payoutTransferId, String providerReference, Instant now);

    int markFailed(Long payoutTransferId, String failureCode, String failureMessage, Instant now);
}

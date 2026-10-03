package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.domain.model.PayoutTransfer;

public record PayoutTransferEnsureResult(
        PayoutTransfer transfer,
        boolean created
) {
}

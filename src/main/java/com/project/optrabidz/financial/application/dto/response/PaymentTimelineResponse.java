package com.project.optrabidz.financial.application.dto.response;

import java.util.List;

public record PaymentTimelineResponse(
        Long paymentIntentId,
        boolean demonstration,
        List<PaymentTimelineEntryResponse> entries
) {
}

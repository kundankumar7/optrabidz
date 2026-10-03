package com.project.optrabidz.financial.application.dto.response;

import java.time.Instant;

public record PaymentTimelineEntryResponse(
        PaymentTimelineStage stage,
        PaymentTimelineStatus status,
        String label,
        Instant occurredAt,
        Integer attemptCount
) {
}

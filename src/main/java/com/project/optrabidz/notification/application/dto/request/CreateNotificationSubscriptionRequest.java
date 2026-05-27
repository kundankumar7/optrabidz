package com.project.optrabidz.notification.application.dto.request;

import com.project.optrabidz.notification.domain.model.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNotificationSubscriptionRequest(
        @NotNull ChannelType channelType,
        @NotBlank @Size(max = 1000) String endpoint,
        @Size(max = 1000) String publicKey,
        @Size(max = 1000) String authSecret
) {
}

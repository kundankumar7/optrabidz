package com.project.optrabidz.notification.application.channel;

import com.project.optrabidz.notification.domain.model.ChannelType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "optrabidz.notification.channels.email", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SandboxEmailNotificationChannelStrategy implements NotificationChannelStrategy {
    @Override
    public ChannelType channelType() {
        return ChannelType.EMAIL;
    }

    @Override
    public NotificationSendResult send(NotificationDispatchContext context) {
        if (context.endpoint() == null || context.endpoint().isBlank()) {
            return NotificationSendResult.failed(
                    "NO_ACTIVE_EMAIL_SUBSCRIPTION",
                    "No active email subscription endpoint is available for this account.",
                    false
            );
        }
        return NotificationSendResult.delivered("sandbox-email-" + context.deliveryId() + "-" + context.nextAttemptNumber());
    }
}

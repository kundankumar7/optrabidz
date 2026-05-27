package com.project.optrabidz.notification.application.channel;

import com.project.optrabidz.common.observability.OperationalEventLogger;
import org.springframework.stereotype.Component;

@Component
public class NotificationChannelProxy {
    private final OperationalEventLogger operationalEventLogger;

    public NotificationChannelProxy(OperationalEventLogger operationalEventLogger) {
        this.operationalEventLogger = operationalEventLogger;
    }

    public NotificationSendResult send(NotificationChannelStrategy strategy, NotificationDispatchContext context) {
        operationalEventLogger.info(
                "NOTIFICATION_DELIVERY_ATTEMPT",
                "deliveryId=" + context.deliveryId()
                        + " channel=" + context.channelType()
                        + " notificationName=" + context.notificationName()
        );
        try {
            NotificationSendResult result = strategy.send(context);
            if (result.successful()) {
                operationalEventLogger.info(
                        "NOTIFICATION_DELIVERY_SUCCESS",
                        "deliveryId=" + context.deliveryId()
                                + " channel=" + context.channelType()
                                + " providerMessageId=" + result.providerMessageId()
                );
            } else {
                operationalEventLogger.warn(
                        "NOTIFICATION_DELIVERY_RETRYABLE_FAILURE",
                        "deliveryId=" + context.deliveryId()
                                + " channel=" + context.channelType()
                                + " errorCode=" + result.errorCode()
                                + " retryable=" + result.retryable()
                );
            }
            return result;
        } catch (RuntimeException exception) {
            operationalEventLogger.error(
                    "NOTIFICATION_DELIVERY_EXCEPTION",
                    "deliveryId=" + context.deliveryId() + " channel=" + context.channelType(),
                    exception
            );
            return NotificationSendResult.failed(
                    "CHANNEL_EXCEPTION",
                    exception.getMessage(),
                    true
            );
        }
    }
}

package com.project.optrabidz.notification.application.channel;

public record NotificationSendResult(
        boolean successful,
        String providerMessageId,
        String errorCode,
        String errorMessage,
        boolean retryable
) {
    public static NotificationSendResult delivered(String providerMessageId) {
        return new NotificationSendResult(true, providerMessageId, null, null, false);
    }

    public static NotificationSendResult failed(String errorCode, String errorMessage, boolean retryable) {
        return new NotificationSendResult(false, null, errorCode, errorMessage, retryable);
    }
}

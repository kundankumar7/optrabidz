package com.project.optrabidz.financial.application.replay;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

@Component
public final class PaymentWebhookReplayFingerprintFactory {
    private static final int FINGERPRINT_VERSION = 1;

    public PaymentWebhookReplayEvent create(PaymentProviderWebhookCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        PaymentWebhookReplayContent content = new PaymentWebhookReplayContent(
                FINGERPRINT_VERSION,
                command.providerCode(),
                command.providerEventId(),
                command.eventType(),
                command.paymentAttemptId(),
                command.providerPaymentId(),
                command.failureCode(),
                command.failureMessage()
        );
        return new PaymentWebhookReplayEvent(
                content,
                sha256(canonicalBytes(content))
        );
    }

    private byte[] canonicalBytes(PaymentWebhookReplayContent content) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(content.fingerprintVersion());
                writeNullableString(output, content.providerCode());
                writeNullableString(output, content.providerEventId());
                writeNullableString(output, content.eventType().name());
                output.writeLong(content.paymentAttemptId());
                writeNullableString(output, content.providerPaymentId());
                writeNullableString(output, content.providerFailureCode());
                writeNullableString(output, content.providerFailureMessage());
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Payment webhook fingerprint input could not be encoded",
                    exception
            );
        }
    }

    private void writeNullableString(DataOutputStream output, String value)
            throws IOException {
        if (value == null) {
            output.writeInt(-1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable for payment webhook fingerprinting",
                    exception
            );
        }
    }
}

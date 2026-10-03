package com.project.optrabidz.financial.infrastructure.provider.webhook;

import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectedException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectionReason;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Component
public class PaymentWebhookHmac {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    public String sign(String timestamp, byte[] body, String secret) {
        return SIGNATURE_PREFIX + HexFormat.of().formatHex(calculate(timestamp, body, secret));
    }

    byte[] calculate(String timestamp, byte[] body, String secret) {
        try {
            byte[] prefix = (timestamp + ".").getBytes(StandardCharsets.US_ASCII);
            ByteBuffer canonical = ByteBuffer.allocate(prefix.length + body.length);
            canonical.put(prefix);
            canonical.put(body);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(canonical.array());
        } catch (GeneralSecurityException | RuntimeException exception) {
            throw new PaymentWebhookRejectedException(
                    PaymentWebhookRejectionReason.SIGNATURE_INVALID,
                    exception
            );
        }
    }
}

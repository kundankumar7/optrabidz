package com.project.optrabidz.financial.api;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEnvelope;
import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectedException;
import com.project.optrabidz.financial.infrastructure.provider.webhook.PaymentWebhookProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentWebhookHttpRequestReaderTest {
    private static final String TIMESTAMP = "1787553600";
    private static final String SIGNATURE = "sha256=" + "a".repeat(64);

    private PaymentWebhookHttpRequestReader reader;

    @BeforeEach
    void setUp() {
        PaymentWebhookProperties properties = new PaymentWebhookProperties();
        properties.setMaxBodySize(DataSize.ofKilobytes(64));
        reader = new PaymentWebhookHttpRequestReader(properties);
    }

    @Test
    void preservesExactBytesAndOnlyAllowlistedHeaders() {
        byte[] body = "{\"message\":\"नमस्ते\"}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = request(body);
        request.addHeader("Authorization", "Bearer must-not-cross-boundary");
        request.addHeader("Cookie", "JSESSIONID=must-not-cross-boundary");

        PaymentProviderWebhookEnvelope envelope = reader.read(" upi ", request);

        assertThat(envelope.providerCode()).isEqualTo("UPI");
        assertThat(envelope.rawBody()).containsExactly(body);
        assertThat(envelope.timestamp()).isEqualTo(TIMESTAMP);
        assertThat(envelope.signature()).isEqualTo(SIGNATURE);
    }

    @Test
    void rejectsDeclaredOrStreamedBodyAboveLimit() {
        MockHttpServletRequest declared = new MockHttpServletRequest() {
            @Override
            public long getContentLengthLong() {
                return 65_537;
            }
        };
        declared.setContent("{}".getBytes(StandardCharsets.UTF_8));
        addProtocolHeaders(declared);
        MockHttpServletRequest streamed = new MockHttpServletRequest() {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        streamed.setContent(new byte[65_537]);
        addProtocolHeaders(streamed);

        assertRejected(() -> reader.read("UPI", declared));
        assertRejected(() -> reader.read("UPI", streamed));
    }

    @Test
    void rejectsEmptyBodyAndInvalidProviderCode() {
        assertRejected(() -> reader.read("UPI", request(new byte[0])));
        assertRejected(() -> reader.read("../UPI", request("{}".getBytes(StandardCharsets.UTF_8))));
        assertRejected(() -> reader.read("A".repeat(33), request("{}".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void rejectsMissingRepeatedOrOversizedProtocolHeaders() {
        MockHttpServletRequest missing = new MockHttpServletRequest();
        missing.setContent("{}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletRequest repeated = request("{}".getBytes(StandardCharsets.UTF_8));
        repeated.addHeader("X-Payment-Signature", List.of(SIGNATURE, SIGNATURE));

        MockHttpServletRequest oversizedTimestamp = request("{}".getBytes(StandardCharsets.UTF_8));
        oversizedTimestamp.removeHeader("X-Payment-Timestamp");
        oversizedTimestamp.addHeader("X-Payment-Timestamp", "1".repeat(21));

        MockHttpServletRequest oversizedSignature = request("{}".getBytes(StandardCharsets.UTF_8));
        oversizedSignature.removeHeader("X-Payment-Signature");
        oversizedSignature.addHeader("X-Payment-Signature", "a".repeat(81));

        assertRejected(() -> reader.read("UPI", missing));
        assertRejected(() -> reader.read("UPI", repeated));
        assertRejected(() -> reader.read("UPI", oversizedTimestamp));
        assertRejected(() -> reader.read("UPI", oversizedSignature));
    }

    @Test
    void envelopeDefensivelyCopiesBodyBytes() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        PaymentProviderWebhookEnvelope envelope = reader.read("UPI", request(body));

        body[0] = 'X';
        byte[] exposed = envelope.rawBody();
        exposed[0] = 'Y';

        assertThat(envelope.rawBody()).containsExactly("{}".getBytes(StandardCharsets.UTF_8));
    }

    private static MockHttpServletRequest request(byte[] body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(body);
        addProtocolHeaders(request);
        return request;
    }

    private static void addProtocolHeaders(MockHttpServletRequest request) {
        request.addHeader("X-Payment-Timestamp", TIMESTAMP);
        request.addHeader("X-Payment-Signature", SIGNATURE);
    }

    private static void assertRejected(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOf(PaymentWebhookRejectedException.class);
    }
}

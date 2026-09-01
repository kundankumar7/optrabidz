package com.project.optrabidz.financial.api;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;

import static org.springframework.http.HttpStatus.NO_CONTENT;

class PaymentProviderWebhookControllerTest {
    @Test
    void delegatesHttpRequestWithoutAuthenticatingOrParsingInController()
            throws Exception {
        PaymentWebhookHttpIngress ingress = mock(PaymentWebhookHttpIngress.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        PaymentProviderWebhookController controller = new PaymentProviderWebhookController(ingress);

        controller.handleProviderWebhook("upi", request);

        verify(ingress).handle("upi", request);
        Method endpoint = PaymentProviderWebhookController.class.getMethod(
                "handleProviderWebhook",
                String.class,
                jakarta.servlet.http.HttpServletRequest.class
        );
        assertThat(endpoint.getReturnType()).isEqualTo(void.class);
        assertThat(endpoint.getAnnotation(ResponseStatus.class).value())
                .isEqualTo(NO_CONTENT);
    }
}

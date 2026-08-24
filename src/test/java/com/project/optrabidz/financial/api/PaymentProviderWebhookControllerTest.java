package com.project.optrabidz.financial.api;

import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentProviderWebhookControllerTest {
    @Test
    void delegatesHttpRequestWithoutAuthenticatingOrParsingInController() {
        PaymentWebhookHttpIngress ingress = mock(PaymentWebhookHttpIngress.class);
        PaymentAttemptResponse response = mock(PaymentAttemptResponse.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(ingress.handle("upi", request)).thenReturn(response);
        PaymentProviderWebhookController controller = new PaymentProviderWebhookController(ingress);

        assertThat(controller.handleProviderWebhook("upi", request).data()).isSameAs(response);
        verify(ingress).handle("upi", request);
    }
}

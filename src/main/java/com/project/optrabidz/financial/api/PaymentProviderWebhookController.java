package com.project.optrabidz.financial.api;

import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment-providers")
public class PaymentProviderWebhookController {
    private final PaymentWebhookHttpIngress httpIngress;

    public PaymentProviderWebhookController(PaymentWebhookHttpIngress httpIngress) {
        this.httpIngress = httpIngress;
    }

    @PostMapping(
            value = "/{providerCode}/webhooks",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public SuccessResponse<PaymentAttemptResponse> handleProviderWebhook(
            @PathVariable String providerCode,
            HttpServletRequest request) {
        return ApiResponse.success(httpIngress.handle(providerCode, request), request);
    }
}

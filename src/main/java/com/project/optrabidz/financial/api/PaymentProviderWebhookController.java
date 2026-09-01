package com.project.optrabidz.financial.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/payment-providers")
public class PaymentProviderWebhookController {
    private final PaymentWebhookHttpIngress httpIngress;

    public PaymentProviderWebhookController(PaymentWebhookHttpIngress httpIngress) {
        this.httpIngress = httpIngress;
    }

    @PostMapping(
            value = "/{providerCode}/webhooks",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(NO_CONTENT)
    public void handleProviderWebhook(
            @PathVariable String providerCode,
            HttpServletRequest request) {
        httpIngress.handle(providerCode, request);
    }
}

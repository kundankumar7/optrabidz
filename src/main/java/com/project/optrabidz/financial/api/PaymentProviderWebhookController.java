package com.project.optrabidz.financial.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.financial.application.PaymentProviderWebhookService;
import com.project.optrabidz.financial.application.dto.request.PaymentProviderWebhookRequest;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/api/v1/payment-providers")
public class PaymentProviderWebhookController {
    private final PaymentProviderWebhookService webhookService;
    private final ObjectMapper objectMapper;

    public PaymentProviderWebhookController(PaymentProviderWebhookService webhookService,
                                            ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(
            value = "/{providerCode}/webhooks",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public SuccessResponse<PaymentAttemptResponse> handleProviderWebhook(
            @PathVariable String providerCode,
            @RequestBody String rawPayload,
            HttpServletRequest httpRequest) {
        PaymentProviderWebhookRequest request = parse(rawPayload);
        PaymentAttemptResponse response = webhookService.handle(
                request.toCommand(providerCode, rawPayload, headers(httpRequest))
        );
        return ApiResponse.success(response, httpRequest);
    }

    private PaymentProviderWebhookRequest parse(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, PaymentProviderWebhookRequest.class);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Malformed webhook payload", exception);
        }
    }

    private Map<String, String> headers(HttpServletRequest request) {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }
}

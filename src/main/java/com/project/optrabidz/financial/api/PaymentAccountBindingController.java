package com.project.optrabidz.financial.api;

import com.project.optrabidz.financial.application.PaymentAccountBindingService;
import com.project.optrabidz.financial.application.dto.request.CreatePaymentAccountBindingRequest;
import com.project.optrabidz.financial.application.dto.request.ReplacePaymentAccountBindingRequest;
import com.project.optrabidz.financial.application.dto.response.PaymentAccountBindingReadinessResponse;
import com.project.optrabidz.financial.application.dto.response.PaymentAccountBindingResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/payment-account-bindings")
@Profile("demo")
@ConditionalOnProperty(name = "optrabidz.financial.demo-provider.enabled", havingValue = "true")
public class PaymentAccountBindingController {
    private final PaymentAccountBindingService service;

    public PaymentAccountBindingController(PaymentAccountBindingService service) {
        this.service = service;
    }

    @PostMapping
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Payment account binding created",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = PaymentAccountBindingResponse.class)
            ),
            headers = @io.swagger.v3.oas.annotations.headers.Header(
                    name = "Location",
                    description = "URI of the created payment account binding",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "uri")
            )
    )
    public ResponseEntity<PaymentAccountBindingResponse> create(
            @Valid @RequestBody CreatePaymentAccountBindingRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        PaymentAccountBindingResponse response = service.create(
                principal.getAccountId(), principal.getRole(), request);
        return created(response);
    }

    @PostMapping("/{bindingId}/actions/replace")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Replacement payment account binding created",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = PaymentAccountBindingResponse.class)
            ),
            headers = @io.swagger.v3.oas.annotations.headers.Header(
                    name = "Location",
                    description = "URI of the replacement payment account binding",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "uri")
            )
    )
    public ResponseEntity<PaymentAccountBindingResponse> replace(
            @PathVariable Long bindingId,
            @Valid @RequestBody ReplacePaymentAccountBindingRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        PaymentAccountBindingResponse response = service.replace(
                principal.getAccountId(), principal.getRole(), bindingId, request);
        return created(response);
    }

    @GetMapping("/current")
    public PaymentAccountBindingReadinessResponse getCurrent(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return service.getCurrent(principal.getAccountId(), principal.getRole());
    }

    @GetMapping("/{bindingId}")
    public PaymentAccountBindingResponse getById(
            @PathVariable Long bindingId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return service.getById(principal.getAccountId(), principal.getRole(), bindingId);
    }

    @PostMapping("/{bindingId}/actions/verify")
    public PaymentAccountBindingResponse verify(
            @PathVariable Long bindingId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return service.verify(principal.getAccountId(), principal.getRole(), bindingId);
    }

    @PostMapping("/{bindingId}/actions/deactivate")
    public PaymentAccountBindingResponse deactivate(
            @PathVariable Long bindingId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return service.deactivate(principal.getAccountId(), principal.getRole(), bindingId);
    }

    private ResponseEntity<PaymentAccountBindingResponse> created(
            PaymentAccountBindingResponse response
    ) {
        return ResponseEntity.created(URI.create(
                "/api/v1/payment-account-bindings/" + response.paymentAccountBindingId()
        )).body(response);
    }
}

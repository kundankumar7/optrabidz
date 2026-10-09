package com.project.optrabidz.financial.api;

import com.project.optrabidz.financial.application.DemoPaymentService;
import com.project.optrabidz.financial.application.dto.request.DemoPaymentOutcomeRequest;
import com.project.optrabidz.financial.application.dto.response.DemoCheckoutResponse;
import com.project.optrabidz.financial.application.dto.response.DemoPaymentOutcomeResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment-attempts")
@Profile("demo")
@ConditionalOnProperty(name = "optrabidz.financial.demo-provider.enabled", havingValue = "true")
public class DemoPaymentController {
    private final DemoPaymentService demoPaymentService;

    public DemoPaymentController(DemoPaymentService demoPaymentService) {
        this.demoPaymentService = demoPaymentService;
    }

    @GetMapping("/{paymentAttemptId}/demo-checkout")
    public DemoCheckoutResponse getCheckout(
            @PathVariable Long paymentAttemptId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return demoPaymentService.getCheckout(principal.getAccountId(), paymentAttemptId);
    }

    @PostMapping("/{paymentAttemptId}/actions/demo-outcome")
    public DemoPaymentOutcomeResponse processOutcome(
            @PathVariable Long paymentAttemptId,
            @Valid @RequestBody DemoPaymentOutcomeRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return demoPaymentService.processOutcome(principal.getAccountId(), paymentAttemptId, request);
    }
}

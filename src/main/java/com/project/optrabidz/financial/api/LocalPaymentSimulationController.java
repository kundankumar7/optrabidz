package com.project.optrabidz.financial.api;

import com.project.optrabidz.financial.application.FinancialService;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "optrabidz.financial.local-provider.enabled", havingValue = "true")
public class LocalPaymentSimulationController {
    private final FinancialService financialService;

    public LocalPaymentSimulationController(FinancialService financialService) {
        this.financialService = financialService;
    }

    @PostMapping("/payment-attempts/{paymentAttemptId}/actions/local-confirm")
    public PaymentAttemptResponse confirmLocalPaymentAttempt(
            @PathVariable Long paymentAttemptId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return financialService.confirmLocalPaymentAttempt(
                principal.getAccountId(), principal.getRole(), paymentAttemptId);
    }

    @PostMapping("/payment-attempts/{paymentAttemptId}/actions/local-fail")
    public PaymentAttemptResponse failLocalPaymentAttempt(
            @PathVariable Long paymentAttemptId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return financialService.failLocalPaymentAttempt(
                principal.getAccountId(), principal.getRole(), paymentAttemptId);
    }
}

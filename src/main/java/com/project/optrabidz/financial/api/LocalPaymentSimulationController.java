package com.project.optrabidz.financial.api;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.financial.application.FinancialService;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "optrabidz.financial.local-provider.enabled", havingValue = "true")
public class LocalPaymentSimulationController {
    private final FinancialService financialService;

    public LocalPaymentSimulationController(FinancialService financialService) {
        this.financialService = financialService;
    }

    @PostMapping("/payment-attempts/{paymentAttemptId}/actions/local-confirm")
    public SuccessResponse<PaymentAttemptResponse> confirmLocalPaymentAttempt(
            @PathVariable Long paymentAttemptId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.confirmLocalPaymentAttempt(user.getAccountId(), user.getRole(), paymentAttemptId),
                httpRequest
        );
    }

    @PostMapping("/payment-attempts/{paymentAttemptId}/actions/local-fail")
    public SuccessResponse<PaymentAttemptResponse> failLocalPaymentAttempt(
            @PathVariable Long paymentAttemptId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.failLocalPaymentAttempt(user.getAccountId(), user.getRole(), paymentAttemptId),
                httpRequest
        );
    }

    private AuthenticatedUserPrincipal requirePrincipal(AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ErrorCode.AUTHENTICATION_REQUIRED, "Authentication is required");
        }
        return principal;
    }
}

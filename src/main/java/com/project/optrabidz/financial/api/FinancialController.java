package com.project.optrabidz.financial.api;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.financial.application.FinancialService;
import com.project.optrabidz.financial.application.dto.request.CreatePaymentAttemptRequest;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.financial.application.dto.response.PaymentIntentResponse;
import com.project.optrabidz.financial.application.dto.response.RepaymentInstallmentResponse;
import com.project.optrabidz.financial.application.dto.response.RepaymentProgressResponse;
import com.project.optrabidz.financial.application.dto.response.RepaymentResponse;
import com.project.optrabidz.financial.application.dto.response.SettlementResponse;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentPaymentView;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class FinancialController {
    private final FinancialService financialService;

    public FinancialController(FinancialService financialService) {
        this.financialService = financialService;
    }

    @GetMapping("/settlements/{settlementId}")
    public SuccessResponse<SettlementResponse> getSettlement(@PathVariable Long settlementId,
                                                             @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                             HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.getSettlement(user.getAccountId(), user.getRole(), settlementId),
                httpRequest
        );
    }

    @GetMapping("/investors/me/settlements")
    public SuccessResponse<PageResponse<SettlementResponse>> getMyInvestorSettlements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.getMyInvestorSettlements(user.getAccountId(), user.getRole(), page, size),
                httpRequest
        );
    }

    @GetMapping("/startups/me/settlements")
    public SuccessResponse<PageResponse<SettlementResponse>> getMyStartupSettlements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.getMyStartupSettlements(user.getAccountId(), user.getRole(), page, size),
                httpRequest
        );
    }

    @PostMapping("/settlements/{settlementId}/payment-intents")
    public SuccessResponse<PaymentIntentResponse> createSettlementPaymentIntent(
            @PathVariable Long settlementId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.createSettlementPaymentIntent(user.getAccountId(), user.getRole(), settlementId),
                httpRequest
        );
    }

    @GetMapping("/repayments/{repaymentId}")
    public SuccessResponse<RepaymentResponse> getRepayment(@PathVariable Long repaymentId,
                                                           @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                           HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.getRepayment(user.getAccountId(), user.getRole(), repaymentId),
                httpRequest
        );
    }

    @GetMapping("/repayments/{repaymentId}/installments")
    public SuccessResponse<PageResponse<RepaymentInstallmentResponse>> getRepaymentInstallments(
            @PathVariable Long repaymentId,
            @RequestParam(required = false) RepaymentInstallmentState installmentState,
            @RequestParam(required = false) RepaymentInstallmentPaymentView paymentView,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.getRepaymentInstallments(user.getAccountId(), user.getRole(), repaymentId, installmentState, paymentView, page, size),
                httpRequest
        );
    }

    @GetMapping("/repayment-installments/{installmentId}")
    public SuccessResponse<RepaymentInstallmentResponse> getRepaymentInstallment(
            @PathVariable Long installmentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.getRepaymentInstallment(user.getAccountId(), user.getRole(), installmentId),
                httpRequest
        );
    }

    @GetMapping("/agreements/{agreementId}/repayment-progress")
    public SuccessResponse<RepaymentProgressResponse> getRepaymentProgress(
            @PathVariable Long agreementId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.getRepaymentProgress(user.getAccountId(), user.getRole(), agreementId),
                httpRequest
        );
    }

    @GetMapping("/investors/me/repayments")
    public SuccessResponse<PageResponse<RepaymentResponse>> getMyInvestorRepayments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.getMyInvestorRepayments(user.getAccountId(), user.getRole(), page, size),
                httpRequest
        );
    }

    @GetMapping("/investors/me/repayment-installments")
    public SuccessResponse<PageResponse<RepaymentInstallmentResponse>> getMyInvestorRepaymentInstallments(
            @RequestParam(required = false) RepaymentInstallmentState installmentState,
            @RequestParam(required = false) RepaymentInstallmentPaymentView paymentView,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.getMyInvestorRepaymentInstallments(user.getAccountId(), user.getRole(), installmentState, paymentView, page, size),
                httpRequest
        );
    }

    @GetMapping("/startups/me/repayments")
    public SuccessResponse<PageResponse<RepaymentResponse>> getMyStartupRepayments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.getMyStartupRepayments(user.getAccountId(), user.getRole(), page, size),
                httpRequest
        );
    }

    @GetMapping("/startups/me/repayment-installments")
    public SuccessResponse<PageResponse<RepaymentInstallmentResponse>> getMyStartupRepaymentInstallments(
            @RequestParam(required = false) RepaymentInstallmentState installmentState,
            @RequestParam(required = false) RepaymentInstallmentPaymentView paymentView,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.getMyStartupRepaymentInstallments(user.getAccountId(), user.getRole(), installmentState, paymentView, page, size),
                httpRequest
        );
    }

    @PostMapping("/repayments/{repaymentId}/payment-intents")
    public SuccessResponse<PaymentIntentResponse> createRepaymentPaymentIntent(
            @PathVariable Long repaymentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.createRepaymentPaymentIntent(user.getAccountId(), user.getRole(), repaymentId),
                httpRequest
        );
    }

    @PostMapping("/repayment-installments/{installmentId}/payment-intents")
    public SuccessResponse<PaymentIntentResponse> createRepaymentInstallmentPaymentIntent(
            @PathVariable Long installmentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.createRepaymentInstallmentPaymentIntent(user.getAccountId(), user.getRole(), installmentId),
                httpRequest
        );
    }

    @GetMapping("/payment-intents/{paymentIntentId}")
    public SuccessResponse<PaymentIntentResponse> getPaymentIntent(@PathVariable Long paymentIntentId,
                                                                   @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                                   HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.getPaymentIntent(user.getAccountId(), user.getRole(), paymentIntentId),
                httpRequest
        );
    }

    @PostMapping("/payment-intents/{paymentIntentId}/attempts")
    public SuccessResponse<PaymentAttemptResponse> createPaymentAttempt(
            @PathVariable Long paymentIntentId,
            @RequestBody(required = false) CreatePaymentAttemptRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                financialService.createPaymentAttempt(user.getAccountId(), user.getRole(), paymentIntentId, request),
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

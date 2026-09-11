package com.project.optrabidz.financial.api;

import com.project.optrabidz.common.application.pagination.PageResponse;
import com.project.optrabidz.financial.application.FinancialService;
import com.project.optrabidz.financial.application.dto.request.CreatePaymentAttemptRequest;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.financial.application.dto.response.PaymentIntentResponse;
import com.project.optrabidz.financial.application.dto.response.RepaymentInstallmentResponse;
import com.project.optrabidz.financial.application.dto.response.RepaymentProgressResponse;
import com.project.optrabidz.financial.application.dto.response.RepaymentResponse;
import com.project.optrabidz.financial.application.dto.response.SettlementResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
public class FinancialController {
    private final FinancialService financialService;

    public FinancialController(FinancialService financialService) {
        this.financialService = financialService;
    }

    @GetMapping("/settlements/{settlementId}")
    public SettlementResponse getSettlement(@PathVariable Long settlementId,
                                            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return financialService.getSettlement(principal.getAccountId(), principal.getRole(), settlementId);
    }

    @GetMapping("/investors/me/settlements")
    public PageResponse<SettlementResponse> getMyInvestorSettlements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return financialService.getMyInvestorSettlements(principal.getAccountId(), principal.getRole(), page, size);
    }

    @GetMapping("/startups/me/settlements")
    public PageResponse<SettlementResponse> getMyStartupSettlements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return financialService.getMyStartupSettlements(principal.getAccountId(), principal.getRole(), page, size);
    }

    @PostMapping("/settlements/{settlementId}/payment-intents")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Payment intent created",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = PaymentIntentResponse.class)
            ),
            headers = @io.swagger.v3.oas.annotations.headers.Header(
                    name = "Location",
                    description = "URI of the created payment intent",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "string", format = "uri")
            )
    )
    public ResponseEntity<PaymentIntentResponse> createSettlementPaymentIntent(
            @PathVariable Long settlementId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        PaymentIntentResponse response = financialService.createSettlementPaymentIntent(
                principal.getAccountId(), principal.getRole(), settlementId);
        return paymentIntentCreated(response);
    }

    @GetMapping("/repayments/{repaymentId}")
    public RepaymentResponse getRepayment(@PathVariable Long repaymentId,
                                          @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return financialService.getRepayment(principal.getAccountId(), principal.getRole(), repaymentId);
    }

    @GetMapping("/repayments/{repaymentId}/installments")
    public PageResponse<RepaymentInstallmentResponse> getRepaymentInstallments(
            @PathVariable Long repaymentId,
            @Valid @ModelAttribute RepaymentInstallmentQuery query,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return financialService.getRepaymentInstallments(
                principal.getAccountId(),
                principal.getRole(),
                repaymentId,
                query.installmentState(),
                query.paymentView(),
                query.page(),
                query.size());
    }

    @GetMapping("/repayment-installments/{installmentId}")
    public RepaymentInstallmentResponse getRepaymentInstallment(
            @PathVariable Long installmentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return financialService.getRepaymentInstallment(principal.getAccountId(), principal.getRole(), installmentId);
    }

    @GetMapping("/agreements/{agreementId}/repayment-progress")
    public RepaymentProgressResponse getRepaymentProgress(
            @PathVariable Long agreementId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return financialService.getRepaymentProgress(principal.getAccountId(), principal.getRole(), agreementId);
    }

    @GetMapping("/investors/me/repayments")
    public PageResponse<RepaymentResponse> getMyInvestorRepayments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return financialService.getMyInvestorRepayments(principal.getAccountId(), principal.getRole(), page, size);
    }

    @GetMapping("/investors/me/repayment-installments")
    public PageResponse<RepaymentInstallmentResponse> getMyInvestorRepaymentInstallments(
            @Valid @ModelAttribute RepaymentInstallmentQuery query,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return financialService.getMyInvestorRepaymentInstallments(
                principal.getAccountId(),
                principal.getRole(),
                query.installmentState(),
                query.paymentView(),
                query.page(),
                query.size());
    }

    @GetMapping("/startups/me/repayments")
    public PageResponse<RepaymentResponse> getMyStartupRepayments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return financialService.getMyStartupRepayments(principal.getAccountId(), principal.getRole(), page, size);
    }

    @GetMapping("/startups/me/repayment-installments")
    public PageResponse<RepaymentInstallmentResponse> getMyStartupRepaymentInstallments(
            @Valid @ModelAttribute RepaymentInstallmentQuery query,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return financialService.getMyStartupRepaymentInstallments(
                principal.getAccountId(),
                principal.getRole(),
                query.installmentState(),
                query.paymentView(),
                query.page(),
                query.size());
    }

    @PostMapping("/repayments/{repaymentId}/payment-intents")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Payment intent created",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = PaymentIntentResponse.class)
            ),
            headers = @io.swagger.v3.oas.annotations.headers.Header(
                    name = "Location",
                    description = "URI of the created payment intent",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "string", format = "uri")
            )
    )
    public ResponseEntity<PaymentIntentResponse> createRepaymentPaymentIntent(
            @PathVariable Long repaymentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        PaymentIntentResponse response = financialService.createRepaymentPaymentIntent(
                principal.getAccountId(), principal.getRole(), repaymentId);
        return paymentIntentCreated(response);
    }

    @PostMapping("/repayment-installments/{installmentId}/payment-intents")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Payment intent created",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = PaymentIntentResponse.class)
            ),
            headers = @io.swagger.v3.oas.annotations.headers.Header(
                    name = "Location",
                    description = "URI of the created payment intent",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "string", format = "uri")
            )
    )
    public ResponseEntity<PaymentIntentResponse> createRepaymentInstallmentPaymentIntent(
            @PathVariable Long installmentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        PaymentIntentResponse response = financialService.createRepaymentInstallmentPaymentIntent(
                principal.getAccountId(), principal.getRole(), installmentId);
        return paymentIntentCreated(response);
    }

    @GetMapping("/payment-intents/{paymentIntentId}")
    public PaymentIntentResponse getPaymentIntent(@PathVariable Long paymentIntentId,
                                                  @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return financialService.getPaymentIntent(principal.getAccountId(), principal.getRole(), paymentIntentId);
    }

    @PostMapping("/payment-intents/{paymentIntentId}/attempts")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Payment attempt created",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = PaymentAttemptResponse.class)
            )
    )
    public ResponseEntity<PaymentAttemptResponse> createPaymentAttempt(
            @PathVariable Long paymentIntentId,
            @RequestBody(required = false) CreatePaymentAttemptRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                financialService.createPaymentAttempt(
                        principal.getAccountId(), principal.getRole(), paymentIntentId, request));
    }

    private ResponseEntity<PaymentIntentResponse> paymentIntentCreated(PaymentIntentResponse response) {
        return ResponseEntity.created(URI.create("/api/v1/payment-intents/" + response.paymentIntentId()))
                .body(response);
    }
}

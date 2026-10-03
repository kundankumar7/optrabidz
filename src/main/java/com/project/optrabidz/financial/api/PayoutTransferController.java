package com.project.optrabidz.financial.api;

import com.project.optrabidz.financial.application.PayoutService;
import com.project.optrabidz.financial.application.PaymentTimelineService;
import com.project.optrabidz.financial.application.dto.response.PaymentTimelineResponse;
import com.project.optrabidz.financial.application.dto.response.PayoutTransferResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PayoutTransferController {
    private final PayoutService payoutService;
    private final PaymentTimelineService paymentTimelineService;

    public PayoutTransferController(PayoutService payoutService,
                                    PaymentTimelineService paymentTimelineService) {
        this.payoutService = payoutService;
        this.paymentTimelineService = paymentTimelineService;
    }

    @GetMapping("/payout-transfers/{payoutTransferId}")
    public PayoutTransferResponse getById(
            @PathVariable Long payoutTransferId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return payoutService.getById(
                principal.getAccountId(), principal.getRole(), payoutTransferId);
    }

    @GetMapping("/payment-intents/{paymentIntentId}/payout-transfer")
    public PayoutTransferResponse getByPaymentIntentId(
            @PathVariable Long paymentIntentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return payoutService.getByPaymentIntentId(
                principal.getAccountId(), principal.getRole(), paymentIntentId);
    }

    @GetMapping("/payment-intents/{paymentIntentId}/timeline")
    public PaymentTimelineResponse getTimeline(
            @PathVariable Long paymentIntentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return paymentTimelineService.getTimeline(
                principal.getAccountId(), principal.getRole(), paymentIntentId);
    }

    @PostMapping("/payout-transfers/{payoutTransferId}/actions/retry")
    public PayoutTransferResponse retry(
            @PathVariable Long payoutTransferId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return payoutService.retryFailedForParticipant(
                principal.getAccountId(), principal.getRole(), payoutTransferId);
    }
}

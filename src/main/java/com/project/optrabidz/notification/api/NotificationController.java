package com.project.optrabidz.notification.api;

import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.notification.application.NotificationService;
import com.project.optrabidz.notification.application.dto.request.CreateNotificationSubscriptionRequest;
import com.project.optrabidz.notification.application.dto.response.NotificationFeedResponse;
import com.project.optrabidz.notification.application.dto.response.MarkAllReadResponse;
import com.project.optrabidz.notification.application.dto.response.NotificationResponse;
import com.project.optrabidz.notification.application.dto.response.NotificationSubscriptionResponse;
import com.project.optrabidz.notification.domain.model.ReadStatus;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications/me")
    public PageResponse<NotificationResponse> getMyNotifications(
            @RequestParam(required = false) ReadStatus readStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return notificationService.getMyFeed(principal.getAccountId(), readStatus, page, size);
    }

    @GetMapping("/notifications/me/summary")
    public NotificationFeedResponse getMyNotificationSummary(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return new NotificationFeedResponse(notificationService.unreadCount(principal.getAccountId()));
    }

    @PatchMapping("/notifications/{recipientId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long recipientId,
                                         @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        notificationService.markRead(principal.getAccountId(), recipientId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/notifications/me/read-all")
    public MarkAllReadResponse markAllRead(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        int updated = notificationService.markAllRead(principal.getAccountId());
        return new MarkAllReadResponse(updated);
    }

    @DeleteMapping("/notifications/{recipientId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long recipientId,
                                                   @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        notificationService.delete(principal.getAccountId(), recipientId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/notification-subscriptions")
    public NotificationSubscriptionResponse createSubscription(
            @RequestBody @Valid CreateNotificationSubscriptionRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return notificationService.saveSubscription(principal.getAccountId(), request);
    }

    @DeleteMapping("/notification-subscriptions/{subscriptionId}")
    public ResponseEntity<Void> revokeSubscription(@PathVariable Long subscriptionId,
                                                   @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        notificationService.revokeSubscription(principal.getAccountId(), subscriptionId);
        return ResponseEntity.noContent().build();
    }
}

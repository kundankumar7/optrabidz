package com.project.optrabidz.notification.api;

import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.MessageData;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.notification.application.NotificationService;
import com.project.optrabidz.notification.application.dto.request.CreateNotificationSubscriptionRequest;
import com.project.optrabidz.notification.application.dto.response.NotificationFeedResponse;
import com.project.optrabidz.notification.application.dto.response.NotificationResponse;
import com.project.optrabidz.notification.application.dto.response.NotificationSubscriptionResponse;
import com.project.optrabidz.notification.domain.model.ReadStatus;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    public SuccessResponse<PageResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(required = false) ReadStatus readStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(
                notificationService.getMyFeed(principal.getAccountId(), readStatus, page, size),
                httpRequest
        );
    }

    @GetMapping("/notifications/me/summary")
    public SuccessResponse<NotificationFeedResponse> getMyNotificationSummary(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(
                new NotificationFeedResponse(notificationService.unreadCount(principal.getAccountId())),
                httpRequest
        );
    }

    @PatchMapping("/notifications/{recipientId}/read")
    public SuccessResponse<MessageData> markRead(@PathVariable Long recipientId,
                                                 @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                 HttpServletRequest httpRequest) {
        notificationService.markRead(principal.getAccountId(), recipientId);
        return ApiResponse.success(new MessageData("Notification marked as read"), httpRequest);
    }

    @PatchMapping("/notifications/me/read-all")
    public SuccessResponse<MessageData> markAllRead(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                    HttpServletRequest httpRequest) {
        int updated = notificationService.markAllRead(principal.getAccountId());
        return ApiResponse.success(new MessageData("Marked " + updated + " notification(s) as read"), httpRequest);
    }

    @DeleteMapping("/notifications/{recipientId}")
    public SuccessResponse<MessageData> deleteNotification(@PathVariable Long recipientId,
                                                           @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                           HttpServletRequest httpRequest) {
        notificationService.delete(principal.getAccountId(), recipientId);
        return ApiResponse.success(new MessageData("Notification deleted"), httpRequest);
    }

    @PostMapping("/notification-subscriptions")
    public SuccessResponse<NotificationSubscriptionResponse> createSubscription(@RequestBody @Valid CreateNotificationSubscriptionRequest request,
                                                                                @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                                                HttpServletRequest httpRequest) {
        return ApiResponse.success(notificationService.saveSubscription(principal.getAccountId(), request), httpRequest);
    }

    @DeleteMapping("/notification-subscriptions/{subscriptionId}")
    public SuccessResponse<MessageData> revokeSubscription(@PathVariable Long subscriptionId,
                                                           @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                           HttpServletRequest httpRequest) {
        notificationService.revokeSubscription(principal.getAccountId(), subscriptionId);
        return ApiResponse.success(new MessageData("Notification subscription revoked"), httpRequest);
    }
}

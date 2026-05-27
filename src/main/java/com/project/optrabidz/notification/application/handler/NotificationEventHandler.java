package com.project.optrabidz.notification.application.handler;

import com.project.optrabidz.common.outbox.OutboxEvent;
import com.project.optrabidz.common.outbox.OutboxEventProcessor;
import com.project.optrabidz.notification.application.NotificationService;
import com.project.optrabidz.notification.application.rule.NotificationRuleRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventHandler implements OutboxEventProcessor {
    private final NotificationRuleRegistry notificationRuleRegistry;
    private final NotificationService notificationService;

    public NotificationEventHandler(NotificationRuleRegistry notificationRuleRegistry,
                                    NotificationService notificationService) {
        this.notificationRuleRegistry = notificationRuleRegistry;
        this.notificationService = notificationService;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return !notificationRuleRegistry.createPlans(event).isEmpty();
    }

    @Override
    public void process(OutboxEvent event) {
        notificationRuleRegistry.createPlans(event)
                .forEach(plan -> notificationService.createFromPlan(event, plan));
    }
}

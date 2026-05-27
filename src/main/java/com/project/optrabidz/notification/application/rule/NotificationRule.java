package com.project.optrabidz.notification.application.rule;

import com.project.optrabidz.common.outbox.OutboxEvent;

import java.util.List;

public interface NotificationRule {
    boolean supports(OutboxEvent event);

    List<NotificationPlan> createPlans(OutboxEvent event);
}

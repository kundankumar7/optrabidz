package com.project.optrabidz.notification.application.rule;

import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationRuleRegistry {
    private final List<NotificationRule> rules;

    public NotificationRuleRegistry(List<NotificationRule> rules) {
        this.rules = rules;
    }

    public List<NotificationPlan> createPlans(OutboxEvent event) {
        return rules.stream()
                .filter(rule -> rule.supports(event))
                .flatMap(rule -> rule.createPlans(event).stream())
                .toList();
    }
}

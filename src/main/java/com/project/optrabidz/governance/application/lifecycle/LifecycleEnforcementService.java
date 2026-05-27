package com.project.optrabidz.governance.application.lifecycle;

import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.governance.application.lifecycle.event.LifecycleRuleEnforcedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class LifecycleEnforcementService {
    private static final Logger log = LoggerFactory.getLogger(LifecycleEnforcementService.class);

    private final List<LifecycleRule> lifecycleRules;
    private final EventPublisher eventPublisher;

    public LifecycleEnforcementService(List<LifecycleRule> lifecycleRules,
                                       EventPublisher eventPublisher) {
        this.lifecycleRules = lifecycleRules;
        this.eventPublisher = eventPublisher;
    }

    public List<LifecycleEnforcementResult> enforceDueLifecycleRules() {
        Instant now = Instant.now();
        return lifecycleRules.stream()
                .map(rule -> enforceRule(rule, now))
                .toList();
    }

    private LifecycleEnforcementResult enforceRule(LifecycleRule rule, Instant now) {
        try {
            LifecycleEnforcementResult result = rule.enforce(now);
            if (result.changedCount() > 0 || result.failedCount() > 0) {
                log.info(
                        "Lifecycle rule {} evaluated={}, changed={}, failed={}",
                        result.ruleName(),
                        result.evaluatedCount(),
                        result.changedCount(),
                        result.failedCount()
                );
            }
            publishSystemAuditEvent(result, now);
            return result;
        } catch (Exception exception) {
            log.error("Lifecycle rule {} failed", rule.ruleName(), exception);
            LifecycleEnforcementResult result = LifecycleEnforcementResult.failed(rule.ruleName(), exception.getMessage());
            publishSystemAuditEvent(result, now);
            return result;
        }
    }

    private void publishSystemAuditEvent(LifecycleEnforcementResult result, Instant occurredAt) {
        if (result.changedCount() == 0 && result.failedCount() == 0) {
            return;
        }
        try {
            eventPublisher.publish(new LifecycleRuleEnforcedEvent(
                    result.ruleName(),
                    result.evaluatedCount(),
                    result.changedCount(),
                    result.failedCount(),
                    result.messages(),
                    occurredAt
            ));
        } catch (Exception exception) {
            log.error("Lifecycle rule {} system audit event could not be published", result.ruleName(), exception);
        }
    }
}

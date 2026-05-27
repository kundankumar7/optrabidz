package com.project.optrabidz.governance.application.lifecycle;

import com.project.optrabidz.common.event.DomainEvent;
import com.project.optrabidz.governance.application.lifecycle.event.LifecycleRuleEnforcedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LifecycleEnforcementServiceTest {
    @Test
    void publishesSystemAuditEventsForChangedAndFailedLifecycleRules() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        LifecycleEnforcementService service = new LifecycleEnforcementService(
                List.of(
                        staticRule("listing-expiry", LifecycleEnforcementResult.changed("listing-expiry", 2)),
                        staticRule("payment-intent-expiry", LifecycleEnforcementResult.skipped("payment-intent-expiry", "nothing due")),
                        staticRule("settlement-expiry", LifecycleEnforcementResult.failed("settlement-expiry", "planned lifecycle failure"))
                ),
                publishedEvents::add
        );

        List<LifecycleEnforcementResult> results = service.enforceDueLifecycleRules();

        assertThat(results).hasSize(3);
        assertThat(publishedEvents)
                .filteredOn(LifecycleRuleEnforcedEvent.class::isInstance)
                .map(LifecycleRuleEnforcedEvent.class::cast)
                .extracting(LifecycleRuleEnforcedEvent::ruleName)
                .containsExactly("listing-expiry", "settlement-expiry");
    }

    private LifecycleRule staticRule(String ruleName, LifecycleEnforcementResult result) {
        return new LifecycleRule() {
            @Override
            public String ruleName() {
                return ruleName;
            }

            @Override
            public LifecycleEnforcementResult enforce(Instant now) {
                return result;
            }
        };
    }
}

package com.project.optrabidz.common.outbox;

import com.project.optrabidz.common.event.DomainEvent;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Locale;

@Component
public class OutboxEventMetadataResolver {
    public OutboxEventMetadata resolve(DomainEvent event) {
        return new OutboxEventMetadata(
                resolveSourceModule(event),
                resolveAggregateType(event),
                resolveAggregateId(event)
        );
    }

    private String resolveSourceModule(DomainEvent event) {
        if ("AccountRegisteredEvent".equals(event.getClass().getSimpleName())) {
            return "IDENTITY";
        }
        String packageName = event.getClass().getPackageName();
        String marker = "com.project.optrabidz.";
        if (!packageName.startsWith(marker)) {
            return "COMMON";
        }
        String remaining = packageName.substring(marker.length());
        int dot = remaining.indexOf('.');
        String module = dot < 0 ? remaining : remaining.substring(0, dot);
        return module.toUpperCase(Locale.ROOT);
    }

    private String resolveAggregateType(DomainEvent event) {
        for (String methodName : aggregateIdMethodCandidates()) {
            if (hasNoArgMethod(event, methodName)) {
                return methodName.replace("Id", "").replaceAll("([a-z])([A-Z])", "$1_$2")
                        .toUpperCase(Locale.ROOT);
            }
        }
        return event.getClass().getSimpleName().replace("Event", "").toUpperCase(Locale.ROOT);
    }

    private String resolveAggregateId(DomainEvent event) {
        for (String methodName : aggregateIdMethodCandidates()) {
            Object value = invokeNoArgMethod(event, methodName);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private String[] aggregateIdMethodCandidates() {
        return new String[] {
                "agreementId",
                "bidId",
                "listingId",
                "startupId",
                "investorId",
                "newAdminAccountId",
                "revokedAdminAccountId",
                "accountId",
                "repaymentInstallmentId",
                "repaymentId",
                "settlementId"
        };
    }

    private boolean hasNoArgMethod(DomainEvent event, String methodName) {
        try {
            event.getClass().getMethod(methodName);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    private Object invokeNoArgMethod(DomainEvent event, String methodName) {
        try {
            Method method = event.getClass().getMethod(methodName);
            return method.invoke(event);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}

package com.project.optrabidz.documentation.error;

import com.project.optrabidz.classification.application.error.ClassificationErrors;
import com.project.optrabidz.common.api.error.FrameworkProblem;
import com.project.optrabidz.common.api.error.SecurityProblem;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.financial.application.error.FinancialErrors;
import com.project.optrabidz.governance.application.error.GovernanceErrors;
import com.project.optrabidz.identity.application.error.IdentityErrors;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;
import com.project.optrabidz.notification.application.error.NotificationErrors;
import com.project.optrabidz.participation.application.error.AdminErrors;
import com.project.optrabidz.participation.application.error.InvestorErrors;
import com.project.optrabidz.participation.application.error.ParticipationErrors;
import com.project.optrabidz.participation.application.error.StartupErrors;
import com.project.optrabidz.security.application.error.SecurityErrors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public final class PublicErrorCatalogue {

    private final List<PublicErrorDefinition> entries;

    private PublicErrorCatalogue(List<PublicErrorDefinition> entries) {
        this.entries = List.copyOf(entries);
    }

    public static PublicErrorCatalogue createDefault() {
        List<PublicErrorDefinition> definitions = new ArrayList<>();
        addModule(definitions, "classification", ClassificationErrors.descriptors());
        addModule(definitions, "identity", IdentityErrors.descriptors());
        addModule(definitions, "security-application", SecurityErrors.descriptors());
        addModule(definitions, "financial", FinancialErrors.descriptors());
        addModule(definitions, "participation-admin", AdminErrors.descriptors());
        addModule(definitions, "participation-investor", InvestorErrors.descriptors());
        addModule(definitions, "participation", ParticipationErrors.descriptors());
        addModule(definitions, "participation-startup", StartupErrors.descriptors());
        addModule(definitions, "marketplace", MarketplaceErrors.descriptors());
        addModule(definitions, "notification", NotificationErrors.descriptors());
        addModule(definitions, "governance", GovernanceErrors.descriptors());

        for (FrameworkProblem problem : FrameworkProblem.values()) {
            definitions.add(PublicErrorDefinition.fromFramework(problem));
        }
        for (SecurityProblem problem : SecurityProblem.values()) {
            definitions.add(PublicErrorDefinition.fromSecurity(problem));
        }
        return from(definitions);
    }

    public static PublicErrorCatalogue from(
            List<PublicErrorDefinition> definitions
    ) {
        Objects.requireNonNull(definitions, "definitions must not be null");
        Map<String, List<PublicErrorDefinition>> byCode = new TreeMap<>();
        for (PublicErrorDefinition definition : definitions) {
            Objects.requireNonNull(definition, "definition must not be null");
            byCode.computeIfAbsent(definition.code(), ignored -> new ArrayList<>())
                    .add(definition);
        }

        List<PublicErrorDefinition> merged = byCode.values().stream()
                .map(PublicErrorCatalogue::merge)
                .toList();
        return new PublicErrorCatalogue(merged);
    }

    public List<PublicErrorDefinition> entries() {
        return entries;
    }

    private static void addModule(
            List<PublicErrorDefinition> target,
            String source,
            List<ErrorDescriptor> descriptors
    ) {
        for (ErrorDescriptor descriptor : descriptors) {
            target.add(PublicErrorDefinition.fromModule(source, descriptor));
        }
    }

    private static PublicErrorDefinition merge(
            List<PublicErrorDefinition> candidates
    ) {
        PublicErrorDefinition merged = candidates.getFirst();
        SortedSet<String> sources = new TreeSet<>(merged.sources());
        for (int index = 1; index < candidates.size(); index++) {
            PublicErrorDefinition candidate = candidates.get(index);
            if (!merged.sameContract(candidate)) {
                SortedSet<String> conflictingSources = new TreeSet<>(sources);
                conflictingSources.addAll(candidate.sources());
                throw new IllegalStateException(
                        "Conflicting public error definition for "
                                + merged.code()
                                + " from sources "
                                + String.join(", ", conflictingSources)
                );
            }
            sources.addAll(candidate.sources());
        }
        return merged.withSources(sources);
    }
}

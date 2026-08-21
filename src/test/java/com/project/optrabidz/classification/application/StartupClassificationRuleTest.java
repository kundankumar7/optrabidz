package com.project.optrabidz.classification.application;

import com.project.optrabidz.classification.application.exception.StartupClassificationAlreadyExistsException;
import com.project.optrabidz.classification.application.exception.StartupClassificationRuleViolationException;
import com.project.optrabidz.classification.application.policy.DefaultStartupClassificationTypePolicy;
import com.project.optrabidz.classification.application.policy.StartupClassificationTypePolicy;
import com.project.optrabidz.classification.application.specification.StartupClassificationCardinalitySpec;
import com.project.optrabidz.classification.application.specification.StartupClassificationIntegritySpec;
import com.project.optrabidz.classification.application.specification.StartupClassificationUniquenessSpec;
import com.project.optrabidz.classification.domain.model.StartupClassification;
import com.project.optrabidz.classification.domain.model.StartupClassificationProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StartupClassificationRuleTest {

    @Test
    void duplicateEntriesUseTheStartupConflict() {
        StartupClassificationProfile duplicate = StartupClassificationProfile.establish(
                7L,
                List.of(
                        StartupClassification.create("SECTOR", "FINTECH"),
                        StartupClassification.create("SECTOR", "FINTECH")
                )
        );

        assertThatThrownBy(() -> new StartupClassificationUniquenessSpec().validate(duplicate))
                .isInstanceOf(StartupClassificationAlreadyExistsException.class);
    }

    @Test
    void unsupportedTypeUsesTheStartupRuleViolation() {
        StartupClassificationProfile profile = profileWith(
                StartupClassification.create("UNSUPPORTED", "VALUE")
        );

        assertThatThrownBy(() ->
                new StartupClassificationIntegritySpec(List.of()).validate(profile))
                .isInstanceOf(StartupClassificationRuleViolationException.class);
    }

    @Test
    void cardinalityLimitUsesTheStartupRuleViolation() {
        StartupClassificationProfile profile = profileWith(
                StartupClassification.create("SECTOR", "FINTECH"),
                StartupClassification.create("SECTOR", "HEALTHTECH")
        );

        assertThatThrownBy(() -> new StartupClassificationCardinalitySpec(
                List.of(singleValuePolicy())
        ).validate(profile)).isInstanceOf(StartupClassificationRuleViolationException.class);
    }

    @Test
    void blankValueUsesTheStartupRuleViolation() {
        assertThatThrownBy(() ->
                new DefaultStartupClassificationTypePolicy().validateValue(" "))
                .isInstanceOf(StartupClassificationRuleViolationException.class);
    }

    private static StartupClassificationProfile profileWith(StartupClassification... entries) {
        return StartupClassificationProfile.establish(7L, List.of(entries));
    }

    private static StartupClassificationTypePolicy singleValuePolicy() {
        return new StartupClassificationTypePolicy() {
            @Override
            public boolean supports(String classificationType) {
                return "SECTOR".equals(classificationType);
            }

            @Override
            public void validateValue(String classificationValue) {
            }

            @Override
            public int maxAllowedPerType() {
                return 1;
            }
        };
    }
}

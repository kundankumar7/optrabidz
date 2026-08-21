package com.project.optrabidz.classification.application;

import com.project.optrabidz.classification.application.exception.InvestorPreferenceAlreadyExistsException;
import com.project.optrabidz.classification.application.exception.InvestorPreferenceRuleViolationException;
import com.project.optrabidz.classification.application.policy.DefaultInvestorPreferenceTypePolicy;
import com.project.optrabidz.classification.application.policy.InvestorPreferenceTypePolicy;
import com.project.optrabidz.classification.application.specification.InvestorPreferenceCardinalitySpec;
import com.project.optrabidz.classification.application.specification.InvestorPreferenceIntegritySpec;
import com.project.optrabidz.classification.application.specification.InvestorPreferenceUniquenessSpec;
import com.project.optrabidz.classification.domain.model.InvestorPreference;
import com.project.optrabidz.classification.domain.model.InvestorPreferenceProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvestorPreferenceRuleTest {

    @Test
    void duplicateEntriesUseTheInvestorConflict() {
        InvestorPreferenceProfile duplicate = InvestorPreferenceProfile.establish(
                8L,
                List.of(
                        InvestorPreference.create("SECTOR", "FINTECH"),
                        InvestorPreference.create("SECTOR", "FINTECH")
                )
        );

        assertThatThrownBy(() -> new InvestorPreferenceUniquenessSpec().validate(duplicate))
                .isInstanceOf(InvestorPreferenceAlreadyExistsException.class);
    }

    @Test
    void unsupportedTypeUsesTheInvestorRuleViolation() {
        InvestorPreferenceProfile profile = profileWith(
                InvestorPreference.create("UNSUPPORTED", "VALUE")
        );

        assertThatThrownBy(() ->
                new InvestorPreferenceIntegritySpec(List.of()).validate(profile))
                .isInstanceOf(InvestorPreferenceRuleViolationException.class);
    }

    @Test
    void cardinalityLimitUsesTheInvestorRuleViolation() {
        InvestorPreferenceProfile profile = profileWith(
                InvestorPreference.create("SECTOR", "FINTECH"),
                InvestorPreference.create("SECTOR", "HEALTHTECH")
        );

        assertThatThrownBy(() -> new InvestorPreferenceCardinalitySpec(
                List.of(singleValuePolicy())
        ).validate(profile)).isInstanceOf(InvestorPreferenceRuleViolationException.class);
    }

    @Test
    void blankValueUsesTheInvestorRuleViolation() {
        assertThatThrownBy(() ->
                new DefaultInvestorPreferenceTypePolicy().validateValue(" "))
                .isInstanceOf(InvestorPreferenceRuleViolationException.class);
    }

    private static InvestorPreferenceProfile profileWith(InvestorPreference... entries) {
        return InvestorPreferenceProfile.establish(8L, List.of(entries));
    }

    private static InvestorPreferenceTypePolicy singleValuePolicy() {
        return new InvestorPreferenceTypePolicy() {
            @Override
            public boolean supports(String preferenceType) {
                return "SECTOR".equals(preferenceType);
            }

            @Override
            public void validateValue(String preferenceValue) {
            }

            @Override
            public int maxAllowedPerType() {
                return 1;
            }
        };
    }
}

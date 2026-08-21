package com.project.optrabidz.classification.application;

import com.project.optrabidz.classification.application.error.ClassificationErrors;
import com.project.optrabidz.classification.application.exception.InvestorPreferenceAlreadyExistsException;
import com.project.optrabidz.classification.application.exception.InvestorPreferenceNotFoundException;
import com.project.optrabidz.classification.application.exception.InvestorPreferenceProfileRequiredException;
import com.project.optrabidz.classification.application.exception.InvestorPreferenceRuleViolationException;
import com.project.optrabidz.classification.application.exception.StartupClassificationAlreadyExistsException;
import com.project.optrabidz.classification.application.exception.StartupClassificationNotFoundException;
import com.project.optrabidz.classification.application.exception.StartupClassificationProfileRequiredException;
import com.project.optrabidz.classification.application.exception.StartupClassificationRuleViolationException;
import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificationErrorContractTest {

    @Test
    void exposesFixedPublicDescriptors() {
        assertThat(ClassificationErrors.STARTUP_CLASSIFICATION_PROFILE_REQUIRED)
                .isEqualTo(descriptor(
                        "STARTUP_CLASSIFICATION_PROFILE_REQUIRED",
                        ErrorCategory.BUSINESS_RULE,
                        "Create a startup profile before managing classifications"
                ));
        assertThat(ClassificationErrors.STARTUP_CLASSIFICATION_ALREADY_EXISTS)
                .isEqualTo(descriptor(
                        "STARTUP_CLASSIFICATION_ALREADY_EXISTS",
                        ErrorCategory.CONFLICT,
                        "The startup classification already exists"
                ));
        assertThat(ClassificationErrors.STARTUP_CLASSIFICATION_NOT_FOUND)
                .isEqualTo(descriptor(
                        "STARTUP_CLASSIFICATION_NOT_FOUND",
                        ErrorCategory.NOT_FOUND,
                        "The requested startup classification was not found"
                ));
        assertThat(ClassificationErrors.STARTUP_CLASSIFICATION_RULE_VIOLATION)
                .isEqualTo(descriptor(
                        "STARTUP_CLASSIFICATION_RULE_VIOLATION",
                        ErrorCategory.BUSINESS_RULE,
                        "The startup classification does not satisfy classification rules"
                ));
        assertThat(ClassificationErrors.INVESTOR_PREFERENCE_PROFILE_REQUIRED)
                .isEqualTo(descriptor(
                        "INVESTOR_PREFERENCE_PROFILE_REQUIRED",
                        ErrorCategory.BUSINESS_RULE,
                        "Create an investor profile before managing preferences"
                ));
        assertThat(ClassificationErrors.INVESTOR_PREFERENCE_ALREADY_EXISTS)
                .isEqualTo(descriptor(
                        "INVESTOR_PREFERENCE_ALREADY_EXISTS",
                        ErrorCategory.CONFLICT,
                        "The investor preference already exists"
                ));
        assertThat(ClassificationErrors.INVESTOR_PREFERENCE_NOT_FOUND)
                .isEqualTo(descriptor(
                        "INVESTOR_PREFERENCE_NOT_FOUND",
                        ErrorCategory.NOT_FOUND,
                        "The requested investor preference was not found"
                ));
        assertThat(ClassificationErrors.INVESTOR_PREFERENCE_RULE_VIOLATION)
                .isEqualTo(descriptor(
                        "INVESTOR_PREFERENCE_RULE_VIOLATION",
                        ErrorCategory.BUSINESS_RULE,
                        "The investor preference does not satisfy preference rules"
                ));
    }

    @Test
    void typedFailuresKeepProtectedContextOutOfPublicMessages() {
        assertFailure(
                new StartupClassificationProfileRequiredException(41L),
                ClassificationErrors.STARTUP_CLASSIFICATION_PROFILE_REQUIRED,
                "CLASSIFICATION.STARTUP.PROFILE_REQUIRED",
                "41"
        );
        assertFailure(
                new StartupClassificationAlreadyExistsException("SECTOR", "PRIVATE-STARTUP"),
                ClassificationErrors.STARTUP_CLASSIFICATION_ALREADY_EXISTS,
                "CLASSIFICATION.STARTUP.ALREADY_EXISTS",
                "PRIVATE-STARTUP"
        );
        assertFailure(
                new StartupClassificationNotFoundException("SECTOR", "MISSING-STARTUP"),
                ClassificationErrors.STARTUP_CLASSIFICATION_NOT_FOUND,
                "CLASSIFICATION.STARTUP.NOT_FOUND",
                "MISSING-STARTUP"
        );
        assertFailure(
                new StartupClassificationRuleViolationException("Rejected startup value PRIVATE-RULE"),
                ClassificationErrors.STARTUP_CLASSIFICATION_RULE_VIOLATION,
                "CLASSIFICATION.STARTUP.RULE_VIOLATION",
                "PRIVATE-RULE"
        );
        assertFailure(
                new InvestorPreferenceProfileRequiredException(42L),
                ClassificationErrors.INVESTOR_PREFERENCE_PROFILE_REQUIRED,
                "CLASSIFICATION.INVESTOR.PROFILE_REQUIRED",
                "42"
        );
        assertFailure(
                new InvestorPreferenceAlreadyExistsException("SECTOR", "PRIVATE-INVESTOR"),
                ClassificationErrors.INVESTOR_PREFERENCE_ALREADY_EXISTS,
                "CLASSIFICATION.INVESTOR.ALREADY_EXISTS",
                "PRIVATE-INVESTOR"
        );
        assertFailure(
                new InvestorPreferenceNotFoundException("SECTOR", "MISSING-INVESTOR"),
                ClassificationErrors.INVESTOR_PREFERENCE_NOT_FOUND,
                "CLASSIFICATION.INVESTOR.NOT_FOUND",
                "MISSING-INVESTOR"
        );
        assertFailure(
                new InvestorPreferenceRuleViolationException("Rejected investor value PRIVATE-PREFERENCE"),
                ClassificationErrors.INVESTOR_PREFERENCE_RULE_VIOLATION,
                "CLASSIFICATION.INVESTOR.RULE_VIOLATION",
                "PRIVATE-PREFERENCE"
        );
    }

    private static ErrorDescriptor descriptor(
            String code,
            ErrorCategory category,
            String publicMessage
    ) {
        return new ErrorDescriptor(code, category, publicMessage);
    }

    private static void assertFailure(
            ApplicationException failure,
            ErrorDescriptor descriptor,
            String diagnosticCode,
            String protectedValue
    ) {
        assertThat(failure.descriptor()).isSameAs(descriptor);
        assertThat(failure.diagnosticCode()).isEqualTo(diagnosticCode);
        assertThat(failure.getMessage()).contains(protectedValue);
        assertThat(failure.descriptor().publicMessage()).doesNotContain(protectedValue);
    }
}

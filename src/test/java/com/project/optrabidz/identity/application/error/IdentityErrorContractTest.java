package com.project.optrabidz.identity.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.identity.application.exception.AccountNotFoundException;
import com.project.optrabidz.identity.application.exception.AccountStateConflictException;
import com.project.optrabidz.identity.application.exception.ProfileStateConflictException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityErrorContractTest {

    @Test
    void exposesFixedPublicDescriptors() {
        assertThat(IdentityErrors.ACCOUNT_NOT_FOUND)
                .isEqualTo(new ErrorDescriptor(
                        "ACCOUNT_NOT_FOUND",
                        ErrorCategory.NOT_FOUND,
                        "The requested account was not found"
                ));
        assertThat(IdentityErrors.ACCOUNT_STATE_CONFLICT)
                .isEqualTo(new ErrorDescriptor(
                        "ACCOUNT_STATE_CONFLICT",
                        ErrorCategory.CONFLICT,
                        "The account state does not allow this operation"
                ));
        assertThat(IdentityErrors.PROFILE_STATE_CONFLICT)
                .isEqualTo(new ErrorDescriptor(
                        "PROFILE_STATE_CONFLICT",
                        ErrorCategory.CONFLICT,
                        "The profile state does not allow this operation"
                ));
    }

    @Test
    void accountNotFoundKeepsIdentifierOutOfPublicContract() {
        AccountNotFoundException failure = new AccountNotFoundException(41L);

        assertThat(failure.descriptor()).isSameAs(IdentityErrors.ACCOUNT_NOT_FOUND);
        assertThat(failure.diagnosticCode()).isEqualTo("IDENTITY.ACCOUNT.NOT_FOUND");
        assertThat(failure.getMessage()).contains("41");
        assertThat(failure.descriptor().publicMessage()).doesNotContain("41");
    }

    @Test
    void stateConflictsRetainProtectedContextAndCause() {
        IllegalStateException accountCause = new IllegalStateException("Only CREATED account can be enabled");
        AccountStateConflictException accountFailure =
                new AccountStateConflictException(41L, "activate", accountCause);
        IllegalStateException profileCause = new IllegalStateException("Profile already complete");
        ProfileStateConflictException profileFailure =
                new ProfileStateConflictException(41L, "complete", profileCause);

        assertThat(accountFailure.descriptor()).isSameAs(IdentityErrors.ACCOUNT_STATE_CONFLICT);
        assertThat(accountFailure.diagnosticCode()).isEqualTo("IDENTITY.ACCOUNT.STATE_CONFLICT");
        assertThat(accountFailure.getMessage()).contains("41", "activate");
        assertThat(accountFailure.getCause()).isSameAs(accountCause);
        assertThat(accountFailure.descriptor().publicMessage()).doesNotContain("41", "activate");

        assertThat(profileFailure.descriptor()).isSameAs(IdentityErrors.PROFILE_STATE_CONFLICT);
        assertThat(profileFailure.diagnosticCode()).isEqualTo("IDENTITY.PROFILE.STATE_CONFLICT");
        assertThat(profileFailure.getMessage()).contains("41", "complete");
        assertThat(profileFailure.getCause()).isSameAs(profileCause);
        assertThat(profileFailure.descriptor().publicMessage()).doesNotContain("41", "complete");
    }
}

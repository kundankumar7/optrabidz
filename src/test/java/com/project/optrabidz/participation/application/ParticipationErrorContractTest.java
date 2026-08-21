package com.project.optrabidz.participation.application;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.participation.application.error.AdminErrors;
import com.project.optrabidz.participation.application.error.InvestorErrors;
import com.project.optrabidz.participation.application.error.ParticipationErrors;
import com.project.optrabidz.participation.application.error.StartupErrors;
import com.project.optrabidz.participation.application.exception.ActiveAdminAlreadyExistsException;
import com.project.optrabidz.participation.application.exception.ActiveAdminNotFoundException;
import com.project.optrabidz.participation.application.exception.AdminAuthorityAlreadyGrantedException;
import com.project.optrabidz.participation.application.exception.InvestorAlreadyExistsException;
import com.project.optrabidz.participation.application.exception.InvestorNotFoundException;
import com.project.optrabidz.participation.application.exception.ParticipationAuthorizationException;
import com.project.optrabidz.participation.application.exception.StartupAlreadyExistsException;
import com.project.optrabidz.participation.application.exception.StartupNotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipationErrorContractTest {

    @Test
    void exposesFixedPublicDescriptors() {
        assertThat(StartupErrors.STARTUP_ALREADY_EXISTS).isEqualTo(descriptor(
                "STARTUP_ALREADY_EXISTS", ErrorCategory.CONFLICT,
                "A startup profile already exists"));
        assertThat(StartupErrors.STARTUP_NOT_FOUND).isEqualTo(descriptor(
                "STARTUP_NOT_FOUND", ErrorCategory.NOT_FOUND,
                "The requested startup profile was not found"));
        assertThat(InvestorErrors.INVESTOR_ALREADY_EXISTS).isEqualTo(descriptor(
                "INVESTOR_ALREADY_EXISTS", ErrorCategory.CONFLICT,
                "An investor profile already exists"));
        assertThat(InvestorErrors.INVESTOR_NOT_FOUND).isEqualTo(descriptor(
                "INVESTOR_NOT_FOUND", ErrorCategory.NOT_FOUND,
                "The requested investor profile was not found"));
        assertThat(AdminErrors.ACTIVE_ADMIN_ALREADY_EXISTS).isEqualTo(descriptor(
                "ACTIVE_ADMIN_ALREADY_EXISTS", ErrorCategory.CONFLICT,
                "An active administrator already exists"));
        assertThat(AdminErrors.ADMIN_AUTHORITY_ALREADY_GRANTED).isEqualTo(descriptor(
                "ADMIN_AUTHORITY_ALREADY_GRANTED", ErrorCategory.CONFLICT,
                "Administrator authority was previously granted to this account"));
        assertThat(AdminErrors.ACTIVE_ADMIN_NOT_FOUND).isEqualTo(descriptor(
                "ACTIVE_ADMIN_NOT_FOUND", ErrorCategory.NOT_FOUND,
                "No active administrator was found"));
        assertThat(ParticipationErrors.AUTHORIZATION_FAILED).isEqualTo(descriptor(
                "AUTHORIZATION_FAILED", ErrorCategory.AUTHORIZATION,
                "You are not authorized to perform this action"));
    }

    @Test
    void typedFailuresReferenceTheirModuleCatalogues() {
        assertFailure(new StartupAlreadyExistsException(41L),
                StartupErrors.STARTUP_ALREADY_EXISTS,
                "PARTICIPATION.STARTUP.ALREADY_EXISTS", "41");
        assertFailure(new StartupNotFoundException(41L),
                StartupErrors.STARTUP_NOT_FOUND,
                "PARTICIPATION.STARTUP.NOT_FOUND", "41");
        assertFailure(new InvestorAlreadyExistsException(41L),
                InvestorErrors.INVESTOR_ALREADY_EXISTS,
                "PARTICIPATION.INVESTOR.ALREADY_EXISTS", "41");
        assertFailure(new InvestorNotFoundException(41L),
                InvestorErrors.INVESTOR_NOT_FOUND,
                "PARTICIPATION.INVESTOR.NOT_FOUND", "41");
        assertFailure(new AdminAuthorityAlreadyGrantedException(41L),
                AdminErrors.ADMIN_AUTHORITY_ALREADY_GRANTED,
                "PARTICIPATION.ADMIN.AUTHORITY_ALREADY_GRANTED", "41");

        ActiveAdminAlreadyExistsException activeConflict =
                new ActiveAdminAlreadyExistsException();
        assertThat(activeConflict.descriptor())
                .isSameAs(AdminErrors.ACTIVE_ADMIN_ALREADY_EXISTS);
        assertThat(activeConflict.diagnosticCode())
                .isEqualTo("PARTICIPATION.ADMIN.ACTIVE_ALREADY_EXISTS");

        ActiveAdminNotFoundException missingActive = new ActiveAdminNotFoundException();
        assertThat(missingActive.descriptor()).isSameAs(AdminErrors.ACTIVE_ADMIN_NOT_FOUND);
        assertThat(missingActive.diagnosticCode())
                .isEqualTo("PARTICIPATION.ADMIN.ACTIVE_NOT_FOUND");
    }

    @Test
    void roleContextRemainsProtected() {
        ParticipationAuthorizationException failure =
                new ParticipationAuthorizationException(RoleType.STARTUP, RoleType.INVESTOR);

        assertThat(failure.descriptor()).isSameAs(ParticipationErrors.AUTHORIZATION_FAILED);
        assertThat(failure.diagnosticCode()).isEqualTo("PARTICIPATION.AUTHORIZATION.FAILED");
        assertThat(failure.getMessage()).contains("STARTUP", "INVESTOR");
        assertThat(failure.descriptor().publicMessage())
                .doesNotContain("STARTUP", "INVESTOR");
    }

    private static ErrorDescriptor descriptor(String code, ErrorCategory category, String message) {
        return new ErrorDescriptor(code, category, message);
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

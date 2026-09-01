package com.project.optrabidz.security.application;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.security.application.error.SecurityErrors;
import com.project.optrabidz.security.application.exception.CredentialNotFoundException;
import com.project.optrabidz.security.application.exception.CurrentPasswordInvalidException;
import com.project.optrabidz.security.application.exception.EmailAlreadyRegisteredException;
import com.project.optrabidz.security.application.exception.InvalidCredentialsException;
import com.project.optrabidz.security.application.exception.PasswordPolicyViolationException;
import com.project.optrabidz.security.application.exception.SecurityAuthorizationException;
import com.project.optrabidz.security.application.exception.SelfRegistrationNotAllowedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorContractTest {

    @Test
    void exposesTheApprovedLoginReasonAllowlist() {
        assertThat(LoginFailureReason.values()).containsExactly(
                LoginFailureReason.UNKNOWN_IDENTITY,
                LoginFailureReason.INVALID_SECRET,
                LoginFailureReason.CREDENTIAL_LOCKED,
                LoginFailureReason.CREDENTIAL_DISABLED,
                LoginFailureReason.ACCOUNT_RESTRICTED
        );
    }

    @Test
    void exposesFixedPublicDescriptors() {
        assertThat(SecurityErrors.INVALID_CREDENTIALS).isEqualTo(descriptor(
                "INVALID_CREDENTIALS", ErrorCategory.AUTHENTICATION,
                "Invalid email or password"));
        assertThat(SecurityErrors.CURRENT_PASSWORD_INVALID).isEqualTo(descriptor(
                "CURRENT_PASSWORD_INVALID", ErrorCategory.AUTHENTICATION,
                "Current password is incorrect"));
        assertThat(SecurityErrors.EMAIL_ALREADY_REGISTERED).isEqualTo(descriptor(
                "EMAIL_ALREADY_REGISTERED", ErrorCategory.CONFLICT,
                "Email is already registered"));
        assertThat(SecurityErrors.CREDENTIAL_NOT_FOUND).isEqualTo(descriptor(
                "CREDENTIAL_NOT_FOUND", ErrorCategory.NOT_FOUND,
                "The requested credential was not found"));
        assertThat(SecurityErrors.PASSWORD_POLICY_VIOLATION).isEqualTo(descriptor(
                "PASSWORD_POLICY_VIOLATION", ErrorCategory.VALIDATION,
                "Password must contain at least one letter and one digit"));
        assertThat(SecurityErrors.SELF_REGISTRATION_NOT_ALLOWED).isEqualTo(descriptor(
                "SELF_REGISTRATION_NOT_ALLOWED", ErrorCategory.BUSINESS_RULE,
                "Only startup or investor accounts can self-register"));
        assertThat(SecurityErrors.AUTHORIZATION_FAILED).isEqualTo(descriptor(
                "AUTHORIZATION_FAILED", ErrorCategory.AUTHORIZATION,
                "You are not authorized to perform this action"));
    }

    @Test
    void typedFailuresKeepProtectedContextOutOfPublicMessages() {
        InvalidCredentialsException login =
                new InvalidCredentialsException(LoginFailureReason.CREDENTIAL_LOCKED);
        CurrentPasswordInvalidException currentPassword =
                new CurrentPasswordInvalidException(41L);
        EmailAlreadyRegisteredException duplicateEmail =
                new EmailAlreadyRegisteredException("private@example.com");
        CredentialNotFoundException missingCredential =
                new CredentialNotFoundException(41L);
        PasswordPolicyViolationException passwordPolicy =
                new PasswordPolicyViolationException();
        SelfRegistrationNotAllowedException registrationRole =
                new SelfRegistrationNotAllowedException(RoleType.ADMIN);
        SecurityAuthorizationException authorization =
                new SecurityAuthorizationException(41L, "change password");

        assertThat(login.descriptor()).isSameAs(SecurityErrors.INVALID_CREDENTIALS);
        assertThat(login.diagnosticCode()).isEqualTo("SECURITY.LOGIN.CREDENTIAL_LOCKED");
        assertThat(login.getMessage()).contains("CREDENTIAL_LOCKED");
        assertThat(login.descriptor().publicMessage()).doesNotContain("LOCKED");

        assertFailure(currentPassword, SecurityErrors.CURRENT_PASSWORD_INVALID,
                "SECURITY.PASSWORD.CURRENT_INVALID", "41");
        assertFailure(duplicateEmail, SecurityErrors.EMAIL_ALREADY_REGISTERED,
                "SECURITY.EMAIL.ALREADY_REGISTERED", "private@example.com");
        assertFailure(missingCredential, SecurityErrors.CREDENTIAL_NOT_FOUND,
                "SECURITY.CREDENTIAL.NOT_FOUND", "41");
        assertThat(passwordPolicy.diagnosticCode())
                .isEqualTo("SECURITY.PASSWORD.POLICY_VIOLATION");
        assertFailure(registrationRole, SecurityErrors.SELF_REGISTRATION_NOT_ALLOWED,
                "SECURITY.REGISTRATION.ROLE_NOT_ALLOWED", "ADMIN");
        assertFailure(authorization, SecurityErrors.AUTHORIZATION_FAILED,
                "SECURITY.AUTHORIZATION.FAILED", "41");
    }

    private static ErrorDescriptor descriptor(String code, ErrorCategory category, String message) {
        return new ErrorDescriptor(code, category, message);
    }

    private static void assertFailure(
            com.project.optrabidz.common.error.ApplicationException failure,
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

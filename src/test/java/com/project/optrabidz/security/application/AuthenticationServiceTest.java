package com.project.optrabidz.security.application;

import com.project.optrabidz.audit.application.SecurityAuditService;
import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.identity.application.command.ActivateAccountCommand;
import com.project.optrabidz.identity.application.command.CreateAccountCommand;
import com.project.optrabidz.identity.application.port.IdentityCommandPort;
import com.project.optrabidz.identity.application.port.IdentityQueryPort;
import com.project.optrabidz.identity.application.query.AccountSnapshot;
import com.project.optrabidz.identity.domain.model.AccountState;
import com.project.optrabidz.identity.domain.model.ProfileStatus;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.security.application.dto.request.ChangePasswordRequest;
import com.project.optrabidz.security.application.dto.request.LoginRequest;
import com.project.optrabidz.security.application.dto.request.SignupRequest;
import com.project.optrabidz.security.application.error.SecurityErrors;
import com.project.optrabidz.security.application.exception.CredentialNotFoundException;
import com.project.optrabidz.security.application.exception.CurrentPasswordInvalidException;
import com.project.optrabidz.security.application.exception.EmailAlreadyRegisteredException;
import com.project.optrabidz.security.application.exception.InvalidCredentialsException;
import com.project.optrabidz.security.application.exception.PasswordPolicyViolationException;
import com.project.optrabidz.security.application.exception.SecurityAuthorizationException;
import com.project.optrabidz.security.application.exception.SelfRegistrationNotAllowedException;
import com.project.optrabidz.security.domain.model.Credential;
import com.project.optrabidz.security.domain.model.CredentialStatus;
import com.project.optrabidz.security.domain.model.LoginAttempt;
import com.project.optrabidz.security.domain.model.Session;
import com.project.optrabidz.security.domain.model.SessionStatus;
import com.project.optrabidz.security.domain.repository.CredentialRepository;
import com.project.optrabidz.security.domain.repository.LoginAttemptRepository;
import com.project.optrabidz.security.domain.repository.SessionRepository;
import com.project.optrabidz.security.infrastructure.config.SecuritySessionConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final Long ACCOUNT_ID = 41L;
    private static final String EMAIL = "member@example.com";
    private static final String PASSWORD = "Password01";
    private static final String PASSWORD_HASH = "encoded-password";

    @Mock
    private IdentityCommandPort identityCommandPort;
    @Mock
    private IdentityQueryPort identityQueryPort;
    @Mock
    private CredentialRepository credentialRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private LoginAttemptRepository loginAttemptRepository;
    @Mock
    private SecurityAuditService securityAuditService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthenticationService service;
    private MockHttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(
                identityCommandPort,
                identityQueryPort,
                credentialRepository,
                sessionRepository,
                loginAttemptRepository,
                securityAuditService,
                passwordEncoder,
                Duration.ofHours(8),
                5
        );
        httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("127.0.0.1");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(LoginRejectionScenario.class)
    void protectedLoginCausesShareOnePublicContract(LoginRejectionScenario scenario) {
        configureLoginRejection(scenario);

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, PASSWORD), httpRequest))
                .isInstanceOfSatisfying(InvalidCredentialsException.class, failure -> {
                    assertThat(failure.descriptor()).isSameAs(SecurityErrors.INVALID_CREDENTIALS);
                    assertThat(failure.descriptor().publicMessage())
                            .isEqualTo("Invalid email or password");
                    assertThat(failure.diagnosticCode())
                            .isEqualTo("SECURITY.LOGIN." + scenario.reason().name());
                });

        ArgumentCaptor<LoginAttempt> attemptCaptor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().isSuccess()).isFalse();
        assertThat(attemptCaptor.getValue().getFailureReason()).isEqualTo(scenario.reason().name());
        verify(securityAuditService).recordLoginFailure(
                eq(EMAIL), eq(scenario.reason().name()), same(httpRequest));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void thresholdFailureLocksCredentialButKeepsInvalidSecretResponse() {
        Credential credential = credential(CredentialStatus.ACTIVE);
        when(credentialRepository.findByEmail(EMAIL)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);
        when(loginAttemptRepository.findRecentByEmail(EMAIL, 5)).thenReturn(List.of(
                failureAttempt(), failureAttempt(), failureAttempt(), failureAttempt(), failureAttempt()));

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, PASSWORD), httpRequest))
                .isInstanceOfSatisfying(InvalidCredentialsException.class, failure ->
                        assertThat(failure.diagnosticCode()).isEqualTo("SECURITY.LOGIN.INVALID_SECRET"));

        assertThat(credential.getCredentialStatus()).isEqualTo(CredentialStatus.LOCKED);
        verify(credentialRepository).save(credential);
    }

    @Test
    void missingAccountBehindCredentialIsInternalConsistencyFailure() {
        Credential credential = credential(CredentialStatus.ACTIVE);
        when(credentialRepository.findByEmail(EMAIL)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(identityQueryPort.findAccountById(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, PASSWORD), httpRequest))
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(ApplicationException.class)
                .hasMessage("Credential references a missing account");
        verify(sessionRepository, never()).save(any());
        verify(securityAuditService, never()).recordLoginFailure(any(), any(), any());
    }

    @Test
    void successfulLoginKeepsManagedSessionBehavior() {
        Credential credential = credential(CredentialStatus.ACTIVE);
        Session persistedSession = new Session(
                91L, ACCOUNT_ID, Instant.now(), Instant.now().plus(Duration.ofHours(8)), SessionStatus.ACTIVE);
        when(credentialRepository.findByEmail(EMAIL)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(identityQueryPort.findAccountById(ACCOUNT_ID)).thenReturn(Optional.of(account(AccountState.ACTIVE)));
        when(sessionRepository.save(any(Session.class))).thenReturn(persistedSession);

        var response = service.login(new LoginRequest(EMAIL, PASSWORD), httpRequest);

        assertThat(response.message()).isEqualTo("Login successful");
        assertThat(httpRequest.getSession(false)).isNotNull();
        assertThat(httpRequest.getSession(false).getAttribute(
                SecuritySessionConstants.DB_SESSION_ID_ATTRIBUTE)).isEqualTo(91L);
        assertThat(httpRequest.getSession(false).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNotNull();
        ArgumentCaptor<LoginAttempt> attemptCaptor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().isSuccess()).isTrue();
    }

    @Test
    void registrationUsesTypedFailuresAndPreservesSuccessFlow() {
        assertThatThrownBy(() -> service.register(new SignupRequest(EMAIL, PASSWORD, RoleType.ADMIN)))
                .isInstanceOf(SelfRegistrationNotAllowedException.class);
        assertThatThrownBy(() -> service.register(new SignupRequest(EMAIL, "password", RoleType.STARTUP)))
                .isInstanceOf(PasswordPolicyViolationException.class);

        when(credentialRepository.existsByEmail(EMAIL)).thenReturn(true);
        assertThatThrownBy(() -> service.register(new SignupRequest(EMAIL, PASSWORD, RoleType.STARTUP)))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        when(credentialRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(identityCommandPort.createAccount(any(CreateAccountCommand.class))).thenReturn(ACCOUNT_ID);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);

        var response = service.register(new SignupRequest(" MEMBER@EXAMPLE.COM ", PASSWORD, RoleType.STARTUP));

        assertThat(response.message()).isEqualTo("Account created successfully");
        ArgumentCaptor<Credential> credentialCaptor = ArgumentCaptor.forClass(Credential.class);
        verify(credentialRepository).save(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(credentialCaptor.getValue().getPasswordHash()).isEqualTo(PASSWORD_HASH);
        verify(identityCommandPort).activateAccount(new ActivateAccountCommand(ACCOUNT_ID));
    }

    @Test
    void changePasswordUsesTypedFailuresAndPreservesSuccessFlow() {
        AuthenticatedUserPrincipal admin = principal(RoleType.ADMIN);
        assertThatThrownBy(() -> service.changePassword(
                admin, new ChangePasswordRequest(PASSWORD, "Changed01")))
                .isInstanceOf(SecurityAuthorizationException.class);

        AuthenticatedUserPrincipal startup = principal(RoleType.STARTUP);
        when(credentialRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.changePassword(
                startup, new ChangePasswordRequest(PASSWORD, "Changed01")))
                .isInstanceOf(CredentialNotFoundException.class);

        Credential credential = credential(CredentialStatus.ACTIVE);
        when(credentialRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);
        assertThatThrownBy(() -> service.changePassword(
                startup, new ChangePasswordRequest(PASSWORD, "Changed01")))
                .isInstanceOf(CurrentPasswordInvalidException.class);

        assertThatThrownBy(() -> service.changePassword(
                startup, new ChangePasswordRequest(PASSWORD, "onlyletters")))
                .isInstanceOf(PasswordPolicyViolationException.class);

        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(passwordEncoder.encode("Changed01")).thenReturn("changed-hash");

        var response = service.changePassword(
                startup, new ChangePasswordRequest(PASSWORD, "Changed01"));

        assertThat(response.message()).isEqualTo("Password updated successfully");
        assertThat(credential.getPasswordHash()).isEqualTo("changed-hash");
        verify(credentialRepository).save(credential);
    }

    private void configureLoginRejection(LoginRejectionScenario scenario) {
        switch (scenario) {
            case UNKNOWN_IDENTITY -> when(credentialRepository.findByEmail(EMAIL))
                    .thenReturn(Optional.empty());
            case INVALID_SECRET -> {
                when(credentialRepository.findByEmail(EMAIL))
                        .thenReturn(Optional.of(credential(CredentialStatus.ACTIVE)));
                when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);
                when(loginAttemptRepository.findRecentByEmail(EMAIL, 5)).thenReturn(List.of());
            }
            case CREDENTIAL_LOCKED -> when(credentialRepository.findByEmail(EMAIL))
                    .thenReturn(Optional.of(credential(CredentialStatus.LOCKED)));
            case CREDENTIAL_DISABLED -> when(credentialRepository.findByEmail(EMAIL))
                    .thenReturn(Optional.of(credential(CredentialStatus.DISABLED)));
            case SUSPENDED_ACCOUNT -> configureRestrictedAccount(AccountState.SUSPENDED);
            case DEACTIVATED_ACCOUNT -> configureRestrictedAccount(AccountState.DEACTIVATED);
        }
    }

    private void configureRestrictedAccount(AccountState state) {
        when(credentialRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(credential(CredentialStatus.ACTIVE)));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(identityQueryPort.findAccountById(ACCOUNT_ID)).thenReturn(Optional.of(account(state)));
    }

    private static Credential credential(CredentialStatus status) {
        return new Credential(
                11L, ACCOUNT_ID, EMAIL, PASSWORD_HASH, status,
                Instant.parse("2026-08-21T00:00:00Z"), null);
    }

    private static LoginAttempt failureAttempt() {
        return LoginAttempt.failure(EMAIL, LoginFailureReason.INVALID_SECRET.name(), "127.0.0.1");
    }

    private static AccountSnapshot account(AccountState state) {
        return new AccountSnapshot(ACCOUNT_ID, state, ProfileStatus.INCOMPLETE, RoleType.STARTUP);
    }

    private static AuthenticatedUserPrincipal principal(RoleType roleType) {
        return new AuthenticatedUserPrincipal(ACCOUNT_ID, EMAIL, roleType);
    }

    private enum LoginRejectionScenario {
        UNKNOWN_IDENTITY(LoginFailureReason.UNKNOWN_IDENTITY),
        INVALID_SECRET(LoginFailureReason.INVALID_SECRET),
        CREDENTIAL_LOCKED(LoginFailureReason.CREDENTIAL_LOCKED),
        CREDENTIAL_DISABLED(LoginFailureReason.CREDENTIAL_DISABLED),
        SUSPENDED_ACCOUNT(LoginFailureReason.ACCOUNT_RESTRICTED),
        DEACTIVATED_ACCOUNT(LoginFailureReason.ACCOUNT_RESTRICTED);

        private final LoginFailureReason reason;

        LoginRejectionScenario(LoginFailureReason reason) {
            this.reason = reason;
        }

        LoginFailureReason reason() {
            return reason;
        }
    }
}

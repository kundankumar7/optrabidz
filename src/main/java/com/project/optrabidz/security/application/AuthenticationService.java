package com.project.optrabidz.security.application;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.audit.application.SecurityAuditService;
import com.project.optrabidz.identity.application.command.ActivateAccountCommand;
import com.project.optrabidz.identity.application.command.CreateAccountCommand;
import com.project.optrabidz.identity.application.port.IdentityCommandPort;
import com.project.optrabidz.identity.application.port.IdentityQueryPort;
import com.project.optrabidz.identity.application.query.AccountSnapshot;
import com.project.optrabidz.identity.domain.model.AccountState;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.security.application.dto.request.ChangePasswordRequest;
import com.project.optrabidz.security.application.dto.request.LoginRequest;
import com.project.optrabidz.security.application.dto.request.SignupRequest;
import com.project.optrabidz.security.application.dto.response.LoginResponse;
import com.project.optrabidz.security.application.dto.response.MessageResponse;
import com.project.optrabidz.security.application.dto.response.SignupResponse;
import com.project.optrabidz.security.application.exception.CredentialLockedException;
import com.project.optrabidz.security.application.exception.EmailAlreadyExistsException;
import com.project.optrabidz.security.application.exception.InvalidCredentialsException;
import com.project.optrabidz.security.domain.model.Credential;
import com.project.optrabidz.security.domain.model.CredentialStatus;
import com.project.optrabidz.security.domain.model.LoginAttempt;
import com.project.optrabidz.security.domain.model.Session;
import com.project.optrabidz.security.domain.repository.CredentialRepository;
import com.project.optrabidz.security.domain.repository.LoginAttemptRepository;
import com.project.optrabidz.security.domain.repository.SessionRepository;
import com.project.optrabidz.security.infrastructure.config.SecuritySessionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class AuthenticationService {
    private final IdentityCommandPort identityCommandPort;
    private final IdentityQueryPort identityQueryPort;
    private final CredentialRepository credentialRepository;
    private final SessionRepository sessionRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final SecurityAuditService securityAuditService;
    private final PasswordEncoder passwordEncoder;
    private final Duration sessionDuration;
    private final int maxLoginFailures;

    public AuthenticationService(IdentityCommandPort identityCommandPort,
                                 IdentityQueryPort identityQueryPort,
                                 CredentialRepository credentialRepository,
                                 SessionRepository sessionRepository,
                                 LoginAttemptRepository loginAttemptRepository,
                                 SecurityAuditService securityAuditService,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${optrabidz.security.session-duration:PT8H}") Duration sessionDuration,
                                 @Value("${optrabidz.security.max-login-failures:5}") int maxLoginFailures) {
        this.identityCommandPort = identityCommandPort;
        this.identityQueryPort = identityQueryPort;
        this.credentialRepository = credentialRepository;
        this.sessionRepository = sessionRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.securityAuditService = securityAuditService;
        this.passwordEncoder = passwordEncoder;
        this.sessionDuration = sessionDuration;
        this.maxLoginFailures = maxLoginFailures;
    }

    @Transactional
    public SignupResponse register(SignupRequest request) {
        String email = normalizeEmail(request.email());
        validateSupportedRole(request.role());
        validatePasswordPolicy(request.password());

        if (credentialRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        Long accountId = identityCommandPort.createAccount(new CreateAccountCommand(request.role()));
        credentialRepository.save(Credential.register(
                accountId,
                email,
                passwordEncoder.encode(request.password())
        ));
        identityCommandPort.activateAccount(new ActivateAccountCommand(accountId));

        return new SignupResponse("Account created successfully");
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        sessionRepository.expireExpiredSessions(Instant.now());

        String email = normalizeEmail(request.email());
        String sourceIp = resolveSourceIp(httpRequest);
        Credential credential = credentialRepository.findByEmail(email).orElse(null);

        if (credential == null) {
            recordFailedLogin(email, "Invalid credentials", sourceIp, httpRequest);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (credential.getCredentialStatus() == CredentialStatus.LOCKED) {
            recordFailedLogin(email, "Credential locked", sourceIp, httpRequest);
            throw new CredentialLockedException("Credential is locked");
        }

        if (credential.getCredentialStatus() == CredentialStatus.DISABLED) {
            recordFailedLogin(email, "Credential disabled", sourceIp, httpRequest);
            throw new ApiException(ErrorCode.AUTHORIZATION_FAILED, "Credential is disabled");
        }

        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            recordFailedLogin(email, "Invalid credentials", sourceIp, httpRequest);
            enforceLockPolicyIfNeeded(credential, email);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        AccountSnapshot account = identityQueryPort.findAccountById(credential.getAccountId())
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Account not found for credential"
                ));

        if (account.accountState() == AccountState.SUSPENDED
                || account.accountState() == AccountState.DEACTIVATED) {
            recordFailedLogin(email, "Account restricted", sourceIp, httpRequest);
            throw new ApiException(ErrorCode.AUTHORIZATION_FAILED, "Account is restricted");
        }

        loginAttemptRepository.save(LoginAttempt.success(email, sourceIp));
        terminateExistingSessionIfPresent(httpRequest);
        createManagedSession(httpRequest, account, credential);

        return new LoginResponse("Login successful");
    }

    @Transactional
    public MessageResponse logout(HttpServletRequest httpRequest) {
        terminateExistingSessionIfPresent(httpRequest);
        SecurityContextHolder.clearContext();
        return new MessageResponse("Logged out successfully");
    }

    @Transactional
    public MessageResponse changePassword(AuthenticatedUserPrincipal principal,
                                          ChangePasswordRequest request) {
        if (principal.getRole() == RoleType.ADMIN) {
            throw new ApiException(
                    ErrorCode.AUTHORIZATION_FAILED,
                    "Admin password changes are controlled by governance transfer flow"
            );
        }

        validatePasswordPolicy(request.newPassword());

        Credential credential = credentialRepository.findByAccountId(principal.getAccountId())
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Credential not found for account"
                ));

        if (!passwordEncoder.matches(request.currentPassword(), credential.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        credential.changePassword(passwordEncoder.encode(request.newPassword()));
        credentialRepository.save(credential);

        return new MessageResponse("Password updated successfully");
    }

    private void createManagedSession(HttpServletRequest httpRequest, AccountSnapshot account, Credential credential) {
        Session persistedSession = sessionRepository.save(Session.start(account.accountId(), sessionDuration));

        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                account.accountId(),
                credential.getEmail(),
                account.roleType()
        );

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setMaxInactiveInterval((int) sessionDuration.getSeconds());
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        session.setAttribute(SecuritySessionConstants.DB_SESSION_ID_ATTRIBUTE, persistedSession.getSessionId());
    }

    private void terminateExistingSessionIfPresent(HttpServletRequest httpRequest) {
        HttpSession existingSession = httpRequest.getSession(false);
        if (existingSession == null) {
            return;
        }

        Object sessionIdValue = existingSession.getAttribute(SecuritySessionConstants.DB_SESSION_ID_ATTRIBUTE);
        if (sessionIdValue instanceof Long sessionId) {
            sessionRepository.findById(sessionId).ifPresent(session -> {
                if (session.isActive()) {
                    session.terminate();
                    sessionRepository.save(session);
                }
            });
        }

        existingSession.invalidate();
    }

    private void enforceLockPolicyIfNeeded(Credential credential, String email) {
        if (credential.getCredentialStatus() != CredentialStatus.ACTIVE) {
            return;
        }

        long consecutiveFailures = countConsecutiveFailures(email);
        if (consecutiveFailures >= maxLoginFailures) {
            credential.lock();
            credentialRepository.save(credential);
            throw new CredentialLockedException("Credential locked due to repeated failed login attempts");
        }
    }

    private long countConsecutiveFailures(String email) {
        List<LoginAttempt> recentAttempts = loginAttemptRepository.findRecentByEmail(email, maxLoginFailures);
        long failures = 0;
        for (LoginAttempt attempt : recentAttempts) {
            if (attempt.isSuccess()) {
                break;
            }
            failures++;
        }
        return failures;
    }

    private void recordFailedLogin(String email,
                                   String failureReason,
                                   String sourceIp,
                                   HttpServletRequest httpRequest) {
        loginAttemptRepository.save(LoginAttempt.failure(email, failureReason, sourceIp));
        securityAuditService.recordLoginFailure(email, failureReason, httpRequest);
    }

    private void validateSupportedRole(RoleType roleType) {
        if (roleType == RoleType.ADMIN) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Only STARTUP or INVESTOR accounts can self-register");
        }
    }

    private void validatePasswordPolicy(String password) {
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        if (!hasLetter || !hasDigit) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Password must contain at least one letter and one digit"
            );
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveSourceIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }
        return forwardedFor.split(",")[0].trim();
    }
}

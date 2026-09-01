package com.project.optrabidz.security.infrastructure.web;

import com.project.optrabidz.audit.application.SecurityAuditService;
import com.project.optrabidz.common.api.error.SecurityProblem;
import com.project.optrabidz.common.api.error.SecurityProblemResponseWriter;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Component
public final class ProblemAccessDeniedHandler implements AccessDeniedHandler {
    private final SecurityAuditService securityAuditService;
    private final SecurityProblemResponseWriter responseWriter;

    public ProblemAccessDeniedHandler(
            SecurityAuditService securityAuditService,
            SecurityProblemResponseWriter responseWriter
    ) {
        this.securityAuditService = Objects.requireNonNull(
                securityAuditService,
                "securityAuditService"
        );
        this.responseWriter = Objects.requireNonNull(
                responseWriter,
                "responseWriter"
        );
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        SecurityProblem problem = exception instanceof CsrfException
                ? SecurityProblem.CSRF_VALIDATION_FAILED
                : SecurityProblem.AUTHORIZATION_FAILED;
        AuthenticatedUserPrincipal principal = currentPrincipal();
        securityAuditService.recordAuthorizationDenied(
                request,
                problem.code(),
                principal == null ? null : principal.getAccountId(),
                principal == null ? null : principal.getRole().name()
        );
        responseWriter.write(problem, request, response);
    }

    private AuthenticatedUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof AuthenticatedUserPrincipal principal)) {
            return null;
        }
        return principal;
    }
}

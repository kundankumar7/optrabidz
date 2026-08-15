package com.project.optrabidz.security.infrastructure.web;

import com.project.optrabidz.audit.application.SecurityAuditService;
import com.project.optrabidz.common.api.error.SecurityProblem;
import com.project.optrabidz.common.api.error.SecurityProblemResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Component
public final class ProblemAuthenticationEntryPoint
        implements AuthenticationEntryPoint {
    private final SecurityAuditService securityAuditService;
    private final SecurityProblemResponseWriter responseWriter;

    public ProblemAuthenticationEntryPoint(
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
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        SecurityProblem problem = SecurityProblem.AUTHENTICATION_REQUIRED;
        securityAuditService.recordAuthenticationRequired(
                request,
                problem.code()
        );
        responseWriter.write(problem, request, response);
    }
}

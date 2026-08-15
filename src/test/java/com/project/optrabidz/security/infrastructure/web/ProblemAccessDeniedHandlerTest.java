package com.project.optrabidz.security.infrastructure.web;

import com.project.optrabidz.audit.application.SecurityAuditService;
import com.project.optrabidz.common.api.error.SecurityProblem;
import com.project.optrabidz.common.api.error.SecurityProblemResponseWriter;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfException;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class ProblemAccessDeniedHandlerTest {
    @Mock
    private SecurityAuditService securityAuditService;
    @Mock
    private SecurityProblemResponseWriter responseWriter;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mapsAuthorizationFailureAndRecordsAuthenticatedActor()
            throws Exception {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                42L,
                "member@example.com",
                RoleType.INVESTOR
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
        ProblemAccessDeniedHandler handler = handler();

        handler.handle(
                request,
                response,
                new AccessDeniedException("Bearer secret-token")
        );

        verifyCalls(
                SecurityProblem.AUTHORIZATION_FAILED,
                principal.getAccountId(),
                principal.getRole().name()
        );
    }

    @Test
    void mapsCsrfFailureWithoutCopyingItsMessage() throws Exception {
        ProblemAccessDeniedHandler handler = handler();

        handler.handle(
                request,
                response,
                new CsrfException("secret-csrf-token")
        );

        verifyCalls(SecurityProblem.CSRF_VALIDATION_FAILED, null, null);
    }

    @Test
    void mapsAuthorizationFailureWithoutRecognizedPrincipal()
            throws Exception {
        ProblemAccessDeniedHandler handler = handler();

        handler.handle(
                request,
                response,
                new AccessDeniedException("Access denied")
        );

        verifyCalls(SecurityProblem.AUTHORIZATION_FAILED, null, null);
    }

    private ProblemAccessDeniedHandler handler() {
        return new ProblemAccessDeniedHandler(
                securityAuditService,
                responseWriter
        );
    }

    private void verifyCalls(
            SecurityProblem problem,
            Long accountId,
            String role
    ) throws Exception {
        InOrder calls = inOrder(securityAuditService, responseWriter);
        calls.verify(securityAuditService).recordAuthorizationDenied(
                request,
                problem.code(),
                accountId,
                role
        );
        calls.verify(responseWriter).write(problem, request, response);
        calls.verifyNoMoreInteractions();
    }
}

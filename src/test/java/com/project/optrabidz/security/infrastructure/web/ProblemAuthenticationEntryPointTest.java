package com.project.optrabidz.security.infrastructure.web;

import com.project.optrabidz.audit.application.SecurityAuditService;
import com.project.optrabidz.common.api.error.SecurityProblem;
import com.project.optrabidz.common.api.error.SecurityProblemResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class ProblemAuthenticationEntryPointTest {
    @Mock
    private SecurityAuditService securityAuditService;
    @Mock
    private SecurityProblemResponseWriter responseWriter;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Test
    void usesStableProblemCodeInsteadOfExceptionMessage() throws Exception {
        ProblemAuthenticationEntryPoint entryPoint =
                new ProblemAuthenticationEntryPoint(
                        securityAuditService,
                        responseWriter
                );
        var exception = new InsufficientAuthenticationException(
                "Bearer secret-token"
        );

        entryPoint.commence(request, response, exception);

        InOrder calls = inOrder(securityAuditService, responseWriter);
        calls.verify(securityAuditService).recordAuthenticationRequired(
                request,
                SecurityProblem.AUTHENTICATION_REQUIRED.code()
        );
        calls.verify(responseWriter).write(
                SecurityProblem.AUTHENTICATION_REQUIRED,
                request,
                response
        );
        calls.verifyNoMoreInteractions();
    }
}

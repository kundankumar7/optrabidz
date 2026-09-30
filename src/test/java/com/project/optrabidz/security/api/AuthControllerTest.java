package com.project.optrabidz.security.api;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import com.project.optrabidz.security.application.AuthenticationService;
import com.project.optrabidz.security.application.dto.request.ChangePasswordRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completedPasswordChangeStillReturnsNoContentWhenSessionWasAlreadyInvalidated() {
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(httpRequest.getSession(false)).thenReturn(session);
        doThrow(new IllegalStateException("Session already invalidated"))
                .when(session).invalidate();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("member@example.com", null, "ROLE_STARTUP"));

        AuthController controller = new AuthController(authenticationService);

        var response = controller.changePassword(
                new AuthenticatedUserPrincipal(41L, "member@example.com", RoleType.STARTUP),
                new ChangePasswordRequest("Password01", "Changed01"),
                httpRequest
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}

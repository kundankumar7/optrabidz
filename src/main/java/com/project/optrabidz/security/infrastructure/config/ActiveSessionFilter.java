package com.project.optrabidz.security.infrastructure.config;

import com.project.optrabidz.security.domain.repository.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class ActiveSessionFilter extends OncePerRequestFilter {
    private final SessionRepository sessionRepository;

    public ActiveSessionFilter(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        HttpSession httpSession = request.getSession(false);

        if (authentication == null || !authentication.isAuthenticated() || httpSession == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Object sessionIdValue = httpSession.getAttribute(SecuritySessionConstants.DB_SESSION_ID_ATTRIBUTE);
        if (!(sessionIdValue instanceof Long sessionId)) {
            clearSession(httpSession);
            filterChain.doFilter(request, response);
            return;
        }

        sessionRepository.findById(sessionId).ifPresentOrElse(session -> {
            if (!session.isActive()) {
                clearSessionSilently(httpSession);
                return;
            }

            if (session.isExpired(Instant.now())) {
                session.expire();
                sessionRepository.save(session);
                clearSessionSilently(httpSession);
            }
        }, () -> clearSessionSilently(httpSession));

        filterChain.doFilter(request, response);
    }

    private void clearSession(HttpSession httpSession) {
        clearSessionSilently(httpSession);
    }

    private void clearSessionSilently(HttpSession httpSession) {
        SecurityContextHolder.clearContext();
        try {
            httpSession.invalidate();
        } catch (IllegalStateException ignored) {
            // Session is already invalidated.
        }
    }
}

package com.project.optrabidz.common.observability;

import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityMdcFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
                MDC.put(ObservabilityMdcKeys.ACCOUNT_ID, String.valueOf(principal.getAccountId()));
                MDC.put(ObservabilityMdcKeys.ROLE, principal.getRole().name());
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(ObservabilityMdcKeys.ACCOUNT_ID);
            MDC.remove(ObservabilityMdcKeys.ROLE);
        }
    }
}

package com.project.optrabidz.security.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.audit.application.SecurityAuditService;
import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.observability.SecurityMdcFilter;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ObjectMapper objectMapper,
                                                   ActiveSessionFilter activeSessionFilter,
                                                   CsrfCookieFilter csrfCookieFilter,
                                                   SecurityMdcFilter securityMdcFilter,
                                                   SecurityAuditService securityAuditService) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");
        csrfTokenRepository.setHeaderName("X-CSRF-TOKEN");
        CsrfTokenRequestAttributeHandler csrfTokenRequestHandler = new CsrfTokenRequestAttributeHandler();

        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfTokenRequestHandler)
                        .ignoringRequestMatchers(publicPostMatchers())
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/recovery/transfer").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payment-providers/*/webhooks").permitAll()
                        .requestMatchers(
                                "/api/v1/auth/logout",
                                "/api/v1/auth/change-password",
                                "/api/v1/me",
                                "/api/v1/startups/**",
                                "/api/v1/investors/**",
                                "/api/v1/notifications/**",
                                "/api/v1/notification-subscriptions/**"
                        ).authenticated()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/startup-classifications/**").hasRole("STARTUP")
                        .requestMatchers("/api/v1/investor-preferences/**").hasRole("INVESTOR")
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            securityAuditService.recordAuthenticationRequired(request, authException.getMessage());
                            writeError(objectMapper, request, response,
                                    ErrorCode.AUTHENTICATION_REQUIRED,
                                    "Authentication is required");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String message = "You are not authorized to perform this action";
                            if (accessDeniedException instanceof org.springframework.security.web.csrf.CsrfException) {
                                message = "CSRF validation failed";
                            }
                            AuthenticatedUserPrincipal principal = currentPrincipal();
                            securityAuditService.recordAuthorizationDenied(
                                    request,
                                    message,
                                    principal == null ? null : principal.getAccountId(),
                                    principal == null ? null : principal.getRole().name()
                            );

                            log.warn(
                                    "Access denied for {} {}: {} - {}",
                                    request.getMethod(),
                                    request.getRequestURI(),
                                    accessDeniedException.getClass().getSimpleName(),
                                    accessDeniedException.getMessage()
                            );

                            writeError(objectMapper, request, response,
                                    ErrorCode.AUTHORIZATION_FAILED,
                                    message);
                        })
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .addFilterBefore(activeSessionFilter, org.springframework.security.web.access.intercept.AuthorizationFilter.class)
                .addFilterAfter(securityMdcFilter, ActiveSessionFilter.class)
                .addFilterAfter(csrfCookieFilter, org.springframework.security.web.csrf.CsrfFilter.class)
                .build();
    }

    private RequestMatcher[] publicPostMatchers() {
        return new RequestMatcher[] {
                new AntPathRequestMatcher("/api/v1/auth/register", "POST"),
                new AntPathRequestMatcher("/api/v1/auth/login", "POST"),
                new AntPathRequestMatcher("/api/v1/admin/recovery/transfer", "POST"),
                new AntPathRequestMatcher("/api/v1/payment-providers/*/webhooks", "POST")
        };
    }

    private AuthenticatedUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return null;
        }
        return principal;
    }

    private void writeError(ObjectMapper objectMapper,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            ErrorCode errorCode,
                            String message) throws IOException {
        response.setStatus(errorCode.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.error(errorCode, message, List.of(), request)
        );
    }
}

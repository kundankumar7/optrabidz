package com.project.optrabidz.security.infrastructure.config;

import com.project.optrabidz.common.observability.SecurityMdcFilter;
import com.project.optrabidz.security.infrastructure.web.ProblemAccessDeniedHandler;
import com.project.optrabidz.security.infrastructure.web.ProblemAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ActiveSessionFilter activeSessionFilter,
                                                   CsrfCookieFilter csrfCookieFilter,
                                                   SecurityMdcFilter securityMdcFilter,
                                                   ProblemAuthenticationEntryPoint authenticationEntryPoint,
                                                   ProblemAccessDeniedHandler accessDeniedHandler) throws Exception {
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
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
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

}

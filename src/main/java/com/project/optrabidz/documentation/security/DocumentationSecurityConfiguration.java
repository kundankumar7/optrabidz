package com.project.optrabidz.documentation.security;

import com.project.optrabidz.common.observability.SecurityMdcFilter;
import com.project.optrabidz.security.infrastructure.config.ActiveSessionFilter;
import com.project.optrabidz.security.infrastructure.web.ProblemAccessDeniedHandler;
import com.project.optrabidz.security.infrastructure.web.ProblemAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import java.util.Set;
import java.util.stream.Stream;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DocumentationExposureProperties.class)
public class DocumentationSecurityConfiguration {

    private static final String[] API_DOC_PATHS = {
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml"
    };
    private static final String[] UI_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/webjars/swagger-ui/**"
    };
    private static final String[] DOCUMENTATION_PATHS = Stream.concat(
            Stream.of(API_DOC_PATHS),
            Stream.of(UI_PATHS)
    ).toArray(String[]::new);

    @Bean
    public DocumentationExposureValidator documentationExposureValidator(
            DocumentationExposureProperties properties,
            Environment environment
    ) {
        DocumentationExposureValidator validator =
                new DocumentationExposureValidator(
                        properties,
                        Set.of(environment.getActiveProfiles()),
                        environment.getProperty(
                                "springdoc.api-docs.path",
                                DocumentationExposureValidator.API_DOCS_PATH
                        ),
                        environment.getProperty(
                                "springdoc.swagger-ui.path",
                                DocumentationExposureValidator.SWAGGER_UI_PATH
                        ),
                        environment.getProperty(
                                "springdoc.use-management-port",
                                Boolean.class,
                                false
                        )
                );
        validator.validate();
        return validator;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain documentationSecurityFilterChain(
            HttpSecurity http,
            DocumentationExposureProperties properties,
            ActiveSessionFilter activeSessionFilter,
            SecurityMdcFilter securityMdcFilter,
            ProblemAuthenticationEntryPoint authenticationEntryPoint,
            ProblemAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        return http
                .securityMatcher(DOCUMENTATION_PATHS)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.IF_REQUIRED
                ))
                .authorizeHttpRequests(authorize -> {
                    if (!properties.apiDocsEnabled()) {
                        authorize.requestMatchers(API_DOC_PATHS).denyAll();
                    } else if (properties.access()
                            == DocumentationExposureProperties.Access.PUBLIC) {
                        authorize.requestMatchers(API_DOC_PATHS).permitAll();
                    } else {
                        authorize.requestMatchers(API_DOC_PATHS).authenticated();
                    }

                    if (!properties.swaggerUiEnabled()) {
                        authorize.requestMatchers(UI_PATHS).denyAll();
                    } else {
                        authorize.requestMatchers(UI_PATHS).permitAll();
                    }
                    authorize.anyRequest().denyAll();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .addFilterBefore(activeSessionFilter, AuthorizationFilter.class)
                .addFilterAfter(securityMdcFilter, ActiveSessionFilter.class)
                .build();
    }
}

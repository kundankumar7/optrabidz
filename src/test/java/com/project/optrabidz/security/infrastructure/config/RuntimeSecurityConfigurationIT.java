package com.project.optrabidz.security.infrastructure.config;

import com.project.optrabidz.testsupport.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeSecurityConfigurationIT extends PostgresIntegrationTestSupport {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void doesNotCreateTheSpringBootDefaultInMemoryUser() {
        assertThat(applicationContext.getBeansOfType(InMemoryUserDetailsManager.class)).isEmpty();
    }
}

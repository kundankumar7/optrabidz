package com.project.optrabidz.testsupport;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class TestDatasourceConfigurationPolicyTest {
    @Test
    void testProfileBindsBoundedDatasourcePoolSize() throws IOException {
        Properties testProfile = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application-test.properties")
        );
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(new PropertiesPropertySource("test-profile", testProfile));

        HikariConfig hikariConfig = new Binder(ConfigurationPropertySources.from(propertySources))
                .bind("spring.datasource.hikari", Bindable.of(HikariConfig.class))
                .orElseGet(HikariConfig::new);

        assertThat(hikariConfig.getMaximumPoolSize()).isEqualTo(4);
    }
}

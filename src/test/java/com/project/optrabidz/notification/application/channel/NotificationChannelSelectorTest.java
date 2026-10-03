package com.project.optrabidz.notification.application.channel;

import com.project.optrabidz.notification.domain.model.ChannelType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationChannelSelectorTest {

    @Test
    void disabledExternalChannelsDoNotCreateDeliveriesForExistingSubscriptions() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForObject(
                anyString(),
                any(SqlParameterSource.class),
                eq(Long.class)
        )).thenReturn(1L);

        new ApplicationContextRunner()
                .withUserConfiguration(SelectorConfiguration.class)
                .withBean(NamedParameterJdbcTemplate.class, () -> jdbcTemplate)
                .withPropertyValues(
                        "optrabidz.notification.channels.email.enabled=false",
                        "optrabidz.notification.channels.push.enabled=false"
                )
                .run(context -> {
                    NotificationChannelSelector selector = context.getBean(NotificationChannelSelector.class);

                    assertThat(selector.resolve(42L, List.of(
                            ChannelType.IN_APP,
                            ChannelType.EMAIL,
                            ChannelType.PUSH
                    )))
                            .extracting(ResolvedNotificationChannel::channelType)
                            .containsExactly(ChannelType.IN_APP);
                });
    }

    @Test
    void enabledExternalChannelsRequireAnAvailableDeliveryStrategy() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForObject(
                anyString(),
                any(SqlParameterSource.class),
                eq(Long.class)
        )).thenReturn(1L);

        new ApplicationContextRunner()
                .withUserConfiguration(SelectorConfiguration.class)
                .withBean(NamedParameterJdbcTemplate.class, () -> jdbcTemplate)
                .withPropertyValues(
                        "optrabidz.notification.channels.email.enabled=true",
                        "optrabidz.notification.channels.push.enabled=true"
                )
                .run(context -> {
                    NotificationChannelSelector selector = context.getBean(NotificationChannelSelector.class);

                    assertThat(selector.resolve(42L, List.of(
                            ChannelType.IN_APP,
                            ChannelType.EMAIL,
                            ChannelType.PUSH
                    )))
                            .extracting(ResolvedNotificationChannel::channelType)
                            .containsExactly(ChannelType.IN_APP);
                });
    }

    @Test
    void demonstrationSelectsEnabledSandboxChannelsWithActiveSubscriptions() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForObject(
                anyString(),
                any(SqlParameterSource.class),
                eq(Long.class)
        )).thenReturn(1L);

        new ApplicationContextRunner()
                .withUserConfiguration(SandboxSelectorConfiguration.class)
                .withBean(NamedParameterJdbcTemplate.class, () -> jdbcTemplate)
                .withSystemProperties("spring.profiles.active=demo")
                .withPropertyValues(
                        "optrabidz.notification.channels.email.enabled=true",
                        "optrabidz.notification.channels.push.enabled=true"
                )
                .run(context -> {
                    NotificationChannelSelector selector = context.getBean(NotificationChannelSelector.class);

                    assertThat(selector.resolve(42L, List.of(
                            ChannelType.IN_APP,
                            ChannelType.EMAIL,
                            ChannelType.PUSH
                    )))
                            .extracting(ResolvedNotificationChannel::channelType)
                            .containsExactly(ChannelType.IN_APP, ChannelType.EMAIL, ChannelType.PUSH);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(NotificationChannelProperties.class)
    @Import({
            InAppNotificationChannelStrategy.class,
            NotificationChannelRegistry.class,
            NotificationChannelSelector.class
    })
    static class SelectorConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(NotificationChannelProperties.class)
    @Import({
            InAppNotificationChannelStrategy.class,
            NotificationChannelRegistry.class,
            NotificationChannelSelector.class,
            SandboxEmailNotificationChannelStrategy.class,
            SandboxPushNotificationChannelStrategy.class
    })
    static class SandboxSelectorConfiguration {
    }
}

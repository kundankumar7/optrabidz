package com.project.optrabidz.configuration;

import com.project.optrabidz.common.observability.OperationalEventLogger;
import com.project.optrabidz.common.outbox.OutboxDispatcher;
import com.project.optrabidz.common.outbox.OutboxDispatcherProperties;
import com.project.optrabidz.common.outbox.OutboxEventRepository;
import com.project.optrabidz.notification.application.channel.NotificationChannelProperties;
import com.project.optrabidz.notification.application.channel.NotificationChannelProxy;
import com.project.optrabidz.notification.application.channel.NotificationChannelRegistry;
import com.project.optrabidz.notification.application.channel.NotificationDeliveryDispatcher;
import com.project.optrabidz.notification.application.channel.NotificationDispatcherProperties;
import com.project.optrabidz.notification.application.channel.SandboxEmailNotificationChannelStrategy;
import com.project.optrabidz.notification.application.channel.SandboxPushNotificationChannelStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DispatcherConfigurationContractTest {

    private final ApplicationContextRunner outboxRunner = new ApplicationContextRunner()
            .withUserConfiguration(OutboxConfiguration.class)
            .withBean(OutboxEventRepository.class, () -> mock(OutboxEventRepository.class))
            .withBean(OperationalEventLogger.class, () -> mock(OperationalEventLogger.class))
            .withBean(TransactionTemplate.class, () -> mock(TransactionTemplate.class));

    private final ApplicationContextRunner notificationRunner = new ApplicationContextRunner()
            .withUserConfiguration(NotificationConfiguration.class)
            .withBean(NamedParameterJdbcTemplate.class, () -> mock(NamedParameterJdbcTemplate.class))
            .withBean(NotificationChannelRegistry.class, () -> mock(NotificationChannelRegistry.class))
            .withBean(NotificationChannelProxy.class, () -> mock(NotificationChannelProxy.class))
            .withBean(TransactionTemplate.class, () -> mock(TransactionTemplate.class));

    private final ApplicationContextRunner channelRunner = new ApplicationContextRunner()
            .withUserConfiguration(ChannelConfiguration.class);

    @Test
    void outboxDispatcherIsEnabledWhenAbsentOrTrueAndDisabledWhenFalse() {
        outboxRunner.run(context -> assertThat(context).hasSingleBean(OutboxDispatcher.class));
        outboxRunner.withPropertyValues("optrabidz.outbox.dispatcher.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(OutboxDispatcher.class));
        outboxRunner.withPropertyValues("optrabidz.outbox.dispatcher.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(OutboxDispatcher.class));
    }

    @Test
    void notificationDispatcherIsEnabledWhenAbsentOrTrueAndDisabledWhenFalse() {
        notificationRunner.run(context -> assertThat(context).hasSingleBean(NotificationDeliveryDispatcher.class));
        notificationRunner.withPropertyValues("optrabidz.notification.dispatcher.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(NotificationDeliveryDispatcher.class));
        notificationRunner.withPropertyValues("optrabidz.notification.dispatcher.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(NotificationDeliveryDispatcher.class));
    }

    @Test
    void emailChannelIsEnabledWhenAbsentOrTrueAndDisabledWhenFalse() {
        channelRunner.run(context -> assertThat(context)
                .hasSingleBean(SandboxEmailNotificationChannelStrategy.class));
        channelRunner.withPropertyValues("optrabidz.notification.channels.email.enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(SandboxEmailNotificationChannelStrategy.class));
        channelRunner.withPropertyValues("optrabidz.notification.channels.email.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(SandboxEmailNotificationChannelStrategy.class));
    }

    @Test
    void pushChannelIsEnabledWhenAbsentOrTrueAndDisabledWhenFalse() {
        channelRunner.run(context -> assertThat(context)
                .hasSingleBean(SandboxPushNotificationChannelStrategy.class));
        channelRunner.withPropertyValues("optrabidz.notification.channels.push.enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(SandboxPushNotificationChannelStrategy.class));
        channelRunner.withPropertyValues("optrabidz.notification.channels.push.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(SandboxPushNotificationChannelStrategy.class));
    }

    @Test
    void outboxDispatcherPreservesBatchAndWorkerDefaultsAndOverrides() {
        outboxRunner.withPropertyValues(
                        "optrabidz.outbox.dispatcher.batch-size=0",
                        "optrabidz.outbox.dispatcher.worker-id="
                )
                .run(context -> {
                    OutboxDispatcher dispatcher = context.getBean(OutboxDispatcher.class);
                    assertThat(ReflectionTestUtils.getField(dispatcher, "batchSize")).isEqualTo(1);
                    assertThat((String) ReflectionTestUtils.getField(dispatcher, "workerId"))
                            .startsWith("outbox-");
                });

        outboxRunner.withPropertyValues(
                        "optrabidz.outbox.dispatcher.batch-size=17",
                        "optrabidz.outbox.dispatcher.worker-id=outbox-review-worker"
                )
                .run(context -> {
                    OutboxDispatcher dispatcher = context.getBean(OutboxDispatcher.class);
                    assertThat(ReflectionTestUtils.getField(dispatcher, "batchSize")).isEqualTo(17);
                    assertThat(ReflectionTestUtils.getField(dispatcher, "workerId"))
                            .isEqualTo("outbox-review-worker");
                });
    }

    @Test
    void notificationDispatcherPreservesLimitsAndWorkerDefaultsAndOverrides() {
        notificationRunner.withPropertyValues(
                        "optrabidz.notification.dispatcher.batch-size=0",
                        "optrabidz.notification.dispatcher.max-attempts=0",
                        "optrabidz.notification.dispatcher.worker-id="
                )
                .run(context -> {
                    NotificationDeliveryDispatcher dispatcher =
                            context.getBean(NotificationDeliveryDispatcher.class);
                    assertThat(ReflectionTestUtils.getField(dispatcher, "batchSize")).isEqualTo(1);
                    assertThat(ReflectionTestUtils.getField(dispatcher, "maxAttempts")).isEqualTo(1);
                    assertThat((String) ReflectionTestUtils.getField(dispatcher, "workerId"))
                            .startsWith("notification-");
                });

        notificationRunner.withPropertyValues(
                        "optrabidz.notification.dispatcher.batch-size=23",
                        "optrabidz.notification.dispatcher.max-attempts=7",
                        "optrabidz.notification.dispatcher.worker-id=notification-review-worker"
                )
                .run(context -> {
                    NotificationDeliveryDispatcher dispatcher =
                            context.getBean(NotificationDeliveryDispatcher.class);
                    assertThat(ReflectionTestUtils.getField(dispatcher, "batchSize")).isEqualTo(23);
                    assertThat(ReflectionTestUtils.getField(dispatcher, "maxAttempts")).isEqualTo(7);
                    assertThat(ReflectionTestUtils.getField(dispatcher, "workerId"))
                            .isEqualTo("notification-review-worker");
                });
    }

    @Test
    void scheduledExpressionsRetainTheirExistingPropertyContracts() throws NoSuchMethodException {
        Scheduled outboxSchedule = OutboxDispatcher.class.getMethod("dispatchPending")
                .getAnnotation(Scheduled.class);
        assertThat(outboxSchedule.initialDelayString())
                .isEqualTo("${optrabidz.outbox.dispatcher.initial-delay-ms:5000}");
        assertThat(outboxSchedule.fixedDelayString())
                .isEqualTo("${optrabidz.outbox.dispatcher.fixed-delay-ms:5000}");

        Scheduled notificationSchedule = NotificationDeliveryDispatcher.class
                .getMethod("dispatchReadyDeliveries")
                .getAnnotation(Scheduled.class);
        assertThat(notificationSchedule.initialDelayString())
                .isEqualTo("${optrabidz.notification.dispatcher.initial-delay-ms:5000}");
        assertThat(notificationSchedule.fixedDelayString())
                .isEqualTo("${optrabidz.notification.dispatcher.fixed-delay-ms:5000}");
    }

    @Test
    void outboxPropertiesBindDefaultsAndOverrides() {
        outboxRunner.run(context -> {
            OutboxDispatcherProperties properties = context.getBean(OutboxDispatcherProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getInitialDelayMs()).isEqualTo(5000);
            assertThat(properties.getFixedDelayMs()).isEqualTo(5000);
            assertThat(properties.getBatchSize()).isEqualTo(50);
            assertThat(properties.getWorkerId()).isEmpty();
        });

        outboxRunner.withPropertyValues(
                        "optrabidz.outbox.dispatcher.enabled=false",
                        "optrabidz.outbox.dispatcher.initial-delay-ms=1100",
                        "optrabidz.outbox.dispatcher.fixed-delay-ms=2200",
                        "optrabidz.outbox.dispatcher.batch-size=17",
                        "optrabidz.outbox.dispatcher.worker-id=outbox-review-worker"
                )
                .run(context -> {
                    OutboxDispatcherProperties properties = context.getBean(OutboxDispatcherProperties.class);
                    assertThat(properties.isEnabled()).isFalse();
                    assertThat(properties.getInitialDelayMs()).isEqualTo(1100);
                    assertThat(properties.getFixedDelayMs()).isEqualTo(2200);
                    assertThat(properties.getBatchSize()).isEqualTo(17);
                    assertThat(properties.getWorkerId()).isEqualTo("outbox-review-worker");
                });
    }

    @Test
    void notificationPropertiesBindDefaultsAndOverrides() {
        notificationRunner.run(context -> {
            NotificationDispatcherProperties properties =
                    context.getBean(NotificationDispatcherProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getInitialDelayMs()).isEqualTo(5000);
            assertThat(properties.getFixedDelayMs()).isEqualTo(5000);
            assertThat(properties.getBatchSize()).isEqualTo(50);
            assertThat(properties.getMaxAttempts()).isEqualTo(3);
            assertThat(properties.getWorkerId()).isEmpty();
        });

        notificationRunner.withPropertyValues(
                        "optrabidz.notification.dispatcher.enabled=false",
                        "optrabidz.notification.dispatcher.initial-delay-ms=3300",
                        "optrabidz.notification.dispatcher.fixed-delay-ms=4400",
                        "optrabidz.notification.dispatcher.batch-size=23",
                        "optrabidz.notification.dispatcher.max-attempts=7",
                        "optrabidz.notification.dispatcher.worker-id=notification-review-worker"
                )
                .run(context -> {
                    NotificationDispatcherProperties properties =
                            context.getBean(NotificationDispatcherProperties.class);
                    assertThat(properties.isEnabled()).isFalse();
                    assertThat(properties.getInitialDelayMs()).isEqualTo(3300);
                    assertThat(properties.getFixedDelayMs()).isEqualTo(4400);
                    assertThat(properties.getBatchSize()).isEqualTo(23);
                    assertThat(properties.getMaxAttempts()).isEqualTo(7);
                    assertThat(properties.getWorkerId()).isEqualTo("notification-review-worker");
                });
    }

    @Test
    void notificationChannelPropertiesBindDefaultsAndOverrides() {
        channelRunner.run(context -> {
            NotificationChannelProperties properties = context.getBean(NotificationChannelProperties.class);
            assertThat(properties.getEmail().isEnabled()).isTrue();
            assertThat(properties.getPush().isEnabled()).isTrue();
        });

        channelRunner.withPropertyValues(
                        "optrabidz.notification.channels.email.enabled=false",
                        "optrabidz.notification.channels.push.enabled=false"
                )
                .run(context -> {
                    NotificationChannelProperties properties =
                            context.getBean(NotificationChannelProperties.class);
                    assertThat(properties.getEmail().isEnabled()).isFalse();
                    assertThat(properties.getPush().isEnabled()).isFalse();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OutboxDispatcherProperties.class)
    @Import(OutboxDispatcher.class)
    static class OutboxConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(NotificationDispatcherProperties.class)
    @Import(NotificationDeliveryDispatcher.class)
    static class NotificationConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(NotificationChannelProperties.class)
    @Import({
            SandboxEmailNotificationChannelStrategy.class,
            SandboxPushNotificationChannelStrategy.class
    })
    static class ChannelConfiguration {
    }
}

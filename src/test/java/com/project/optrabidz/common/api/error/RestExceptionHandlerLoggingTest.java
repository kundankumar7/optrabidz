package com.project.optrabidz.common.api.error;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.project.optrabidz.common.api.response.RequestMetadataFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestExceptionHandlerLoggingTest {
    private Logger handlerLogger;
    private ListAppender<ILoggingEvent> appender;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProblemDetailsFactory factory = new ProblemDetailsFactory(
                Clock.fixed(
                        Instant.parse("2026-08-15T04:00:00Z"),
                        ZoneOffset.UTC
                )
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new LoggingProbeController())
                .setControllerAdvice(new RestExceptionHandler(
                        factory,
                        new ValidationViolationMapper()
                ))
                .addFilters(new RequestMetadataFilter())
                .build();

        handlerLogger = (Logger) LoggerFactory.getLogger(
                RestExceptionHandler.class
        );
        appender = new ListAppender<>();
        appender.setContext(handlerLogger.getLoggerContext());
        appender.start();
        handlerLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        handlerLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void logsUnexpectedFailureOnceWithMdcCorrelation()
            throws Exception {
        mockMvc.perform(get("/test/logged-unexpected")
                        .header("X-Request-Id", "log-request-123"))
                .andExpect(status().isInternalServerError());

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage())
                    .isEqualTo("Unhandled MVC exception");
            assertThat(event.getThrowableProxy().getClassName())
                    .isEqualTo(RuntimeException.class.getName());
            assertThat(event.getMDCPropertyMap())
                    .containsEntry("requestId", "log-request-123")
                    .containsEntry("method", "GET")
                    .containsEntry("path", "/test/logged-unexpected");
        });
    }

    @RestController
    static final class LoggingProbeController {
        @GetMapping("/test/logged-unexpected")
        void fail() {
            throw new RuntimeException("internal diagnostic");
        }
    }
}

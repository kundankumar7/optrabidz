package com.project.optrabidz.common.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OperationalEventLogger {
    private static final Logger log = LoggerFactory.getLogger(OperationalEventLogger.class);
    private final SensitiveDataMasker sensitiveDataMasker;

    public OperationalEventLogger(SensitiveDataMasker sensitiveDataMasker) {
        this.sensitiveDataMasker = sensitiveDataMasker;
    }

    public void info(String eventName, String message) {
        log.info("{} - {}", eventName, sensitiveDataMasker.mask(message));
    }

    public void warn(String eventName, String message) {
        log.warn("{} - {}", eventName, sensitiveDataMasker.mask(message));
    }

    public void error(String eventName, String message, Throwable throwable) {
        log.error("{} - {}", eventName, sensitiveDataMasker.mask(message), throwable);
    }
}

package com.project.optrabidz.governance.application.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminBootstrapProperties properties;
    private final AdminBootstrapService adminBootstrapService;

    public AdminBootstrapRunner(AdminBootstrapProperties properties,
                                AdminBootstrapService adminBootstrapService) {
        this.properties = properties;
        this.adminBootstrapService = adminBootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        adminBootstrapService.bootstrapFirstAdmin(properties.toBootstrapCommand())
                .ifPresentOrElse(
                        accountId -> log.info("Bootstrapped first admin account {}", accountId),
                        () -> log.info("Admin bootstrap skipped because an active admin already exists")
                );
    }
}

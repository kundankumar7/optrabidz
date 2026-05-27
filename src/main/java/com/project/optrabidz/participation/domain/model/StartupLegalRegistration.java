package com.project.optrabidz.participation.domain.model;

import org.springframework.util.Assert;

public record StartupLegalRegistration(String type, String value) {
    public StartupLegalRegistration {
        Assert.hasText(type, "registration type must not be blank");
        Assert.hasText(value, "registration value must not be blank");
    }
}

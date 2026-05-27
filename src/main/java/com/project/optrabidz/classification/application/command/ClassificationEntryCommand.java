package com.project.optrabidz.classification.application.command;

import org.springframework.util.Assert;

public record ClassificationEntryCommand(
        String type,
        String value
) {
    public ClassificationEntryCommand {
        Assert.hasText(type, "type must not be blank");
        Assert.hasText(value, "value must not be blank");
    }
}

package com.project.optrabidz.classification.application.command;

import org.springframework.util.Assert;

public record RemoveStartupClassificationCommand(
        Long accountId,
        String classificationType,
        String classificationValue
) {
    public RemoveStartupClassificationCommand {
        Assert.notNull(accountId, "accountId must not be null");
        Assert.hasText(classificationType, "classificationType must not be blank");
        Assert.hasText(classificationValue, "classificationValue must not be blank");
    }
}

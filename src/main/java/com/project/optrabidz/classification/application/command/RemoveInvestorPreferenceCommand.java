package com.project.optrabidz.classification.application.command;

import org.springframework.util.Assert;

public record RemoveInvestorPreferenceCommand(
        Long accountId,
        String preferenceType,
        String preferenceValue
) {
    public RemoveInvestorPreferenceCommand {
        Assert.notNull(accountId, "accountId must not be null");
        Assert.hasText(preferenceType, "preferenceType must not be blank");
        Assert.hasText(preferenceValue, "preferenceValue must not be blank");
    }
}

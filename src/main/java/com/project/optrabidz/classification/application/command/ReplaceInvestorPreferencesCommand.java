package com.project.optrabidz.classification.application.command;

import org.springframework.util.Assert;

import java.util.List;

public record ReplaceInvestorPreferencesCommand(
        Long accountId,
        List<ClassificationEntryCommand> entries
) {
    public ReplaceInvestorPreferencesCommand {
        Assert.notNull(accountId, "accountId must not be null");
        entries = List.copyOf(entries == null ? List.of() : entries);
    }
}

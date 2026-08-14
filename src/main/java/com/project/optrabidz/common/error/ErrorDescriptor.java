package com.project.optrabidz.common.error;

import java.util.Objects;
import java.util.regex.Pattern;

public record ErrorDescriptor(
        String code,
        ErrorCategory category,
        String publicMessage
) {
    private static final Pattern PUBLIC_CODE = Pattern.compile("[A-Z][A-Z0-9_]*");

    public ErrorDescriptor {
        if (code == null || !PUBLIC_CODE.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "code must use upper snake case and must not be blank"
            );
        }
        Objects.requireNonNull(category, "category must not be null");
        if (publicMessage == null || publicMessage.isBlank()) {
            throw new IllegalArgumentException("publicMessage must not be blank");
        }
        publicMessage = publicMessage.strip();
    }
}

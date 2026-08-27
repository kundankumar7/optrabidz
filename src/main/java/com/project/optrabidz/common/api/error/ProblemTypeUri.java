package com.project.optrabidz.common.api.error;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

public final class ProblemTypeUri {

    private ProblemTypeUri() {
    }

    public static URI fromCode(String code) {
        Objects.requireNonNull(code, "code must not be null");
        return URI.create(
                "urn:optrabidz:problem:"
                        + code.toLowerCase(Locale.ROOT).replace('_', '-')
        );
    }
}

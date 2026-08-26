package com.project.optrabidz.common.api.error;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemTypeUriTest {

    @Test
    void derivesTheExistingPublicTypeUrn() {
        assertThat(ProblemTypeUri.fromCode("PAYMENT_STATE_CONFLICT"))
                .isEqualTo(URI.create(
                        "urn:optrabidz:problem:payment-state-conflict"
                ));
    }

    @Test
    void derivationDoesNotDependOnTheDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            assertThat(ProblemTypeUri.fromCode("INTERNAL_SERVER_ERROR"))
                    .hasToString(
                            "urn:optrabidz:problem:internal-server-error"
                    );
        } finally {
            Locale.setDefault(original);
        }
    }
}

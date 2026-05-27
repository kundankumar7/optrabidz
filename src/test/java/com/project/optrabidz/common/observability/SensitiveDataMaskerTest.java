package com.project.optrabidz.common.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {
    private final SensitiveDataMasker masker = new SensitiveDataMasker();

    @Test
    void masksSensitiveJsonAndQueryValues() {
        String masked = masker.mask("""
                {"password":"Password01","token":"abc123","safe":"visible"} authorization=BearerSecret
                Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.secret
                Cookie: JSESSIONID=abc; XSRF-TOKEN=csrf-secret
                X-CSRF-TOKEN: csrf-header-secret
                """);

        assertThat(masked).contains("\"password\":\"****\"");
        assertThat(masked).contains("\"token\":\"****\"");
        assertThat(masked).contains("authorization=****");
        assertThat(masked).contains("Authorization: ****");
        assertThat(masked).contains("Cookie: ****");
        assertThat(masked).contains("X-CSRF-TOKEN: ****");
        assertThat(masked).contains("\"safe\":\"visible\"");
        assertThat(masked).doesNotContain(
                "Password01",
                "abc123",
                "BearerSecret",
                "eyJhbGciOiJIUzI1NiJ9.secret",
                "csrf-secret",
                "csrf-header-secret"
        );
    }

    @Test
    void masksEmailWithoutDestroyingDomainContext() {
        assertThat(masker.maskEmail("startup.owner@example.com"))
                .isEqualTo("s****r@example.com");
        assertThat(masker.maskEmail("ab@example.com"))
                .isEqualTo("****@example.com");
    }
}

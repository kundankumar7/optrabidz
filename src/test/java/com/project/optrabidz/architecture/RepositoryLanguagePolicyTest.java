package com.project.optrabidz.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepositoryLanguagePolicyTest {

    @Test
    void matchesCompleteTokensAfterNormalization() {
        var policy = List.of(
                RepositoryLanguagePolicy.policyEntry("P01", "amber compass"),
                RepositoryLanguagePolicy.policyEntry("P02", "cedar"));

        var violations = RepositoryLanguagePolicy.inspectText(
                Path.of("notes", "example.txt"),
                "AMBER,\n  COMPASS and CeDaR.",
                policy);

        assertThat(violations)
                .extracting(
                        RepositoryLanguagePolicy.Violation::policyId,
                        RepositoryLanguagePolicy.Violation::line)
                .containsExactly(tuple("P01", 1), tuple("P02", 2));
    }

    @Test
    void doesNotMatchCharacterFragments() {
        var policy = List.of(RepositoryLanguagePolicy.policyEntry("P01", "art"));

        var violations = RepositoryLanguagePolicy.inspectText(
                Path.of("notes", "example.txt"),
                "A cartwheel is not the complete policy token.",
                policy);

        assertThat(violations).isEmpty();
    }

    @Test
    void checksTrackedPathNames() {
        var policy = List.of(RepositoryLanguagePolicy.policyEntry("P03", "amber compass"));

        var violations = RepositoryLanguagePolicy.inspectPath(
                Path.of("docs", "Amber-Compass", "guide.md"),
                policy);

        assertThat(violations)
                .extracting(
                        RepositoryLanguagePolicy.Violation::path,
                        RepositoryLanguagePolicy.Violation::line,
                        RepositoryLanguagePolicy.Violation::policyId)
                .containsExactly(tuple("docs/Amber-Compass/guide.md", null, "P03"));
    }

    @Test
    void sanitizesViolationOutput() {
        var policy = List.of(RepositoryLanguagePolicy.policyEntry("P04", "scarlet meadow"));

        var violation = RepositoryLanguagePolicy.inspectText(
                        Path.of("docs", "example.txt"),
                        "prefix\nScarlet meadow appears with private context.",
                        policy)
                .getFirst();

        assertThat(violation.toString())
                .isEqualTo("docs/example.txt:2 violates repository language policy [P04]")
                .doesNotContain("scarlet meadow", "private context");
    }
}

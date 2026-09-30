package com.project.optrabidz.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepositoryLanguagePolicyTest {

    private static final List<RepositoryLanguagePolicy.PolicyEntry> REPOSITORY_POLICY = List.of(
            new RepositoryLanguagePolicy.PolicyEntry(
                    "P01", 1, "9c1c934e33089936612ec2b89ad42da2b6b32a2bdd12e07069a4c26683f7c2d5"),
            new RepositoryLanguagePolicy.PolicyEntry(
                    "P02", 1, "1e32847769dc4a1588004a1dfdf10041407a3494ae910bc76783917fffea7789"),
            new RepositoryLanguagePolicy.PolicyEntry(
                    "P03", 1, "57de4cf40144bdf7d00010f2f5557a7d642c2b9705309bfade167dd313e2ca93"),
            new RepositoryLanguagePolicy.PolicyEntry(
                    "P04", 1, "7d3194f79e645c42e4396dda38be04766810ec6a00d00aced3ffc2a0a1f1a9ef"),
            new RepositoryLanguagePolicy.PolicyEntry(
                    "P05", 1, "60965168ce762e949600281ba6d01fee136e5b6e8257b1f216f9025ed324474c"),
            new RepositoryLanguagePolicy.PolicyEntry(
                    "P06", 2, "ee662f0a4316976d226a3fafe98060cd4995718d09c62f78dd1d359399a75528"),
            new RepositoryLanguagePolicy.PolicyEntry(
                    "P07", 2, "cdcbb68a3444fdd629ab0af770af3fc99e4d5dde43b576d5f855691e155283ec"),
            new RepositoryLanguagePolicy.PolicyEntry(
                    "P08", 3, "9760d607e65eafcf2cef66bf6b6d00e2c2707e9d4bebe4ab2752bfd16233adb6"),
            new RepositoryLanguagePolicy.PolicyEntry(
                    "P09", 1, "b225390a8984c8de4206c746772ab5ef2abb0a02bcb043da10f6d41c274d1a02"));

    @Test
    void matchesCompleteTokensAfterNormalization() {
        var policy = List.of(
                RepositoryLanguagePolicy.policyEntry("P01", "amber compass"),
                RepositoryLanguagePolicy.policyEntry("P02", "cedar"));

        var violations = RepositoryLanguagePolicy.inspectText(
                Path.of("notes", "example.txt"),
                "AMBER,\n  COMPASS and CEDAR.",
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
                Path.of("docs", "AmberCompass", "guide.md"),
                policy);

        assertThat(violations)
                .extracting(
                        RepositoryLanguagePolicy.Violation::path,
                        RepositoryLanguagePolicy.Violation::line,
                        RepositoryLanguagePolicy.Violation::policyId)
                .containsExactly(tuple("docs/AmberCompass/guide.md", null, "P03"));
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

    @Test
    void trackedRepositoryContainsNoProhibitedLanguage() throws Exception {
        var violations = RepositoryLanguagePolicy.inspectTrackedRepository(
                Path.of("").toAbsolutePath().normalize(), REPOSITORY_POLICY);

        assertThat(violations)
                .as("tracked repository language violations")
                .isEmpty();
    }
}

package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DocumentationLinksTest {

    @Test
    void localDocumentationTargetsResolve() throws Exception {
        Path repositoryRoot = Path.of("").toAbsolutePath().normalize();

        assertThat(DocumentationLinkValidator.findBrokenTargets(repositoryRoot))
                .as("broken local documentation targets")
                .isEmpty();
    }
}

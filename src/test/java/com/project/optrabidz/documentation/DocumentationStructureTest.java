package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DocumentationStructureTest {

    @Test
    void repositoryDocumentationFollowsTheStableHierarchy() throws Exception {
        assertThat(DocumentationStructureValidator.findViolations(Path.of(".")))
                .isEmpty();
    }
}

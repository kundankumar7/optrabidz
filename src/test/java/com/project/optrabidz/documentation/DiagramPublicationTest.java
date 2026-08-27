package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DiagramPublicationTest {

    @Test
    void publishedDiagramsMeetTheRepositoryContract() throws Exception {
        Path repository = Path.of("").toAbsolutePath().normalize();

        assertThat(DiagramPublicationValidator.findViolations(repository))
                .as("diagram publication violations")
                .isEmpty();
    }
}

package com.project.optrabidz.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonPackageContractTest {
    private static final Pattern FORBIDDEN_IMPORT = Pattern.compile(
            "^\\s*import\\s+(?:static\\s+)?com\\.fasterxml\\.jackson\\.(core|databind|datatype)(?:\\.|;)"
    );
    private static final List<Path> SOURCE_ROOTS = List.of(
            Path.of("src", "main", "java"),
            Path.of("src", "test", "java")
    );

    @Test
    void productionAndTestSourcesDoNotImportJackson2CoreOrDatabind() throws IOException {
        Path repositoryRoot = Path.of("").toAbsolutePath().normalize();
        List<String> violations = new ArrayList<>();

        for (Path sourceRoot : SOURCE_ROOTS) {
            Path absoluteRoot = repositoryRoot.resolve(sourceRoot);
            try (Stream<Path> paths = Files.walk(absoluteRoot)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> collectViolations(
                                repositoryRoot,
                                path,
                                violations
                        ));
            }
        }

        assertThat(violations)
                .as("Jackson 2 core, databind, and datatype imports")
                .isEmpty();
    }

    @Test
    void detectsStaticJackson2Imports() {
        assertThat(FORBIDDEN_IMPORT.matcher(
                "import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;"
        ).find()).isTrue();
    }

    private static void collectViolations(
            Path repositoryRoot,
            Path source,
            List<String> violations
    ) {
        try {
            List<String> lines = Files.readAllLines(source);
            for (int index = 0; index < lines.size(); index++) {
                if (FORBIDDEN_IMPORT.matcher(lines.get(index)).find()) {
                    violations.add(repositoryRoot.relativize(source)
                            .toString()
                            .replace('\\', '/') + ":" + (index + 1));
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not inspect Java source " + source,
                    exception
            );
        }
    }
}

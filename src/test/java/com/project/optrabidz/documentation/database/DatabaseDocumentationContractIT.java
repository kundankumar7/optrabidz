package com.project.optrabidz.documentation.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.documentation.database.DatabaseSchemaSnapshot.ForeignKey;
import com.project.optrabidz.documentation.database.DatabaseSchemaSnapshot.NamedObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class DatabaseDocumentationContractIT {

    private static final Path DATABASE_ROOT = Path.of("docs", "database");
    private static final Path REPORT = Path.of(
            "target", "documentation-verification", "schema-report.json");
    private static final Pattern DOCUMENTED_OBJECT = Pattern.compile(
            "`((?:chk|idx|uq|trg)_[a-z0-9_]+)`");
    private static final Pattern CORRELATION_ROW = Pattern.compile(
            "^\\| `([^`]+)` \\| `([^`]+)` \\| `([^`]+)` \\| (.+) \\|$");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("optrabidz_documentation_test")
                    .withUsername("optrabidz")
                    .withPassword("optrabidz");

    @Test
    void effectivePostgresSchemaAgreesWithHumanDocumentation() throws Exception {
        migrate();

        DatabaseSchemaSnapshot schema;
        try (Connection connection = POSTGRES.createConnection("")) {
            schema = new PostgresSchemaIntrospector().read(connection);
        }
        writeDiagnosticReport(schema);

        assertExtractionBaseline(schema);
        String journey = Files.readString(DATABASE_ROOT.resolve("relationship-journey.md"));
        String focusedViews = focusedViewDocumentation();
        String reference = Files.readString(DATABASE_ROOT.resolve("reference/README.md"));
        String allDocumentation = databaseDocumentation();

        assertEveryTableIsPlaced(schema, journey + focusedViews);
        assertEveryForeignKeyIsDocumented(schema, focusedViews);
        assertReferencedObjectsExist(schema, allDocumentation);
        assertCorrelationsReferenceExistingColumns(schema, reference);
    }

    private void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load()
                .migrate();
    }

    private void writeDiagnosticReport(DatabaseSchemaSnapshot schema) throws Exception {
        Files.createDirectories(REPORT.getParent());
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(REPORT.toFile(), schema);
    }

    private void assertExtractionBaseline(DatabaseSchemaSnapshot schema) {
        assertThat(schema.tables()).hasSize(35);
        assertThat(schema.foreignKeys()).hasSize(46);
        assertThat(schema.uniqueConstraints()).hasSize(25);
        assertThat(schema.checkConstraints()).hasSize(57);
        assertThat(schema.partialIndexes()).hasSize(19);
        assertThat(schema.triggers()).hasSize(12);
    }

    private void assertEveryTableIsPlaced(DatabaseSchemaSnapshot schema, String documentation) {
        for (String table : schema.tables()) {
            assertThat(documentation)
                    .as("database documentation must place table %s", table)
                    .contains("`" + table + "`");
        }
    }

    private void assertEveryForeignKeyIsDocumented(
            DatabaseSchemaSnapshot schema, String focusedViews) {
        List<String> rows = focusedViews.lines()
                .filter(line -> line.startsWith("| `R"))
                .toList();

        for (ForeignKey foreignKey : schema.foreignKeys()) {
            String childReference = foreignKey.childTable() + "." + foreignKey.childColumns().getFirst();
            List<String> matches = rows.stream()
                    .filter(line -> line.contains("`" + childReference + "`"))
                    .toList();

            assertThat(matches)
                    .as("exactly one relationship row for %s", childReference)
                    .hasSize(1);
            String row = matches.getFirst();
            assertThat(row)
                    .contains("`" + foreignKey.name() + "`")
                    .contains("`" + foreignKey.parentTable() + "`")
                    .contains("`ON DELETE " + foreignKey.onDelete() + "`");
            for (String childColumn : foreignKey.childColumns()) {
                assertThat(row).contains("`" + foreignKey.childTable() + "." + childColumn + "`");
            }
            if (foreignKey.nullable()) {
                assertThat(row).contains("nullable `FK`");
            } else {
                assertThat(row).containsAnyOf("`NOT NULL`", "composite `PK`");
            }
        }
    }

    private void assertReferencedObjectsExist(
            DatabaseSchemaSnapshot schema, String documentation) {
        Set<String> databaseObjects = new TreeSet<>();
        databaseObjects.addAll(names(schema.uniqueConstraints()));
        databaseObjects.addAll(names(schema.checkConstraints()));
        databaseObjects.addAll(names(schema.partialIndexes()));
        databaseObjects.addAll(names(schema.triggers()));

        Set<String> documentedObjects = DOCUMENTED_OBJECT.matcher(documentation).results()
                .map(match -> match.group(1))
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(databaseObjects)
                .as("every named database object referenced by documentation must exist")
                .containsAll(documentedObjects);
    }

    private void assertCorrelationsReferenceExistingColumns(
            DatabaseSchemaSnapshot schema, String reference) {
        Set<String> columns = schema.columns().stream()
                .map(column -> column.table() + "." + column.name())
                .collect(Collectors.toSet());
        List<Correlation> correlations = new ArrayList<>();
        reference.lines().forEach(line -> {
            Matcher matcher = CORRELATION_ROW.matcher(line);
            if (matcher.matches()) {
                correlations.add(new Correlation(
                        matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)));
            }
        });

        assertThat(reference).contains("## Intentional non-FK correlations");
        assertThat(correlations).hasSize(6);
        for (Correlation correlation : correlations) {
            assertThat(correlation.basis().toLowerCase())
                    .as("correlation %s must be visibly non-FK", correlation.id())
                    .matches(".*\\bno\\b.*\\bforeign key\\b.*");
            assertReferenceColumnsExist(correlation.id(), correlation.from(), schema.tables(), columns);
            assertReferenceColumnsExist(correlation.id(), correlation.to(), schema.tables(), columns);
        }
    }

    private void assertReferenceColumnsExist(
            String correlationId, String reference, List<String> tables, Set<String> columns) {
        int separator = reference.indexOf('.');
        assertThat(separator).as("qualified correlation reference %s", reference).isPositive();
        String table = reference.substring(0, separator);
        String columnExpression = reference.substring(separator + 1);
        assertThat(tables).as("table for correlation %s", correlationId).contains(table);

        String normalized = columnExpression
                .replace("(", "")
                .replace(")", "");
        for (String column : normalized.split("\\s*,\\s*")) {
            assertThat(columns)
                    .as("column for correlation %s", correlationId)
                    .contains(table + "." + column);
        }
    }

    private String focusedViewDocumentation() throws Exception {
        try (var paths = Files.list(DATABASE_ROOT.resolve("views"))) {
            return paths.filter(path -> path.toString().endsWith(".md"))
                    .filter(path -> !path.getFileName().toString().equals("README.md"))
                    .sorted()
                    .map(this::read)
                    .collect(Collectors.joining("\n"));
        }
    }

    private String databaseDocumentation() throws Exception {
        try (var paths = Files.walk(DATABASE_ROOT)) {
            return paths.filter(path -> path.toString().endsWith(".md"))
                    .sorted()
                    .map(this::read)
                    .collect(Collectors.joining("\n"));
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read " + path, exception);
        }
    }

    private Set<String> names(List<NamedObject> values) {
        return values.stream().map(NamedObject::name).collect(Collectors.toSet());
    }

    private record Correlation(String id, String from, String to, String basis) {
    }
}

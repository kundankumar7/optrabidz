package com.project.optrabidz.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

class FrameworkDependencyPolicyTest {

    private static final Path POM = Path.of("pom.xml").toAbsolutePath().normalize();
    private static final List<Path> SOURCE_ROOTS = List.of(
            Path.of("src", "main", "java"),
            Path.of("src", "test", "java"));
    private static final Pattern JACKSON_2_IMPORT = Pattern.compile(
            "^\\s*import\\s+(?:static\\s+)?com\\.fasterxml\\.jackson\\.(core|databind|datatype)(?:\\.|;)");

    @Test
    void requiresSpringBoot411AndJava21() throws Exception {
        Document pom = loadPom();
        Element project = pom.getDocumentElement();
        Element parent = requiredChild(project, "parent");

        assertThat(requiredText(parent, "groupId")).isEqualTo("org.springframework.boot");
        assertThat(requiredText(parent, "artifactId")).isEqualTo("spring-boot-starter-parent");
        assertThat(requiredText(parent, "version")).isEqualTo("4.1.1");

        Element properties = requiredChild(project, "properties");
        assertThat(requiredText(properties, "java.version")).isEqualTo("21");
    }

    @Test
    void requiresApprovedSpringBootModules() throws Exception {
        assertThat(directDependencies(loadPom())).contains(
                "org.springframework.boot:spring-boot-starter-actuator",
                "org.springframework.boot:spring-boot-starter-data-jpa",
                "org.springframework.boot:spring-boot-starter-mail",
                "org.springframework.boot:spring-boot-starter-security",
                "org.springframework.boot:spring-boot-starter-validation",
                "org.springframework.boot:spring-boot-starter-flyway",
                "org.springframework.boot:spring-boot-starter-webmvc",
                "org.springframework.boot:spring-boot-starter-webmvc-test",
                "org.springframework.boot:spring-boot-starter-data-jpa-test",
                "org.springframework.boot:spring-boot-starter-security-test");
    }

    @Test
    void rejectsLegacyAndCompatibilityDependencies() throws Exception {
        assertThat(directDependencies(loadPom())).doesNotContain(
                "org.springframework.boot:spring-boot-starter-web",
                "org.springframework.boot:spring-boot-starter-json",
                "org.springframework.boot:spring-boot-starter-test",
                "org.springframework.boot:spring-boot-starter-test-classic",
                "org.springframework.boot:spring-boot-testcontainers",
                "org.springframework.boot:spring-boot-jackson2",
                "org.springframework.security:spring-security-test",
                "org.flywaydb:flyway-core",
                "com.fasterxml.jackson.core:jackson-core",
                "com.fasterxml.jackson.core:jackson-databind",
                "com.fasterxml.jackson.datatype:jackson-datatype-jsr310");
    }

    @Test
    void rejectsJackson2SourceImports() throws IOException {
        Set<String> violations = new LinkedHashSet<>();

        for (Path sourceRoot : SOURCE_ROOTS) {
            if (!Files.exists(sourceRoot)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(sourceRoot)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    List<String> lines = Files.readAllLines(file);
                    for (int index = 0; index < lines.size(); index++) {
                        if (JACKSON_2_IMPORT.matcher(lines.get(index)).find()) {
                            violations.add(file.toString().replace('\\', '/') + ":" + (index + 1));
                        }
                    }
                }
            }
        }

        assertThat(violations)
                .as("Jackson 2 imports must not re-enter production or test sources")
                .isEmpty();
    }

    private static Document loadPom()
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(POM.toFile());
    }

    private static Set<String> directDependencies(Document pom) {
        Element dependencies = requiredChild(pom.getDocumentElement(), "dependencies");
        return childElements(dependencies, "dependency").stream()
                .map(dependency -> requiredText(dependency, "groupId") + ":"
                        + requiredText(dependency, "artifactId"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Element requiredChild(Element parent, String name) {
        return childElements(parent, name).stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing <" + name + "> under <" + parent.getTagName() + "> in pom.xml"));
    }

    private static String requiredText(Element parent, String name) {
        return requiredChild(parent, name).getTextContent().trim();
    }

    private static List<Element> childElements(Element parent, String name) {
        java.util.ArrayList<Element> matches = new java.util.ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && element.getTagName().equals(name)) {
                matches.add(element);
            }
        }
        return matches;
    }
}

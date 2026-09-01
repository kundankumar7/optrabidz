package com.project.optrabidz.documentation.error;

import com.project.optrabidz.common.api.error.FrameworkProblem;
import com.project.optrabidz.common.api.error.ProblemTypeUri;
import com.project.optrabidz.common.api.error.SecurityProblem;
import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicErrorCatalogueTest {

    @Test
    void normalizesModuleErrorsThroughTheRuntimeMapping() {
        PublicErrorDefinition definition = PublicErrorDefinition.fromModule(
                "marketplace",
                MarketplaceErrors.LISTING_NOT_FOUND
        );

        assertThat(definition.code()).isEqualTo("LISTING_NOT_FOUND");
        assertThat(definition.status()).isEqualTo(404);
        assertThat(definition.title()).isEqualTo("Resource not found");
        assertThat(definition.detail())
                .isEqualTo("The requested listing was not found");
        assertThat(definition.type())
                .hasToString("urn:optrabidz:problem:listing-not-found");
        assertThat(definition.category())
                .contains(ErrorCategory.NOT_FOUND);
        assertThat(definition.sources()).containsExactly("marketplace");
    }

    @Test
    void mergesOnlyIdenticalDuplicateContracts() {
        PublicErrorCatalogue catalogue = PublicErrorCatalogue.createDefault();

        PublicErrorDefinition authorization = catalogue.entries().stream()
                .filter(entry -> entry.code().equals("AUTHORIZATION_FAILED"))
                .findFirst()
                .orElseThrow();

        assertThat(authorization.sources()).containsExactly(
                "participation",
                "security-application",
                "spring-security"
        );
        assertThat(catalogue.entries()).hasSize(69);
    }

    @Test
    void rejectsTwoMeaningsForOneCode() {
        PublicErrorDefinition first = definition(
                "DUPLICATE_CODE", 409, "Request conflict", "First meaning", "a"
        );
        PublicErrorDefinition second = definition(
                "DUPLICATE_CODE", 422, "Business rule violation", "Second meaning", "b"
        );

        assertThatThrownBy(() -> PublicErrorCatalogue.from(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DUPLICATE_CODE")
                .hasMessageContaining("a")
                .hasMessageContaining("b");
    }

    @Test
    void publishesUniqueCodesInStableOrder() {
        List<PublicErrorDefinition> entries =
                PublicErrorCatalogue.createDefault().entries();
        List<String> codes = entries.stream()
                .map(PublicErrorDefinition::code)
                .toList();

        assertThat(codes).doesNotHaveDuplicates();
        assertThat(codes).isSorted();
    }

    @Test
    void includesEveryFixedFrameworkAndSecurityProblem() {
        PublicErrorCatalogue catalogue = PublicErrorCatalogue.createDefault();

        assertThat(catalogue.entries())
                .filteredOn(entry -> entry.sources().contains("spring-mvc"))
                .extracting(PublicErrorDefinition::code)
                .containsExactlyInAnyOrder(
                        java.util.Arrays.stream(FrameworkProblem.values())
                                .map(FrameworkProblem::code)
                                .toArray(String[]::new)
                );
        assertThat(catalogue.entries())
                .filteredOn(entry -> entry.sources().contains("spring-security"))
                .extracting(PublicErrorDefinition::code)
                .containsExactlyInAnyOrder(
                        java.util.Arrays.stream(SecurityProblem.values())
                                .map(SecurityProblem::code)
                                .toArray(String[]::new)
                );
    }

    @Test
    void publishedEntriesAndSourceOwnersAreImmutable() {
        PublicErrorCatalogue catalogue = PublicErrorCatalogue.createDefault();
        PublicErrorDefinition first = catalogue.entries().getFirst();

        assertThatThrownBy(() -> catalogue.entries().add(first))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> first.sources().add("unexpected"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static PublicErrorDefinition definition(
            String code,
            int status,
            String title,
            String detail,
            String source
    ) {
        SortedSet<String> sources = new TreeSet<>();
        sources.add(source);
        return new PublicErrorDefinition(
                code,
                status,
                title,
                detail,
                ProblemTypeUri.fromCode(code),
                Optional.of(ErrorCategory.CONFLICT),
                sources
        );
    }
}

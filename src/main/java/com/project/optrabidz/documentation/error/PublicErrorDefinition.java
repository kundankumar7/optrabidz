package com.project.optrabidz.documentation.error;

import com.project.optrabidz.common.api.error.FrameworkProblem;
import com.project.optrabidz.common.api.error.HttpErrorMapping;
import com.project.optrabidz.common.api.error.ProblemTypeUri;
import com.project.optrabidz.common.api.error.SecurityProblem;
import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;

import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public record PublicErrorDefinition(
        String code,
        int status,
        String title,
        String detail,
        URI type,
        Optional<ErrorCategory> category,
        SortedSet<String> sources
) {

    public PublicErrorDefinition {
        code = requireText(code, "code");
        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("status must be 400..599");
        }
        title = requireText(title, "title");
        detail = requireText(detail, "detail");
        type = Objects.requireNonNull(type, "type must not be null");
        category = Objects.requireNonNull(
                category,
                "category must not be null"
        );
        Objects.requireNonNull(sources, "sources must not be null");
        TreeSet<String> sourceCopy = new TreeSet<>();
        for (String source : sources) {
            sourceCopy.add(requireText(source, "source"));
        }
        if (sourceCopy.isEmpty()) {
            throw new IllegalArgumentException("sources must not be empty");
        }
        sources = Collections.unmodifiableSortedSet(sourceCopy);
    }

    public static PublicErrorDefinition fromModule(
            String source,
            ErrorDescriptor descriptor
    ) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        HttpErrorMapping mapping = HttpErrorMapping.forCategory(
                descriptor.category()
        );
        return definition(
                source,
                descriptor.code(),
                mapping,
                descriptor.publicMessage(),
                Optional.of(descriptor.category())
        );
    }

    public static PublicErrorDefinition fromFramework(
            FrameworkProblem problem
    ) {
        Objects.requireNonNull(problem, "problem must not be null");
        return definition(
                "spring-mvc",
                problem.code(),
                problem.mapping(),
                problem.detail(),
                Optional.empty()
        );
    }

    public static PublicErrorDefinition fromSecurity(SecurityProblem problem) {
        Objects.requireNonNull(problem, "problem must not be null");
        ErrorCategory category = problem == SecurityProblem.AUTHENTICATION_REQUIRED
                ? ErrorCategory.AUTHENTICATION
                : ErrorCategory.AUTHORIZATION;
        return definition(
                "spring-security",
                problem.code(),
                problem.mapping(),
                problem.detail(),
                Optional.of(category)
        );
    }

    public boolean sameContract(PublicErrorDefinition other) {
        Objects.requireNonNull(other, "other must not be null");
        return code.equals(other.code)
                && status == other.status
                && title.equals(other.title)
                && detail.equals(other.detail)
                && type.equals(other.type);
    }

    public PublicErrorDefinition withSources(SortedSet<String> merged) {
        return new PublicErrorDefinition(
                code,
                status,
                title,
                detail,
                type,
                category,
                merged
        );
    }

    private static PublicErrorDefinition definition(
            String source,
            String code,
            HttpErrorMapping mapping,
            String detail,
            Optional<ErrorCategory> category
    ) {
        Objects.requireNonNull(mapping, "mapping must not be null");
        return new PublicErrorDefinition(
                code,
                mapping.status().value(),
                mapping.title(),
                detail,
                ProblemTypeUri.fromCode(code),
                category,
                new TreeSet<>(Set.of(source))
        );
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}

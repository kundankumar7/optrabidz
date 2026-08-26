package com.project.optrabidz.documentation.error;

import com.project.optrabidz.classification.application.error.ClassificationErrors;
import com.project.optrabidz.common.api.error.FrameworkProblem;
import com.project.optrabidz.common.api.error.SecurityProblem;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.financial.application.error.FinancialErrors;
import com.project.optrabidz.governance.application.error.GovernanceErrors;
import com.project.optrabidz.identity.application.error.IdentityErrors;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;
import com.project.optrabidz.notification.application.error.NotificationErrors;
import com.project.optrabidz.participation.application.error.AdminErrors;
import com.project.optrabidz.participation.application.error.InvestorErrors;
import com.project.optrabidz.participation.application.error.ParticipationErrors;
import com.project.optrabidz.participation.application.error.StartupErrors;
import com.project.optrabidz.security.application.error.SecurityErrors;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCatalogueInventoryTest {

    private static final List<Class<?>> MODULE_CATALOGUES = List.of(
            ClassificationErrors.class,
            IdentityErrors.class,
            SecurityErrors.class,
            FinancialErrors.class,
            AdminErrors.class,
            InvestorErrors.class,
            ParticipationErrors.class,
            StartupErrors.class,
            MarketplaceErrors.class,
            NotificationErrors.class,
            GovernanceErrors.class
    );

    private static final Map<Class<?>, String> MODULE_SOURCES =
            moduleSources();

    @Test
    void explicitCollectionsContainEveryDeclaredDescriptorInstance()
            throws ReflectiveOperationException {
        int declaredFieldCount = 0;

        for (Class<?> catalogue : MODULE_CATALOGUES) {
            Set<ErrorDescriptor> declared = identitySet();
            for (Field field : catalogue.getDeclaredFields()) {
                if (isPublicStaticDescriptor(field)) {
                    declared.add((ErrorDescriptor) field.get(null));
                    declaredFieldCount++;
                }
            }

            Method descriptorsMethod = catalogue.getMethod("descriptors");
            Object result = descriptorsMethod.invoke(null);
            assertThat(result).isInstanceOf(List.class);

            Set<ErrorDescriptor> registered = identitySet();
            for (Object candidate : new ArrayList<>((List<?>) result)) {
                assertThat(candidate).isInstanceOf(ErrorDescriptor.class);
                registered.add((ErrorDescriptor) candidate);
            }

            assertThat(registered)
                    .as("explicit descriptor inventory for %s", catalogue.getName())
                    .isEqualTo(declared);
        }

        assertThat(declaredFieldCount).isEqualTo(61);
    }

    @Test
    void aggregateCatalogueRepresentsEveryApprovedSource()
            throws IllegalAccessException {
        List<PublicErrorDefinition> entries =
                PublicErrorCatalogue.createDefault().entries();

        for (Class<?> catalogue : MODULE_CATALOGUES) {
            String source = MODULE_SOURCES.get(catalogue);
            for (Field field : catalogue.getDeclaredFields()) {
                if (isPublicStaticDescriptor(field)) {
                    ErrorDescriptor descriptor = (ErrorDescriptor) field.get(null);
                    assertThat(entries)
                            .as("aggregate entry for %s.%s", catalogue.getName(), field.getName())
                            .anySatisfy(entry -> {
                                assertThat(entry.code()).isEqualTo(descriptor.code());
                                assertThat(entry.sources()).contains(source);
                            });
                }
            }
        }

        for (FrameworkProblem problem : FrameworkProblem.values()) {
            assertThat(entries)
                    .anySatisfy(entry -> {
                        assertThat(entry.code()).isEqualTo(problem.code());
                        assertThat(entry.sources()).contains("spring-mvc");
                    });
        }
        for (SecurityProblem problem : SecurityProblem.values()) {
            assertThat(entries)
                    .anySatisfy(entry -> {
                        assertThat(entry.code()).isEqualTo(problem.code());
                        assertThat(entry.sources()).contains("spring-security");
                    });
        }
    }

    private static boolean isPublicStaticDescriptor(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers)
                && Modifier.isStatic(modifiers)
                && field.getType().equals(ErrorDescriptor.class);
    }

    private static Set<ErrorDescriptor> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static Map<Class<?>, String> moduleSources() {
        Map<Class<?>, String> sources = new LinkedHashMap<>();
        sources.put(ClassificationErrors.class, "classification");
        sources.put(IdentityErrors.class, "identity");
        sources.put(SecurityErrors.class, "security-application");
        sources.put(FinancialErrors.class, "financial");
        sources.put(AdminErrors.class, "participation-admin");
        sources.put(InvestorErrors.class, "participation-investor");
        sources.put(ParticipationErrors.class, "participation");
        sources.put(StartupErrors.class, "participation-startup");
        sources.put(MarketplaceErrors.class, "marketplace");
        sources.put(NotificationErrors.class, "notification");
        sources.put(GovernanceErrors.class, "governance");
        return Map.copyOf(sources);
    }
}

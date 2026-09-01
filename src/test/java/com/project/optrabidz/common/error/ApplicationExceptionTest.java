package com.project.optrabidz.common.error;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationExceptionTest {
    private static final ErrorDescriptor LISTING_NOT_FOUND = new ErrorDescriptor(
            "LISTING_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The listing was not found"
    );

    @Test
    void keepsPublicContractSeparateFromProtectedDiagnostics() {
        IllegalStateException cause = new IllegalStateException("database detail");
        List<ErrorDetail> source = new ArrayList<>(List.of(
                new ErrorDetail("listingId", "was not found")
        ));

        ApplicationException exception = new ApplicationException(
                LISTING_NOT_FOUND,
                "MARKETPLACE.LISTING_LOOKUP_FAILED",
                "Listing 42 was absent during bid placement",
                source,
                cause
        );
        source.clear();

        assertThat(exception.descriptor()).isSameAs(LISTING_NOT_FOUND);
        assertThat(exception.diagnosticCode())
                .isEqualTo("MARKETPLACE.LISTING_LOOKUP_FAILED");
        assertThat(exception.getMessage())
                .isEqualTo("Listing 42 was absent during bid placement")
                .isNotEqualTo(exception.descriptor().publicMessage());
        assertThat(exception.details())
                .containsExactly(new ErrorDetail("listingId", "was not found"));
        assertThat(exception.getCause()).isSameAs(cause);
        assertThatThrownBy(() -> exception.details().add(
                new ErrorDetail("probe", "must be rejected")
        )).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void defaultsOptionalDetailsAndCause() {
        ApplicationException exception = new ApplicationException(
                LISTING_NOT_FOUND,
                "MARKETPLACE.LISTING_NOT_FOUND",
                "Listing lookup returned no row"
        );

        assertThat(exception.details()).isEmpty();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void rejectsInvalidDiagnosticContract() {
        assertThatThrownBy(() -> new ApplicationException(
                LISTING_NOT_FOUND, "invalid code", "Diagnostic"
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApplicationException(
                LISTING_NOT_FOUND, "MARKETPLACE.FAILURE", " "
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApplicationException(
                null, "MARKETPLACE.FAILURE", "Diagnostic"
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void supportsModuleOwnedExceptionTypes() {
        ListingNotFoundException exception = new ListingNotFoundException(42L);

        assertThat(exception.descriptor()).isSameAs(LISTING_NOT_FOUND);
        assertThat(exception.getMessage()).contains("42");
    }

    private static final class ListingNotFoundException extends ApplicationException {
        private ListingNotFoundException(long listingId) {
            super(
                    LISTING_NOT_FOUND,
                    "MARKETPLACE.LISTING_NOT_FOUND",
                    "Listing " + listingId + " was not found"
            );
        }
    }
}

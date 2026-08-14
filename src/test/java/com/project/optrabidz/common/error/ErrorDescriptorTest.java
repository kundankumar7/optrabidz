package com.project.optrabidz.common.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErrorDescriptorTest {
    @Test
    void exposesStablePublicContract() {
        ErrorDescriptor descriptor = new ErrorDescriptor(
                "LISTING_NOT_FOUND",
                ErrorCategory.NOT_FOUND,
                "The listing was not found"
        );

        assertThat(descriptor.code()).isEqualTo("LISTING_NOT_FOUND");
        assertThat(descriptor.category()).isEqualTo(ErrorCategory.NOT_FOUND);
        assertThat(descriptor.publicMessage()).isEqualTo("The listing was not found");
    }

    @Test
    void rejectsMissingOrUnstableDescriptorValues() {
        assertThatThrownBy(() -> new ErrorDescriptor(null, ErrorCategory.NOT_FOUND, "Safe"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ErrorDescriptor("listing-not-found", ErrorCategory.NOT_FOUND, "Safe"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ErrorDescriptor("LISTING_NOT_FOUND", null, "Safe"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ErrorDescriptor("LISTING_NOT_FOUND", ErrorCategory.NOT_FOUND, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createsImmutableAllowlistedDetail() {
        ErrorDetail detail = new ErrorDetail("title", "must not be blank");

        assertThat(detail.field()).isEqualTo("title");
        assertThat(detail.issue()).isEqualTo("must not be blank");
    }

    @Test
    void rejectsIncompleteDetail() {
        assertThatThrownBy(() -> new ErrorDetail(" ", "must not be blank"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ErrorDetail("title", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

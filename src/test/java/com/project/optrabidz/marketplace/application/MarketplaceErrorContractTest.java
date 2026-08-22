package com.project.optrabidz.marketplace.application;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.marketplace.application.exception.AgreementNotFoundException;
import com.project.optrabidz.marketplace.application.exception.BidAcceptanceConflictException;
import com.project.optrabidz.marketplace.application.exception.BidAlreadyAcceptedException;
import com.project.optrabidz.marketplace.application.exception.BidAlreadyExistsException;
import com.project.optrabidz.marketplace.application.exception.BidNotFoundException;
import com.project.optrabidz.marketplace.application.exception.InvalidBidStateException;
import com.project.optrabidz.marketplace.application.exception.InvalidListingStateException;
import com.project.optrabidz.marketplace.application.exception.ListingNotFoundException;
import com.project.optrabidz.marketplace.application.exception.MarketplaceAccessException;
import com.project.optrabidz.marketplace.application.exception.UnsupportedFundingModelException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.project.optrabidz.marketplace.application.error.MarketplaceErrors.AGREEMENT_NOT_FOUND;
import static com.project.optrabidz.marketplace.application.error.MarketplaceErrors.BID_ACCEPTANCE_CONFLICT;
import static com.project.optrabidz.marketplace.application.error.MarketplaceErrors.BID_ALREADY_EXISTS;
import static com.project.optrabidz.marketplace.application.error.MarketplaceErrors.BID_NOT_FOUND;
import static com.project.optrabidz.marketplace.application.error.MarketplaceErrors.BID_STATE_CONFLICT;
import static com.project.optrabidz.marketplace.application.error.MarketplaceErrors.LISTING_NOT_FOUND;
import static com.project.optrabidz.marketplace.application.error.MarketplaceErrors.LISTING_STATE_CONFLICT;
import static com.project.optrabidz.marketplace.application.error.MarketplaceErrors.MARKETPLACE_ACCESS_DENIED;
import static com.project.optrabidz.marketplace.application.error.MarketplaceErrors.UNSUPPORTED_FUNDING_MODEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MarketplaceErrorContractTest {
    private static final String PROTECTED_DIAGNOSTIC =
            "listing=101 bid=501 role=ADMIN state=CLOSED";

    @ParameterizedTest
    @MethodSource("descriptors")
    void exposesApprovedPublicDescriptors(
            ErrorDescriptor descriptor,
            String code,
            ErrorCategory category,
            String publicMessage
    ) {
        assertThat(descriptor).isEqualTo(new ErrorDescriptor(
                code,
                category,
                publicMessage
        ));
    }

    @ParameterizedTest
    @MethodSource("failures")
    void typedFailuresKeepProtectedDiagnosticsOutOfPublicMessages(
            Function<String, ApplicationException> factory,
            ErrorDescriptor descriptor,
            String diagnosticCode
    ) {
        ApplicationException failure = factory.apply(PROTECTED_DIAGNOSTIC);

        assertThat(failure.descriptor()).isSameAs(descriptor);
        assertThat(failure.diagnosticCode()).isEqualTo(diagnosticCode);
        assertThat(failure.getMessage()).contains(PROTECTED_DIAGNOSTIC);
        assertThat(failure.descriptor().publicMessage())
                .doesNotContain("listing=101", "bid=501", "role=ADMIN", "state=CLOSED");
    }

    private static Stream<Arguments> descriptors() {
        return Stream.of(
                arguments(LISTING_NOT_FOUND, "LISTING_NOT_FOUND",
                        ErrorCategory.NOT_FOUND,
                        "The requested listing was not found"),
                arguments(BID_NOT_FOUND, "BID_NOT_FOUND",
                        ErrorCategory.NOT_FOUND,
                        "The requested bid was not found"),
                arguments(AGREEMENT_NOT_FOUND, "AGREEMENT_NOT_FOUND",
                        ErrorCategory.NOT_FOUND,
                        "The requested agreement was not found"),
                arguments(MARKETPLACE_ACCESS_DENIED, "MARKETPLACE_ACCESS_DENIED",
                        ErrorCategory.AUTHORIZATION,
                        "You are not authorized to perform this marketplace action"),
                arguments(LISTING_STATE_CONFLICT, "LISTING_STATE_CONFLICT",
                        ErrorCategory.CONFLICT,
                        "The requested action conflicts with the current listing state"),
                arguments(BID_STATE_CONFLICT, "BID_STATE_CONFLICT",
                        ErrorCategory.CONFLICT,
                        "The requested action conflicts with the current bid state"),
                arguments(BID_ALREADY_EXISTS, "BID_ALREADY_EXISTS",
                        ErrorCategory.CONFLICT,
                        "An active bid already exists for this listing"),
                arguments(BID_ACCEPTANCE_CONFLICT, "BID_ACCEPTANCE_CONFLICT",
                        ErrorCategory.CONFLICT,
                        "The bid cannot be accepted in the current marketplace state"),
                arguments(UNSUPPORTED_FUNDING_MODEL, "UNSUPPORTED_FUNDING_MODEL",
                        ErrorCategory.BUSINESS_RULE,
                        "The requested funding model is not supported")
        );
    }

    private static Stream<Arguments> failures() {
        return Stream.of(
                arguments((Function<String, ApplicationException>) ListingNotFoundException::new,
                        LISTING_NOT_FOUND, "MARKETPLACE.LISTING.NOT_FOUND"),
                arguments((Function<String, ApplicationException>) BidNotFoundException::new,
                        BID_NOT_FOUND, "MARKETPLACE.BID.NOT_FOUND"),
                arguments((Function<String, ApplicationException>) AgreementNotFoundException::new,
                        AGREEMENT_NOT_FOUND, "MARKETPLACE.AGREEMENT.NOT_FOUND"),
                arguments((Function<String, ApplicationException>) MarketplaceAccessException::new,
                        MARKETPLACE_ACCESS_DENIED, "MARKETPLACE.ACCESS.DENIED"),
                arguments((Function<String, ApplicationException>) InvalidListingStateException::new,
                        LISTING_STATE_CONFLICT, "MARKETPLACE.LISTING.STATE_CONFLICT"),
                arguments((Function<String, ApplicationException>) InvalidBidStateException::new,
                        BID_STATE_CONFLICT, "MARKETPLACE.BID.STATE_CONFLICT"),
                arguments((Function<String, ApplicationException>) BidAlreadyExistsException::new,
                        BID_ALREADY_EXISTS, "MARKETPLACE.BID.ALREADY_EXISTS"),
                arguments((Function<String, ApplicationException>) BidAlreadyAcceptedException::new,
                        BID_ACCEPTANCE_CONFLICT, "MARKETPLACE.BID.ACCEPTANCE_CONFLICT"),
                arguments((Function<String, ApplicationException>) BidAcceptanceConflictException::new,
                        BID_ACCEPTANCE_CONFLICT, "MARKETPLACE.BID.ACCEPTANCE_CONFLICT"),
                arguments((Function<String, ApplicationException>) UnsupportedFundingModelException::new,
                        UNSUPPORTED_FUNDING_MODEL, "MARKETPLACE.FUNDING_MODEL.UNSUPPORTED")
        );
    }
}

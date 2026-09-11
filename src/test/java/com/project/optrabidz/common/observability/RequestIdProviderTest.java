package com.project.optrabidz.common.observability;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdProviderTest {
    @Test
    void publishesTheRequestAttributeOwnedByObservability() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String requestId = RequestIdProvider.resolveOrCreate(request);

        assertThat(RequestIdProvider.REQUEST_ID_ATTRIBUTE).isEqualTo("optrabidz.requestId");
        assertThat(request.getAttribute(RequestIdProvider.REQUEST_ID_ATTRIBUTE)).isEqualTo(requestId);
    }

    @Test
    void reusesAnExistingRequestAttributeBeforeReadingTheHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestIdProvider.REQUEST_ID_ATTRIBUTE, "existing-request-123");
        request.addHeader(RequestIdProvider.REQUEST_ID_HEADER, "header-request-456");

        assertThat(RequestIdProvider.resolveOrCreate(request))
                .isEqualTo("existing-request-123");
    }

    @Test
    void reusesASafeInboundRequestId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdProvider.REQUEST_ID_HEADER, "client-ID_123.456");

        assertThat(RequestIdProvider.resolveOrCreate(request))
                .isEqualTo("client-ID_123.456");
    }

    @Test
    void acceptsARequestIdAtTheMaximumLength() {
        String inboundRequestId = "a".repeat(100);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdProvider.REQUEST_ID_HEADER, inboundRequestId);

        assertThat(RequestIdProvider.resolveOrCreate(request))
                .isEqualTo(inboundRequestId);
    }

    @Test
    void replacesAnUnsafeOrOversizedInboundRequestId() {
        MockHttpServletRequest unsafeRequest = new MockHttpServletRequest();
        unsafeRequest.addHeader(RequestIdProvider.REQUEST_ID_HEADER, "invalid request id!");
        MockHttpServletRequest oversizedRequest = new MockHttpServletRequest();
        oversizedRequest.addHeader(RequestIdProvider.REQUEST_ID_HEADER, "a".repeat(101));

        assertThat(RequestIdProvider.resolveOrCreate(unsafeRequest))
                .isNotEqualTo("invalid request id!")
                .matches("[A-Za-z0-9._-]+");
        assertThat(RequestIdProvider.resolveOrCreate(oversizedRequest))
                .isNotEqualTo("a".repeat(101))
                .matches("[A-Za-z0-9._-]+");
    }

    @Test
    void createsAnIdentifierWithoutARequestObject() {
        assertThat(RequestIdProvider.resolveOrCreate(null))
                .isNotBlank()
                .matches("[A-Za-z0-9._-]+");
    }
}

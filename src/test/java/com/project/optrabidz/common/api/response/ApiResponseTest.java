package com.project.optrabidz.common.api.response;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {
    @Test
    void preservesTheExistingSuccessContract() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(
                ApiResponse.REQUEST_ID_ATTRIBUTE,
                "success-request-123"
        );

        SuccessResponse<String> response = ApiResponse.success("ok", request);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("ok");
        assertThat(response.meta().requestId())
                .isEqualTo("success-request-123");
        assertThat(response.meta().timestamp()).isNotNull();
    }

    @Test
    void exposesNoLegacyErrorFactory() {
        assertThat(ApiResponse.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().equals("error"));
    }
}

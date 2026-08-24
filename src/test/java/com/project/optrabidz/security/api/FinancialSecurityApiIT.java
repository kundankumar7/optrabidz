package com.project.optrabidz.security.api;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FinancialSecurityApiIT extends ApiIntegrationTestSupport {
    private static final String REQUEST_ID = "financial-security-request-123";

    @ParameterizedTest
    @MethodSource("protectedFinancialReadPaths")
    void financialReadRequiresAuthentication(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)
                        .header("X-Request-Id", REQUEST_ID)
                        .header("Authorization", "Bearer secret-financial-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(
                        "urn:optrabidz:problem:authentication-required"))
                .andExpect(jsonPath("$.title").value("Authentication required"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication is required"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("secret-financial-token")
                .doesNotContain("Authorization")
                .doesNotContain("ApiException");
    }

    static Stream<String> protectedFinancialReadPaths() {
        return Stream.of(
                "/api/v1/settlements/1",
                "/api/v1/repayments/1",
                "/api/v1/repayment-installments/1",
                "/api/v1/payment-intents/1"
        );
    }

    @Test
    void paymentAttemptActionRequiresAuthenticationAfterCsrfValidation()
            throws Exception {
        mockMvc.perform(post(
                        "/api/v1/payment-attempts/1/actions/local-confirm")
                        .with(csrf())
                        .header("X-Request-Id", REQUEST_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(
                        "AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }
}

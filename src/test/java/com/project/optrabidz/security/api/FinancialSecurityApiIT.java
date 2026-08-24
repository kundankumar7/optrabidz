package com.project.optrabidz.security.api;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
        MvcResult csrfPrimingResult = mockMvc.perform(get("/api/v1/funding-listings"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie xsrfCookie = csrfPrimingResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrfCookie).isNotNull();

        mockMvc.perform(post(
                        "/api/v1/payment-attempts/1/actions/local-confirm")
                        .cookie(xsrfCookie)
                        .header("X-CSRF-TOKEN", xsrfCookie.getValue())
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

    @Test
    void authenticatedFinancialReadReachesTheApplicationBoundary()
            throws Exception {
        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);

        mockMvc.perform(get("/api/v1/settlements/{settlementId}", Long.MAX_VALUE)
                        .session(investor.session())
                        .cookie(investor.xsrfCookie()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Settlement not found"));
    }

    @Test
    void csrfValidAuthenticatedPaymentMutationReachesFinancialService()
            throws Exception {
        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);

        mockMvc.perform(post("/api/v1/payment-intents/{paymentIntentId}/attempts", Long.MAX_VALUE)
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Payment intent not found"));
    }

    @Test
    void authenticatedPaymentMutationStillRequiresCsrf() throws Exception {
        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);

        mockMvc.perform(post("/api/v1/payment-intents/1/attempts")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-Request-Id", REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }
}

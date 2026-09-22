package com.project.optrabidz.common.observability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

class RuntimeHealthApiIT extends ApiIntegrationTestSupport {

    private static final MediaType ACTUATOR_JSON =
            MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json");

    @Test
    void returnsStatusOnlyLivenessAndReadinessWhenDatabaseIsHealthy() throws Exception {
        assertHealthyStatusOnlyProbe("/actuator/health/liveness");
        assertHealthyStatusOnlyProbe("/actuator/health/readiness");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/actuator",
            "/actuator/health",
            "/actuator/env"
    })
    void rejectsUnclassifiedManagementEndpoints(String path) throws Exception {
        mockMvc.perform(get(path)
                        .header("X-Request-Id",
                                "management-default-deny-request"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(
                        "AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.requestId").value(
                        "management-default-deny-request"));
    }

    private void assertHealthyStatusOnlyProbe(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(ACTUATOR_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.groups").doesNotExist());
    }
}

package com.project.optrabidz.common.observability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class RuntimeHealthApiIT extends ApiIntegrationTestSupport {

    private static final MediaType ACTUATOR_JSON =
            MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json");

    @Test
    void returnsStatusOnlyLivenessAndReadinessWhenDatabaseIsHealthy() throws Exception {
        assertHealthyStatusOnlyProbe("/actuator/health/liveness");
        assertHealthyStatusOnlyProbe("/actuator/health/readiness");
    }

    @Test
    void doesNotExposeSensitiveManagementEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isNotFound());
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

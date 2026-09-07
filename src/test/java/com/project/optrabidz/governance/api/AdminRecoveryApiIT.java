package com.project.optrabidz.governance.api;

import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "optrabidz.admin.recovery.enabled=true",
        "optrabidz.admin.recovery.token=test-recovery-token-material-027"
})
class AdminRecoveryApiIT extends ApiIntegrationTestSupport {

    private static final String CONFIGURED_TOKEN = "test-recovery-token-material-027";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void invalidRecoveryTokenReturnsSafeAuthorizationProblem() throws Exception {
        String requestId = "kan-27-recovery-denied";
        String rejectedToken = "rejected-recovery-token-27";

        MvcResult result = mockMvc.perform(post("/api/v1/admin/recovery/transfer")
                        .header("X-ADMIN-RECOVERY-TOKEN", rejectedToken)
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validTransferRequest())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("X-Request-Id", requestId))
                .andExpect(jsonPath("$.type").value(
                        "urn:optrabidz:problem:admin-recovery-access-denied"))
                .andExpect(jsonPath("$.title").value("Access denied"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Admin recovery access was denied"))
                .andExpect(jsonPath("$.instance").value(
                        "urn:optrabidz:request:" + requestId))
                .andExpect(jsonPath("$.code").value("ADMIN_RECOVERY_ACCESS_DENIED"))
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(rejectedToken, CONFIGURED_TOKEN,
                        "GOVERNANCE.RECOVERY.TOKEN_REJECTED");
    }

    @Test
    void configuredRecoveryTokenWithoutActiveAdminReturnsSafeConflict() throws Exception {
        String requestId = "kan-27-admin-unavailable";
        assertThat(activeAdminCount()).isZero();

        MvcResult result = mockMvc.perform(post("/api/v1/admin/recovery/transfer")
                        .header("X-ADMIN-RECOVERY-TOKEN", CONFIGURED_TOKEN)
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validTransferRequest())))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("X-Request-Id", requestId))
                .andExpect(jsonPath("$.type").value(
                        "urn:optrabidz:problem:admin-authority-unavailable"))
                .andExpect(jsonPath("$.title").value("Request conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "No active administrator authority is available for transfer"))
                .andExpect(jsonPath("$.instance").value(
                        "urn:optrabidz:request:" + requestId))
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHORITY_UNAVAILABLE"))
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andReturn();

        assertThat(activeAdminCount()).isZero();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(CONFIGURED_TOKEN,
                        "GOVERNANCE.ADMIN_AUTHORITY.UNAVAILABLE");
    }

    private Map<String, Object> validTransferRequest() {
        return Map.of(
                "newAdminEmail", uniqueEmail("recovery-admin"),
                "newAdminRawPassword", DEFAULT_PASSWORD,
                "newPublicDisplayName", "Recovery Administrator",
                "newOrganizationLabel", "OptraBidz Operations",
                "revocationReason", "Integration test recovery transfer"
        );
    }

    private long activeAdminCount() {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from admin where admin_state = 'ACTIVE'",
                Long.class
        );
        return count == null ? 0 : count;
    }
}

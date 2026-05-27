package com.project.optrabidz.audit.api;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityAuditIT extends ApiIntegrationTestSupport {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void failedLoginCreatesMaskedSecurityAuditRecord() throws Exception {
        String email = uniqueEmail("audit-startup");
        register(email, DEFAULT_PASSWORD, RoleType.STARTUP)
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "WrongPassword01"
                        ))))
                .andExpect(status().isUnauthorized());

        Map<String, Object> audit = jdbcTemplate.queryForMap("""
                select action, outcome, object_id, details::text as details
                from audit_record
                where source_module = 'SECURITY'
                  and action = 'LOGIN_FAILED'
                order by audit_record_id desc
                limit 1
                """);

        assertThat(audit.get("outcome")).isEqualTo("FAILED");
        assertThat(audit.get("object_id").toString()).contains("@example.com");
        assertThat(audit.get("details").toString()).contains("Invalid credentials");
        assertThat(audit.get("details").toString()).doesNotContain("WrongPassword01");
        assertThat(audit.get("details").toString()).doesNotContain(email);
    }

    @Test
    void deniedAdminAccessCreatesSecurityAuditRecord() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);

        mockMvc.perform(get("/api/v1/admin/audit-records")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isForbidden());

        Map<String, Object> audit = jdbcTemplate.queryForMap("""
                select action, outcome, actor_account_id, actor_role, object_id, details::text as details
                from audit_record
                where source_module = 'SECURITY'
                  and action = 'AUTHORIZATION_DENIED'
                order by audit_record_id desc
                limit 1
                """);

        assertThat(audit.get("outcome")).isEqualTo("DENIED");
        assertThat(audit.get("actor_account_id")).isNotNull();
        assertThat(audit.get("actor_role")).isEqualTo("STARTUP");
        assertThat(audit.get("object_id")).isEqualTo("/api/v1/admin/audit-records");
        assertThat(audit.get("details").toString()).contains("You are not authorized");
    }
}

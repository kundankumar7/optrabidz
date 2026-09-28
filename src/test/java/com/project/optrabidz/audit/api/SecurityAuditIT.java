package com.project.optrabidz.audit.api;

import com.project.optrabidz.common.outbox.OutboxDispatcher;
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
    private OutboxDispatcher outboxDispatcher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void passwordChangeProducesOneSafeAuditRecordWithActorContext() throws Exception {
        String email = uniqueEmail("audit-password-change");
        register(email, DEFAULT_PASSWORD, RoleType.STARTUP)
                .andExpect(status().isCreated());
        AuthenticatedClient client = login(email, DEFAULT_PASSWORD);
        Long accountId = jdbcTemplate.queryForObject("""
                select account_id
                from credential
                where lower(email) = lower(?)
                """, Long.class, email);
        String newPassword = "ChangedPassword01";

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .session(client.session())
                        .cookie(client.xsrfCookie())
                        .header("X-CSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "currentPassword", DEFAULT_PASSWORD,
                                "newPassword", newPassword
                        ))))
                .andExpect(status().isNoContent());

        String outboxPayload = jdbcTemplate.queryForObject("""
                select payload::text
                from event_outbox
                where event_type = 'CredentialPasswordChangedEvent'
                  and aggregate_id = ?
                """, String.class, String.valueOf(accountId));
        assertThat(outboxPayload)
                .contains("\"accountId\": " + accountId)
                .contains("\"actorRole\": \"STARTUP\"")
                .contains("\"terminatedSessionCount\": 1")
                .doesNotContain(DEFAULT_PASSWORD, newPassword, email, "passwordHash");

        outboxDispatcher.dispatchPending();
        outboxDispatcher.dispatchPending();

        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from audit_record
                where event_type = 'CredentialPasswordChangedEvent'
                  and object_id = ?
                """, Long.class, String.valueOf(accountId))).isEqualTo(1L);

        Map<String, Object> audit = jdbcTemplate.queryForMap("""
                select source_module, action, object_type, actor_account_id,
                       actor_role, outcome, details::text as details
                from audit_record
                where event_type = 'CredentialPasswordChangedEvent'
                  and object_id = ?
                """, String.valueOf(accountId));

        assertThat(audit.get("source_module")).isEqualTo("SECURITY");
        assertThat(audit.get("action")).isEqualTo("PASSWORD_CHANGED");
        assertThat(audit.get("object_type")).isEqualTo("CREDENTIAL");
        assertThat(audit.get("actor_account_id")).isEqualTo(accountId);
        assertThat(audit.get("actor_role")).isEqualTo("STARTUP");
        assertThat(audit.get("outcome")).isEqualTo("SUCCESS");
        assertThat(audit.get("details").toString())
                .contains("\"terminatedSessionCount\": 1")
                .doesNotContain(DEFAULT_PASSWORD, newPassword, email, "passwordHash");
    }

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
        assertThat(audit.get("details").toString())
                .contains("INVALID_SECRET")
                .doesNotContain("UNKNOWN_IDENTITY")
                .doesNotContain("CREDENTIAL_LOCKED")
                .doesNotContain("CREDENTIAL_DISABLED")
                .doesNotContain("ACCOUNT_RESTRICTED")
                .doesNotContain("WrongPassword01")
                .doesNotContain("BadCredentialsException")
                .doesNotContain(email);
    }

    @Test
    void anonymousAccessCreatesStableAuthenticationAuditRecord()
            throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());

        Map<String, Object> audit = jdbcTemplate.queryForMap("""
                select action, outcome, object_id, details::text as details
                from audit_record
                where source_module = 'SECURITY'
                  and action = 'AUTHENTICATION_REQUIRED'
                  and object_id = '/api/v1/me'
                order by audit_record_id desc
                limit 1
                """);

        assertThat(audit.get("outcome")).isEqualTo("DENIED");
        assertThat(audit.get("object_id")).isEqualTo("/api/v1/me");
        assertThat(audit.get("details").toString())
                .contains("AUTHENTICATION_REQUIRED");
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
                  and object_id = '/api/v1/admin/audit-records'
                order by audit_record_id desc
                limit 1
                """);

        assertThat(audit.get("outcome")).isEqualTo("DENIED");
        assertThat(audit.get("actor_account_id")).isNotNull();
        assertThat(audit.get("actor_role")).isEqualTo("STARTUP");
        assertThat(audit.get("object_id")).isEqualTo("/api/v1/admin/audit-records");
        assertThat(audit.get("details").toString())
                .contains("AUTHORIZATION_FAILED")
                .doesNotContain("You are not authorized");
    }

    @Test
    void missingCsrfCreatesStableAuditRecordWithoutTokenDisclosure()
            throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isForbidden());

        Map<String, Object> audit = jdbcTemplate.queryForMap("""
                select action, outcome, object_id, details::text as details
                from audit_record
                where source_module = 'SECURITY'
                  and action = 'AUTHORIZATION_DENIED'
                  and object_id = '/api/v1/auth/logout'
                order by audit_record_id desc
                limit 1
                """);

        String details = audit.get("details").toString();
        assertThat(audit.get("outcome")).isEqualTo("DENIED");
        assertThat(details)
                .contains("CSRF_VALIDATION_FAILED")
                .doesNotContain(startup.csrfToken())
                .doesNotContain("Invalid CSRF token")
                .doesNotContain("Could not verify the provided CSRF token");
    }
}

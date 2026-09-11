package com.project.optrabidz.audit.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAuditApiIT extends ApiIntegrationTestSupport {
    private static final Instant BASE_TIME = Instant.parse("2026-01-15T10:15:30Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void unfilteredSearchReturnsDirectPageContractAndNormalizesSize() throws Exception {
        AuthenticatedClient administrator = administrator();

        mockMvc.perform(get("/api/v1/admin/audit-records")
                        .session(administrator.session())
                        .cookie(administrator.xsrfCookie())
                        .param("page", "1")
                        .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.totalItems").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.meta").doesNotExist());
    }

    @Test
    void eachSupportedFilterExcludesANonmatchingRecord() throws Exception {
        AdminContext context = administratorContext();
        AdminContext otherActor = administratorContext();

        String actorScope = uniqueValue("KAN52_ACTOR");
        long actorMatch = insertAuditRecord(
                context.accountId(), actorScope, "ACTOR", "AUDIT", "actor-match",
                "SUCCESS", BASE_TIME);
        long actorNonmatch = insertAuditRecord(
                otherActor.accountId(), actorScope, "ACTOR", "AUDIT", "actor-nonmatch",
                "SUCCESS", BASE_TIME);
        assertOnlyRecord(context.client(), Map.of(
                "sourceModule", actorScope,
                "actorAccountId", context.accountId().toString()
        ), actorMatch, actorNonmatch);

        String sourceAction = uniqueValue("KAN52_SOURCE_ACTION");
        String sourceMatchValue = uniqueValue("KAN52_SOURCE_MATCH");
        long sourceMatch = insertAuditRecord(
                context.accountId(), sourceMatchValue, sourceAction, "AUDIT", "source-match",
                "SUCCESS", BASE_TIME);
        long sourceNonmatch = insertAuditRecord(
                context.accountId(), uniqueValue("KAN52_SOURCE_OTHER"), sourceAction,
                "AUDIT", "source-nonmatch", "SUCCESS", BASE_TIME);
        assertOnlyRecord(context.client(), Map.of(
                "action", sourceAction,
                "sourceModule", sourceMatchValue
        ), sourceMatch, sourceNonmatch);

        String actionScope = uniqueValue("KAN52_ACTION_SCOPE");
        String actionMatchValue = uniqueValue("KAN52_ACTION_MATCH");
        long actionMatch = insertAuditRecord(
                context.accountId(), actionScope, actionMatchValue, "AUDIT", "action-match",
                "SUCCESS", BASE_TIME);
        long actionNonmatch = insertAuditRecord(
                context.accountId(), actionScope, uniqueValue("KAN52_ACTION_OTHER"),
                "AUDIT", "action-nonmatch", "SUCCESS", BASE_TIME);
        assertOnlyRecord(context.client(), Map.of(
                "sourceModule", actionScope,
                "action", actionMatchValue
        ), actionMatch, actionNonmatch);

        String objectTypeScope = uniqueValue("KAN52_TYPE_SCOPE");
        String objectTypeMatchValue = uniqueValue("KAN52_TYPE_MATCH");
        long objectTypeMatch = insertAuditRecord(
                context.accountId(), objectTypeScope, "TYPE", objectTypeMatchValue,
                "type-match", "SUCCESS", BASE_TIME);
        long objectTypeNonmatch = insertAuditRecord(
                context.accountId(), objectTypeScope, "TYPE", uniqueValue("KAN52_TYPE_OTHER"),
                "type-nonmatch", "SUCCESS", BASE_TIME);
        assertOnlyRecord(context.client(), Map.of(
                "sourceModule", objectTypeScope,
                "objectType", objectTypeMatchValue
        ), objectTypeMatch, objectTypeNonmatch);

        String objectIdScope = uniqueValue("KAN52_ID_SCOPE");
        String objectIdMatchValue = uniqueValue("KAN52_ID_MATCH");
        long objectIdMatch = insertAuditRecord(
                context.accountId(), objectIdScope, "OBJECT_ID", "AUDIT", objectIdMatchValue,
                "SUCCESS", BASE_TIME);
        long objectIdNonmatch = insertAuditRecord(
                context.accountId(), objectIdScope, "OBJECT_ID", "AUDIT",
                uniqueValue("KAN52_ID_OTHER"), "SUCCESS", BASE_TIME);
        assertOnlyRecord(context.client(), Map.of(
                "sourceModule", objectIdScope,
                "objectId", objectIdMatchValue
        ), objectIdMatch, objectIdNonmatch);

        String outcomeScope = uniqueValue("KAN52_OUTCOME_SCOPE");
        long outcomeMatch = insertAuditRecord(
                context.accountId(), outcomeScope, "OUTCOME", "AUDIT", "outcome-match",
                "SUCCESS", BASE_TIME);
        long outcomeNonmatch = insertAuditRecord(
                context.accountId(), outcomeScope, "OUTCOME", "AUDIT", "outcome-nonmatch",
                "FAILED", BASE_TIME);
        assertOnlyRecord(context.client(), Map.of(
                "sourceModule", outcomeScope,
                "outcome", "SUCCESS"
        ), outcomeMatch, outcomeNonmatch);

        String fromScope = uniqueValue("KAN52_FROM_SCOPE");
        long fromBoundary = insertAuditRecord(
                context.accountId(), fromScope, "FROM", "AUDIT", "from-boundary",
                "SUCCESS", BASE_TIME);
        long beforeFrom = insertAuditRecord(
                context.accountId(), fromScope, "FROM", "AUDIT", "before-from",
                "SUCCESS", BASE_TIME.minusSeconds(1));
        assertOnlyRecord(context.client(), Map.of(
                "sourceModule", fromScope,
                "from", BASE_TIME.toString()
        ), fromBoundary, beforeFrom);

        String toScope = uniqueValue("KAN52_TO_SCOPE");
        long toBoundary = insertAuditRecord(
                context.accountId(), toScope, "TO", "AUDIT", "to-boundary",
                "SUCCESS", BASE_TIME);
        long afterTo = insertAuditRecord(
                context.accountId(), toScope, "TO", "AUDIT", "after-to",
                "SUCCESS", BASE_TIME.plusSeconds(1));
        assertOnlyRecord(context.client(), Map.of(
                "sourceModule", toScope,
                "to", BASE_TIME.toString()
        ), toBoundary, afterTo);
    }

    @Test
    void blankFiltersAndLowerPagingBoundsUseSafeDefaults() throws Exception {
        AdminContext context = administratorContext();
        long matchingId = insertAuditRecord(
                context.accountId(), "AUDIT", "BLANK_FILTERS", "AUDIT", "blank-filters",
                "SUCCESS", BASE_TIME);

        MvcResult result = mockMvc.perform(get("/api/v1/admin/audit-records")
                        .session(context.client().session())
                        .cookie(context.client().xsrfCookie())
                        .param("actorAccountId", context.accountId().toString())
                        .param("sourceModule", "  ")
                        .param("action", "  ")
                        .param("objectType", "  ")
                        .param("objectId", "  ")
                        .param("outcome", "  ")
                        .param("from", BASE_TIME.toString())
                        .param("to", BASE_TIME.toString())
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(itemIds(response)).containsExactly(matchingId);
    }

    @Test
    void omittedPagingParametersUseControllerDefaults() throws Exception {
        AdminContext context = administratorContext();
        String sourceModule = uniqueValue("KAN52_DEFAULT_PAGE");
        long matchingId = insertAuditRecord(
                context.accountId(), sourceModule, "DEFAULT_PAGE", "AUDIT", "default-page",
                "SUCCESS", BASE_TIME);

        MvcResult result = mockMvc.perform(get("/api/v1/admin/audit-records")
                        .session(context.client().session())
                        .cookie(context.client().xsrfCookie())
                        .param("sourceModule", sourceModule))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(itemIds(response)).containsExactly(matchingId);
    }

    @Test
    void combinedFiltersExcludeTheNonmatchingRecord() throws Exception {
        AdminContext context = administratorContext();
        String sourceModule = uniqueValue("KAN52_COMBINED");
        long matchingId = insertAuditRecord(
                context.accountId(), sourceModule, "MATCHED", "PAYMENT", "attempt-1",
                "SUCCESS", BASE_TIME);
        long nonmatchingId = insertAuditRecord(
                context.accountId(), sourceModule, "NOT_MATCHED", "PAYMENT", "attempt-2",
                "FAILED", BASE_TIME.plusSeconds(1));

        JsonNode response = search(context.client(), Map.of(
                "actorAccountId", context.accountId().toString(),
                "sourceModule", sourceModule,
                "action", "MATCHED",
                "objectType", "PAYMENT",
                "objectId", "attempt-1",
                "outcome", "SUCCESS",
                "from", BASE_TIME.minusSeconds(1).toString(),
                "to", BASE_TIME.plusSeconds(1).toString()
        ), 1, 20);

        assertThat(itemIds(response)).containsExactly(matchingId).doesNotContain(nonmatchingId);
        assertThat(response.path("totalItems").asLong()).isEqualTo(1);
    }

    @Test
    void paginationOrdersByRecordedAtThenAuditRecordIdDescending() throws Exception {
        AdminContext context = administratorContext();
        String sourceModule = uniqueValue("KAN52_ORDER");
        long firstInsertedId = insertAuditRecord(
                context.accountId(), sourceModule, "ORDERED", "AUDIT", "first",
                "SUCCESS", BASE_TIME);
        long secondInsertedId = insertAuditRecord(
                context.accountId(), sourceModule, "ORDERED", "AUDIT", "second",
                "SUCCESS", BASE_TIME);

        JsonNode firstPage = search(
                context.client(), Map.of("sourceModule", sourceModule), 1, 1);
        JsonNode secondPage = search(
                context.client(), Map.of("sourceModule", sourceModule), 2, 1);

        assertThat(itemIds(firstPage)).containsExactly(secondInsertedId);
        assertThat(itemIds(secondPage)).containsExactly(firstInsertedId);
    }

    private AuthenticatedClient administrator() throws Exception {
        return administratorContext().client();
    }

    private AdminContext administratorContext() throws Exception {
        String email = uniqueEmail("kan52-admin");
        register(email, DEFAULT_PASSWORD, RoleType.INVESTOR)
                .andExpect(status().isCreated());
        Long accountId = jdbcTemplate.queryForObject(
                "select account_id from credential where email = ?", Long.class, email);
        int updated = jdbcTemplate.update(
                "update role set role_type = 'ADMIN' where account_id = ?", accountId);
        assertThat(updated).isEqualTo(1);
        return new AdminContext(login(email, DEFAULT_PASSWORD), accountId);
    }

    private long insertAuditRecord(Long actorAccountId,
                                   String sourceModule,
                                   String action,
                                   String objectType,
                                   String objectId,
                                   String outcome,
                                   Instant recordedAt) {
        return jdbcTemplate.queryForObject("""
                insert into audit_record (
                    event_id, event_type, source_module, action, object_type, object_id,
                    actor_account_id, actor_role, outcome, request_id, details,
                    occurred_at, recorded_at
                ) values (?, 'Kan52TestEvent', ?, ?, ?, ?, ?, 'ADMIN', ?, ?, '{}'::jsonb, ?, ?)
                returning audit_record_id
                """, Long.class,
                UUID.randomUUID().toString(), sourceModule, action, objectType, objectId,
                actorAccountId, outcome, uniqueValue("request"),
                Timestamp.from(recordedAt.minusSeconds(1)), Timestamp.from(recordedAt));
    }

    private JsonNode search(AuthenticatedClient administrator,
                            Map<String, String> filters,
                            int page,
                            int size) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/admin/audit-records")
                .session(administrator.session())
                .cookie(administrator.xsrfCookie())
                .param("page", Integer.toString(page))
                .param("size", Integer.toString(size));
        filters.forEach(request::param);

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void assertOnlyRecord(AuthenticatedClient administrator,
                                  Map<String, String> filters,
                                  long matchingId,
                                  long nonmatchingId) throws Exception {
        JsonNode response = search(administrator, filters, 1, 100);
        assertThat(itemIds(response))
                .as("filters %s should exclude the nonmatching record", filters)
                .containsExactly(matchingId)
                .doesNotContain(nonmatchingId);
    }

    private List<Long> itemIds(JsonNode response) {
        return response.path("items").findValuesAsText("auditRecordId").stream()
                .map(Long::valueOf)
                .toList();
    }

    private String uniqueValue(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private record AdminContext(AuthenticatedClient client, Long accountId) {
    }
}

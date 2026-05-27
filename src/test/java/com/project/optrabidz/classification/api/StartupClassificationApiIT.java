package com.project.optrabidz.classification.api;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StartupClassificationApiIT extends ApiIntegrationTestSupport {

    @Test
    void startupCanAddReplaceDeleteAndListClassifications() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);
        createStartupProfileForClassification(startup);

        mockMvc.perform(post("/api/v1/startup-classifications")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(startupClassification("GEOGRAPHY", "INDIA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Startup classification added successfully"));

        mockMvc.perform(get("/api/v1/startup-classifications/me")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startupId").isNumber())
                .andExpect(jsonPath("$.data.classifications.length()").value(1))
                .andExpect(jsonPath("$.data.classifications[0].type").value("GEOGRAPHY"))
                .andExpect(jsonPath("$.data.classifications[0].value").value("INDIA"));

        mockMvc.perform(put("/api/v1/startup-classifications/me")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "classifications", List.of(
                                        startupClassification("GEOGRAPHY", "INDIA"),
                                        startupClassification("SECTOR", "FINTECH")
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Startup classifications replaced successfully"));

        mockMvc.perform(delete("/api/v1/startup-classifications/me")
                        .queryParam("classificationType", "GEOGRAPHY")
                        .queryParam("classificationValue", "INDIA")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Startup classification removed successfully"));

        mockMvc.perform(get("/api/v1/startup-classifications/me")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classifications.length()").value(1))
                .andExpect(jsonPath("$.data.classifications[0].type").value("SECTOR"))
                .andExpect(jsonPath("$.data.classifications[0].value").value("FINTECH"));
    }

    @Test
    void duplicateStartupClassificationIsRejected() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);
        createStartupProfileForClassification(startup);

        mockMvc.perform(post("/api/v1/startup-classifications")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(startupClassification("SECTOR", "SAAS"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/startup-classifications")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(startupClassification("SECTOR", "SAAS"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_OPERATION"))
                .andExpect(jsonPath("$.error.message").value("Startup classification already exists"));
    }

    @Test
    void startupClassificationMutationsRequireCsrfHeader() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);
        createStartupProfileForClassification(startup);

        mockMvc.perform(post("/api/v1/startup-classifications")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(startupClassification("GEOGRAPHY", "INDIA"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("CSRF validation failed"));
    }

    @Test
    void startupClassificationEndpointsRequireStartupRoleAndProfile() throws Exception {
        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);
        AuthenticatedClient startupWithoutProfile = registerAndLogin(RoleType.STARTUP);

        mockMvc.perform(get("/api/v1/startup-classifications/me")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHORIZATION_FAILED"));

        mockMvc.perform(post("/api/v1/startup-classifications")
                        .session(startupWithoutProfile.session())
                        .cookie(startupWithoutProfile.xsrfCookie())
                        .header("X-CSRF-TOKEN", startupWithoutProfile.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(startupClassification("SECTOR", "SAAS"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Startup actor not found for account"));
    }

    private void createStartupProfileForClassification(AuthenticatedClient startup) throws Exception {
        mockMvc.perform(post("/api/v1/startups")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "legalEntityName", "Classification Startup Private Limited",
                                "incorporationCountryCode", "IN",
                                "publicDisplayName", "Classification Startup",
                                "businessDescription", "Startup profile used by classification integration tests.",
                                "webPresences", List.of("https://classification-startup.example.com"),
                                "legalRegistrations", List.of(Map.of(
                                        "type", "CIN",
                                        "value", "U12345KA2026PTC000001"
                                ))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private Map<String, String> startupClassification(String type, String value) {
        return Map.of(
                "classificationType", type,
                "classificationValue", value
        );
    }
}

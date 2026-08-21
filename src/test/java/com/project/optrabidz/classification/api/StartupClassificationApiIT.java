package com.project.optrabidz.classification.api;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("STARTUP_CLASSIFICATION_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.detail").value("The startup classification already exists"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(content().string(not(containsString("SAAS"))));
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
                .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("Request security validation failed"));
    }

    @Test
    void startupClassificationEndpointsRequireStartupRoleAndProfile() throws Exception {
        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);
        AuthenticatedClient startupWithoutProfile = registerAndLogin(RoleType.STARTUP);

        mockMvc.perform(get("/api/v1/startup-classifications/me")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("You are not authorized to perform this action"));

        mockMvc.perform(post("/api/v1/startup-classifications")
                        .session(startupWithoutProfile.session())
                        .cookie(startupWithoutProfile.xsrfCookie())
                        .header("X-CSRF-TOKEN", startupWithoutProfile.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(startupClassification("SECTOR", "SAAS"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("STARTUP_CLASSIFICATION_PROFILE_REQUIRED"))
                .andExpect(jsonPath("$.detail").value(
                        "Create a startup profile before managing classifications"
                ));
    }

    @Test
    void missingStartupClassificationUsesNotFoundProblem() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);
        createStartupProfileForClassification(startup);

        mockMvc.perform(delete("/api/v1/startup-classifications/me")
                        .queryParam("classificationType", "SECTOR")
                        .queryParam("classificationValue", "MISSING-STARTUP")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("STARTUP_CLASSIFICATION_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value(
                        "The requested startup classification was not found"
                ))
                .andExpect(content().string(not(containsString("MISSING-STARTUP"))));
    }

    @Test
    void blankStartupClassificationFieldRemainsAValidationProblem() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);
        createStartupProfileForClassification(startup);

        mockMvc.perform(post("/api/v1/startup-classifications")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(startupClassification("SECTOR", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations").isNotEmpty());
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

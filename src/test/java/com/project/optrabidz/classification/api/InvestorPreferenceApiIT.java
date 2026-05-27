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

class InvestorPreferenceApiIT extends ApiIntegrationTestSupport {

    @Test
    void investorCanAddReplaceDeleteAndListPreferences() throws Exception {
        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);
        createInvestorProfileForClassification(investor);

        mockMvc.perform(post("/api/v1/investor-preferences")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(investorPreference("GEOGRAPHY", "INDIA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Investor preference added successfully"));

        mockMvc.perform(get("/api/v1/investor-preferences/me")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.investorId").isNumber())
                .andExpect(jsonPath("$.data.preferences.length()").value(1))
                .andExpect(jsonPath("$.data.preferences[0].type").value("GEOGRAPHY"))
                .andExpect(jsonPath("$.data.preferences[0].value").value("INDIA"));

        mockMvc.perform(put("/api/v1/investor-preferences/me")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "preferences", List.of(
                                        investorPreference("GEOGRAPHY", "INDIA"),
                                        investorPreference("SECTOR", "FINTECH")
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Investor preferences replaced successfully"));

        mockMvc.perform(delete("/api/v1/investor-preferences/me")
                        .queryParam("preferenceType", "GEOGRAPHY")
                        .queryParam("preferenceValue", "INDIA")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Investor preference removed successfully"));

        mockMvc.perform(get("/api/v1/investor-preferences/me")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferences.length()").value(1))
                .andExpect(jsonPath("$.data.preferences[0].type").value("SECTOR"))
                .andExpect(jsonPath("$.data.preferences[0].value").value("FINTECH"));
    }

    @Test
    void duplicateInvestorPreferenceIsRejected() throws Exception {
        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);
        createInvestorProfileForClassification(investor);

        mockMvc.perform(post("/api/v1/investor-preferences")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(investorPreference("SECTOR", "SAAS"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/investor-preferences")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(investorPreference("SECTOR", "SAAS"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_OPERATION"))
                .andExpect(jsonPath("$.error.message").value("Investor preference already exists"));
    }

    @Test
    void investorPreferenceMutationsRequireCsrfHeader() throws Exception {
        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);
        createInvestorProfileForClassification(investor);

        mockMvc.perform(post("/api/v1/investor-preferences")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(investorPreference("GEOGRAPHY", "INDIA"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("CSRF validation failed"));
    }

    @Test
    void investorPreferenceEndpointsRequireInvestorRoleAndProfile() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);
        AuthenticatedClient investorWithoutProfile = registerAndLogin(RoleType.INVESTOR);

        mockMvc.perform(get("/api/v1/investor-preferences/me")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHORIZATION_FAILED"));

        mockMvc.perform(post("/api/v1/investor-preferences")
                        .session(investorWithoutProfile.session())
                        .cookie(investorWithoutProfile.xsrfCookie())
                        .header("X-CSRF-TOKEN", investorWithoutProfile.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(investorPreference("SECTOR", "SAAS"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Investor actor not found for account"));
    }

    private void createInvestorProfileForClassification(AuthenticatedClient investor) throws Exception {
        mockMvc.perform(post("/api/v1/investors")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "publicDisplayName", "Classification Investor",
                                "investorDescription", "Investor profile used by classification integration tests.",
                                "legalEntityName", "Classification Investor LLP",
                                "webPresences", List.of("https://classification-investor.example.com")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private Map<String, String> investorPreference(String type, String value) {
        return Map.of(
                "preferenceType", type,
                "preferenceValue", value
        );
    }
}

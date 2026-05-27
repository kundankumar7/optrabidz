package com.project.optrabidz.participation.api;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ParticipationApiIT extends ApiIntegrationTestSupport {

    @Test
    void startupCanCreateIncompleteProfileThenUpdateToCompleteProfile() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);

        mockMvc.perform(get("/api/v1/me")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("STARTUP"))
                .andExpect(jsonPath("$.data.profileStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.data.actorExists").value(false));

        mockMvc.perform(post("/api/v1/startups")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(incompleteStartupRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Startup created successfully"));

        mockMvc.perform(get("/api/v1/me")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.data.actorExists").value(true));

        mockMvc.perform(patch("/api/v1/startups/me")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(completeStartupRequest("Updated Startup Private Limited", "Updated Startup"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Startup updated successfully"));

        mockMvc.perform(get("/api/v1/startups/me")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.legalEntityName").value("Updated Startup Private Limited"))
                .andExpect(jsonPath("$.data.publicDisplayName").value("Updated Startup"))
                .andExpect(jsonPath("$.data.webPresences[0]").value("https://startup.example.com"))
                .andExpect(jsonPath("$.data.legalRegistrations[0].type").value("CIN"))
                .andExpect(jsonPath("$.data.legalRegistrations[0].value").value("U12345KA2026PTC000001"));

        mockMvc.perform(get("/api/v1/me")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.data.actorExists").value(true));
    }

    @Test
    void investorCanCreateAndUpdateCompleteProfile() throws Exception {
        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);

        mockMvc.perform(post("/api/v1/investors")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(investorRequest("Investor One", "Investor One Ventures LLP"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Investor created successfully"));

        mockMvc.perform(get("/api/v1/me")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("INVESTOR"))
                .andExpect(jsonPath("$.data.profileStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.data.actorExists").value(true));

        mockMvc.perform(patch("/api/v1/investors/me")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(investorRequest("Investor One Updated", "Investor One Capital LLP"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Investor updated successfully"));

        mockMvc.perform(get("/api/v1/investors/me")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicDisplayName").value("Investor One Updated"))
                .andExpect(jsonPath("$.data.legalEntityName").value("Investor One Capital LLP"))
                .andExpect(jsonPath("$.data.webPresences[0]").value("https://investor.example.com"));
    }

    @Test
    void participationEndpointsRejectWrongActorRole() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);
        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);

        mockMvc.perform(post("/api/v1/investors")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(investorRequest("Invalid Investor", "Invalid Investor LLP"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("Role is not allowed to perform this operation"));

        mockMvc.perform(post("/api/v1/startups")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(completeStartupRequest("Invalid Startup Private Limited", "Invalid Startup"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("Role is not allowed to perform this operation"));
    }

    @Test
    void participationMutationRequiresCsrfHeader() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);

        mockMvc.perform(post("/api/v1/startups")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(completeStartupRequest("CSRF Startup Private Limited", "CSRF Startup"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("CSRF validation failed"));
    }

    private Map<String, Object> incompleteStartupRequest() {
        return Map.of(
                "legalEntityName", "Incomplete Startup Private Limited",
                "incorporationCountryCode", "IN",
                "publicDisplayName", "Incomplete Startup",
                "businessDescription", "This startup intentionally omits optional profile lists.",
                "webPresences", List.of(),
                "legalRegistrations", List.of()
        );
    }

    private Map<String, Object> completeStartupRequest(String legalEntityName, String publicDisplayName) {
        return Map.of(
                "legalEntityName", legalEntityName,
                "incorporationCountryCode", "IN",
                "publicDisplayName", publicDisplayName,
                "businessDescription", "Helps startups manage fundraising workflows.",
                "webPresences", List.of("https://startup.example.com"),
                "legalRegistrations", List.of(Map.of(
                        "type", "CIN",
                        "value", "U12345KA2026PTC000001"
                ))
        );
    }

    private Map<String, Object> investorRequest(String publicDisplayName, String legalEntityName) {
        return Map.of(
                "publicDisplayName", publicDisplayName,
                "investorDescription", "Early-stage investor focused on SaaS and fintech platforms.",
                "legalEntityName", legalEntityName,
                "webPresences", List.of("https://investor.example.com")
        );
    }
}

package com.project.optrabidz.testsupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.identity.domain.model.RoleType;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public abstract class ApiIntegrationTestSupport extends PostgresIntegrationTestSupport {
    protected static final String DEFAULT_PASSWORD = "Password01";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected AuthenticatedClient registerAndLogin(RoleType roleType) throws Exception {
        String email = uniqueEmail(roleType.name().toLowerCase());
        register(email, DEFAULT_PASSWORD, roleType)
                .andExpect(status().isCreated());
        return login(email, DEFAULT_PASSWORD);
    }

    protected ResultActions register(String email, String password, RoleType roleType) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "email", email,
                        "password", password,
                        "role", roleType.name()
                ))));
    }

    protected AuthenticatedClient login(String email, String password) throws Exception {
        MvcResult loginResult = loginAttempt(email, password)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Login successful"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).as("Login must create a stateful HTTP session").isNotNull();

        Cookie xsrfCookie = loginResult.getResponse().getCookie("XSRF-TOKEN");
        if (xsrfCookie == null) {
            MvcResult csrfPrimingResult = mockMvc.perform(get("/api/v1/me").session(session))
                    .andExpect(status().isOk())
                    .andReturn();
            xsrfCookie = csrfPrimingResult.getResponse().getCookie("XSRF-TOKEN");
        }

        assertThat(xsrfCookie).as("Authenticated client must receive an XSRF-TOKEN cookie").isNotNull();
        assertThat(xsrfCookie.getValue()).as("XSRF-TOKEN cookie value").isNotBlank();

        return new AuthenticatedClient(session, xsrfCookie);
    }

    protected ResultActions loginAttempt(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "email", email,
                        "password", password
                ))));
    }

    protected void createCompleteStartupProfile(AuthenticatedClient startup) throws Exception {
        createCompleteStartupProfile(startup, "Test Startup " + UUID.randomUUID().toString().substring(0, 8));
    }

    protected void createCompleteStartupProfile(AuthenticatedClient startup, String publicDisplayName) throws Exception {
        String registrationValue = "REG-" + UUID.randomUUID();
        mockMvc.perform(post("/api/v1/startups")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "legalEntityName", publicDisplayName + " Private Limited",
                                "incorporationCountryCode", "IN",
                                "publicDisplayName", publicDisplayName,
                                "businessDescription", "Test startup profile used by API integration tests.",
                                "webPresences", List.of("https://" + publicDisplayName.toLowerCase().replace(" ", "-") + ".example.com"),
                                "legalRegistrations", List.of(Map.of(
                                        "type", "TEST_REGISTRATION",
                                        "value", registrationValue
                                ))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    protected void createCompleteInvestorProfile(AuthenticatedClient investor) throws Exception {
        createCompleteInvestorProfile(investor, "Test Investor " + UUID.randomUUID().toString().substring(0, 8));
    }

    protected void createCompleteInvestorProfile(AuthenticatedClient investor, String publicDisplayName) throws Exception {
        mockMvc.perform(post("/api/v1/investors")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "publicDisplayName", publicDisplayName,
                                "investorDescription", "Test investor profile used by API integration tests.",
                                "legalEntityName", publicDisplayName + " LLP",
                                "webPresences", List.of("https://" + publicDisplayName.toLowerCase().replace(" ", "-") + ".example.com")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    protected void addStartupClassification(AuthenticatedClient startup, String type, String value) throws Exception {
        mockMvc.perform(post("/api/v1/startup-classifications")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "classificationType", type,
                                "classificationValue", value
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    protected void addInvestorPreference(AuthenticatedClient investor, String type, String value) throws Exception {
        mockMvc.perform(post("/api/v1/investor-preferences")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "preferenceType", type,
                                "preferenceValue", value
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    protected String json(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    protected record AuthenticatedClient(MockHttpSession session, Cookie xsrfCookie) {
        public String csrfToken() {
            return xsrfCookie.getValue();
        }
    }
}

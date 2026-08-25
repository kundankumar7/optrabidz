package com.project.optrabidz.common.api.error;

import com.project.optrabidz.common.api.response.RequestMetadataFilter;
import com.project.optrabidz.common.observability.SecurityMdcFilter;
import com.project.optrabidz.security.infrastructure.config.ActiveSessionFilter;
import com.project.optrabidz.security.infrastructure.config.CsrfCookieFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = RestExceptionHandlerContextProbeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        ActiveSessionFilter.class,
                        CsrfCookieFilter.class,
                        RequestMetadataFilter.class,
                        SecurityMdcFilter.class
                }
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({ProblemDetailsFactory.class, ValidationViolationMapper.class})
class RestExceptionHandlerContextTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void discoversTheSingleMvcAdviceForUnexpectedFailures()
            throws Exception {
        mockMvc.perform(get("/test/context-unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code").value(
                        "INTERNAL_SERVER_ERROR"
                ))
                .andExpect(jsonPath("$.detail").value(
                        "An unexpected error occurred"
                ));
    }
}

@RestController
class RestExceptionHandlerContextProbeController {
    @GetMapping("/test/context-unexpected")
    void fail() {
        throw new RuntimeException("private context detail");
    }
}

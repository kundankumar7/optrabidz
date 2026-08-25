package com.project.optrabidz.common.api.error;

import com.project.optrabidz.common.api.response.RequestMetadataFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestExceptionHandlerFrameworkTest {
    private static final String REQUEST_ID = "framework-request-123";
    private static final String NOW = "2026-08-15T04:00:00Z";
    private static final String MALFORMED_SENTINEL = "private-token-value";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProblemDetailsFactory factory = new ProblemDetailsFactory(
                Clock.fixed(Instant.parse(NOW), ZoneOffset.UTC)
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FrameworkProbeController())
                .setControllerAdvice(
                        new RestExceptionHandler(
                                factory,
                                new ValidationViolationMapper()
                        )
                )
                .addFilters(new RequestMetadataFilter())
                .build();
    }

    @Test
    void rendersMalformedJsonWithoutParserOrSubmittedContent()
            throws Exception {
        ResultActions result = mockMvc.perform(post("/test/body")
                .header("X-Request-Id", REQUEST_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"" + MALFORMED_SENTINEL + "\""));

        expectFrameworkProblem(
                result,
                400,
                "malformed-request",
                "Malformed request",
                "The request body is malformed",
                "MALFORMED_REQUEST"
        )
                .andExpect(content().string(not(containsString(
                        MALFORMED_SENTINEL
                ))))
                .andExpect(content().string(not(containsString(
                        "HttpMessageNotReadableException"
                ))));
    }

    @Test
    void rendersUnknownEndpoint() throws Exception {
        expectFrameworkProblem(
                mockMvc.perform(get("/test/does-not-exist")
                        .header("X-Request-Id", REQUEST_ID)),
                404,
                "endpoint-not-found",
                "Endpoint not found",
                "The requested endpoint is unavailable",
                "ENDPOINT_NOT_FOUND"
        );
    }

    @Test
    void rendersUnsupportedMethodAndPreservesAllowHeader() throws Exception {
        expectFrameworkProblem(
                mockMvc.perform(post("/test/json")
                        .header("X-Request-Id", REQUEST_ID)),
                405,
                "method-not-allowed",
                "Method not allowed",
                "The HTTP method is not supported for this endpoint",
                "METHOD_NOT_ALLOWED"
        ).andExpect(header().string(
                HttpHeaders.ALLOW,
                containsString("GET")
        ));
    }

    @Test
    void rendersUnacceptableResponseMediaType() throws Exception {
        expectFrameworkProblem(
                mockMvc.perform(get("/test/json")
                        .header("X-Request-Id", REQUEST_ID)
                        .accept(MediaType.APPLICATION_XML)),
                406,
                "not-acceptable",
                "Response type not acceptable",
                "The requested response media type is not available",
                "NOT_ACCEPTABLE"
        );
    }

    @Test
    void rendersUnsupportedRequestMediaTypeAndPreservesAcceptHeader()
            throws Exception {
        expectFrameworkProblem(
                mockMvc.perform(post("/test/body")
                        .header("X-Request-Id", REQUEST_ID)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("private request content")),
                415,
                "unsupported-media-type",
                "Unsupported media type",
                "The request media type is not supported",
                "UNSUPPORTED_MEDIA_TYPE"
        )
                .andExpect(header().string(
                        HttpHeaders.ACCEPT,
                        containsString(MediaType.APPLICATION_JSON_VALUE)
                ))
                .andExpect(content().string(not(containsString(
                        "private request content"
                ))));
    }

    @Test
    void leavesValidEndpointResponsesUnchanged() throws Exception {
        mockMvc.perform(get("/test/json")
                        .header("X-Request-Id", REQUEST_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.result").value("ok"))
                .andExpect(header().string("X-Request-Id", REQUEST_ID));

        mockMvc.perform(post("/test/body")
                        .header("X-Request-Id", REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"safe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("safe"));
    }

    private ResultActions expectFrameworkProblem(
            ResultActions result,
            int expectedStatus,
            String typeSlug,
            String title,
            String detail,
            String code
    ) throws Exception {
        return result
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(header().string("X-Request-Id", REQUEST_ID))
                .andExpect(jsonPath("$.type").value(
                        "urn:optrabidz:problem:" + typeSlug
                ))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value(
                        "urn:optrabidz:request:" + REQUEST_ID
                ))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.timestamp").value(NOW))
                .andExpect(jsonPath("$.violations").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @RestController
    static final class FrameworkProbeController {
        @GetMapping(
                path = "/test/json",
                produces = MediaType.APPLICATION_JSON_VALUE
        )
        Map<String, String> json() {
            return Map.of("result", "ok");
        }

        @PostMapping(
                path = "/test/body",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE
        )
        FrameworkRequest body(@RequestBody FrameworkRequest request) {
            return request;
        }
    }

    private record FrameworkRequest(String value) {
    }
}

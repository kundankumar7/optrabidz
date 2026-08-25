package com.project.optrabidz.common.api.error;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.api.response.RequestMetadataFilter;
import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestExceptionHandlerTest {
    private static final String PUBLIC_MESSAGE =
            "The requested funding listing is unavailable";
    private static final String INTERNAL_MESSAGE =
            "password=hunter2 table=credential row=42";

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ProblemDetailsFactory factory = new ProblemDetailsFactory(
                Clock.fixed(
                        Instant.parse("2026-08-15T04:00:00Z"),
                        ZoneOffset.UTC
                )
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FailureProbeController())
                .setControllerAdvice(
                        new RestExceptionHandler(
                                factory,
                                new ValidationViolationMapper()
                        )
                )
                .addFilters(new RequestMetadataFilter())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void rendersApplicationExceptionAsProblemDetails() throws Exception {
        mockMvc.perform(get("/test/application-error")
                        .header("X-Request-Id", "request-123"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(header().string("X-Request-Id", "request-123"))
                .andExpect(jsonPath("$.type").value(
                        "urn:optrabidz:problem:listing-not-found"
                ))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(PUBLIC_MESSAGE))
                .andExpect(jsonPath("$.instance").value(
                        "urn:optrabidz:request:request-123"
                ))
                .andExpect(jsonPath("$.code").value("LISTING_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").value("request-123"))
                .andExpect(jsonPath("$.timestamp").value(
                        "2026-08-15T04:00:00Z"
                ))
                .andExpect(jsonPath("$.diagnosticCode").doesNotExist())
                .andExpect(jsonPath("$.violations").doesNotExist())
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString(
                                        INTERNAL_MESSAGE
                                )
                        )
                ));
    }

    @Test
    void replacesInvalidRequestIdAndKeepsHeaderAndBodyEqual()
            throws Exception {
        MvcResult result = mockMvc.perform(get("/test/application-error")
                        .header("X-Request-Id", "invalid request id!"))
                .andExpect(status().isNotFound())
                .andReturn();

        String headerRequestId = result.getResponse()
                .getHeader("X-Request-Id");
        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertThat(headerRequestId).isNotBlank();
        assertThatCode(() -> UUID.fromString(headerRequestId))
                .doesNotThrowAnyException();
        assertThat(body.path("requestId").asText())
                .isEqualTo(headerRequestId);
        assertThat(body.path("instance").asText())
                .isEqualTo("urn:optrabidz:request:" + headerRequestId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"argument", "state", "null", "runtime"})
    void sanitizesEveryUnexpectedRuntimeFailure(String failure)
            throws Exception {
        String requestId = "unexpected-" + failure;

        mockMvc.perform(get("/test/unexpected/{failure}", failure)
                        .header("X-Request-Id", requestId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(header().string("X-Request-Id", requestId))
                .andExpect(jsonPath("$.type").value(
                        "urn:optrabidz:problem:internal-server-error"
                ))
                .andExpect(jsonPath("$.title").value(
                        "Internal server error"
                ))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value(
                        "An unexpected error occurred"
                ))
                .andExpect(jsonPath("$.code").value(
                        "INTERNAL_SERVER_ERROR"
                ))
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.instance").value(
                        "urn:optrabidz:request:" + requestId
                ))
                .andExpect(jsonPath("$.timestamp").value(
                        "2026-08-15T04:00:00Z"
                ))
                .andExpect(jsonPath("$.violations").doesNotExist())
                .andExpect(content().string(not(containsString(
                        "password=hunter2"
                ))))
                .andExpect(content().string(not(containsString(
                        "jdbc:postgresql://private-host"
                ))))
                .andExpect(content().string(not(containsString(
                        "credential was null"
                ))))
                .andExpect(content().string(not(containsString(
                        "provider-secret-value"
                ))))
                .andExpect(content().string(not(containsString(
                        "IllegalArgumentException"
                ))))
                .andExpect(content().string(not(containsString(
                        "IllegalStateException"
                ))))
                .andExpect(content().string(not(containsString(
                        "NullPointerException"
                ))));
    }

    @RestController
    static final class FailureProbeController {
        private static final ErrorDescriptor LISTING_NOT_FOUND =
                new ErrorDescriptor(
                        "LISTING_NOT_FOUND",
                        ErrorCategory.NOT_FOUND,
                        PUBLIC_MESSAGE
                );

        @GetMapping("/test/application-error")
        void applicationError() {
            throw new ApplicationException(
                    LISTING_NOT_FOUND,
                    "MARKETPLACE.LISTING_LOOKUP_FAILED",
                    INTERNAL_MESSAGE,
                    new IllegalStateException(
                            "jdbc:postgresql://internal-host"
                    )
            );
        }

        @GetMapping("/test/unexpected/{failure}")
        void unexpected(@PathVariable String failure) {
            RuntimeException exception = switch (failure) {
                case "argument" -> new IllegalArgumentException(
                        "password=hunter2"
                );
                case "state" -> new IllegalStateException(
                        "jdbc:postgresql://private-host"
                );
                case "null" -> new NullPointerException(
                        "credential was null"
                );
                case "runtime" -> new RuntimeException(
                        "provider-secret-value"
                );
                default -> new IllegalArgumentException(
                        "unknown test failure"
                );
            };
            throw exception;
        }
    }
}

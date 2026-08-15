package com.project.optrabidz.common.api.error;

import com.project.optrabidz.common.api.exception.GlobalExceptionHandler;
import com.project.optrabidz.common.api.response.RequestMetadataFilter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestExceptionHandlerValidationTest {
    private static final String REQUEST_ID = "validation-request-123";
    private static final String NOW = "2026-08-15T04:00:00Z";
    private static final String REJECTED_EMAIL = "private-email-value";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProblemDetailsFactory factory = new ProblemDetailsFactory(
                Clock.fixed(Instant.parse(NOW), ZoneOffset.UTC)
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ValidationProbeController())
                .setControllerAdvice(
                        new GlobalExceptionHandler(),
                        new RestExceptionHandler(
                                factory,
                                new ValidationViolationMapper()
                        )
                )
                .addFilters(new RequestMetadataFilter())
                .build();
    }

    @Test
    void rendersDtoNestedAndCollectionViolationsWithoutRejectedValues()
            throws Exception {
        String body = """
                {
                  "name": " ",
                  "email": "private-email-value",
                  "nested": {"amount": 0},
                  "items": ["valid", " "]
                }
                """;

        ResultActions result = mockMvc.perform(post("/test/validation")
                .header("X-Request-Id", REQUEST_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        expectValidationProblem(result)
                .andExpect(jsonPath("$.violations.length()").value(4))
                .andExpect(jsonPath("$.violations[0].field").value("email"))
                .andExpect(jsonPath("$.violations[0].message").value(
                        "must be a well-formed email address"
                ))
                .andExpect(jsonPath("$.violations[1].field").value(
                        "items[1]"
                ))
                .andExpect(jsonPath("$.violations[1].message").value(
                        "must not be blank"
                ))
                .andExpect(jsonPath("$.violations[2].field").value("name"))
                .andExpect(jsonPath("$.violations[2].message").value(
                        "must not be blank"
                ))
                .andExpect(jsonPath("$.violations[3].field").value(
                        "nested.amount"
                ))
                .andExpect(jsonPath("$.violations[3].message").value(
                        "must be greater than zero"
                ))
                .andExpect(content().string(not(containsString(
                        REJECTED_EMAIL
                ))));
    }

    @Test
    void rendersMissingParameterAsValidationProblem() throws Exception {
        expectValidationProblem(mockMvc.perform(
                get("/test/required-parameter")
                        .header("X-Request-Id", REQUEST_ID)
        ))
                .andExpect(jsonPath("$.violations[0].field").value(
                        "accountId"
                ))
                .andExpect(jsonPath("$.violations[0].message").value(
                        "is required"
                ));
    }

    @Test
    void rendersMissingHeaderAsValidationProblem() throws Exception {
        expectValidationProblem(mockMvc.perform(
                get("/test/required-header")
                        .header("X-Request-Id", REQUEST_ID)
        ))
                .andExpect(jsonPath("$.violations[0].field").value(
                        "X-Required"
                ))
                .andExpect(jsonPath("$.violations[0].message").value(
                        "is required"
                ));
    }

    @Test
    void rendersTypeMismatchAsValidationProblem() throws Exception {
        expectValidationProblem(mockMvc.perform(
                get("/test/type-mismatch")
                        .header("X-Request-Id", REQUEST_ID)
                        .param("count", "not-a-number")
        ))
                .andExpect(jsonPath("$.violations[0].field").value("count"))
                .andExpect(jsonPath("$.violations[0].message").value(
                        "has an invalid type"
                ))
                .andExpect(content().string(not(containsString(
                        "not-a-number"
                ))));
    }

    @Test
    void rendersHandlerMethodConstraintAsValidationProblem()
            throws Exception {
        expectValidationProblem(mockMvc.perform(
                get("/test/method-validation")
                        .header("X-Request-Id", REQUEST_ID)
                        .param("amount", "0")
        ))
                .andExpect(jsonPath("$.violations[0].field").value("amount"))
                .andExpect(jsonPath("$.violations[0].message").value(
                        "must be greater than zero"
                ));
    }

    @Test
    void rendersConstraintViolationAsValidationProblem() throws Exception {
        expectValidationProblem(mockMvc.perform(
                get("/test/constraint-violation")
                        .header("X-Request-Id", REQUEST_ID)
        ))
                .andExpect(jsonPath("$.violations[0].field").value("amount"))
                .andExpect(jsonPath("$.violations[0].message").value(
                        "must be greater than zero"
                ));
    }

    private ResultActions expectValidationProblem(ResultActions result)
            throws Exception {
        return result
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(header().string("X-Request-Id", REQUEST_ID))
                .andExpect(jsonPath("$.type").value(
                        "urn:optrabidz:problem:validation-error"
                ))
                .andExpect(jsonPath("$.title").value(
                        "Request validation failed"
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(
                        "One or more request values are invalid"
                ))
                .andExpect(jsonPath("$.instance").value(
                        "urn:optrabidz:request:" + REQUEST_ID
                ))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.timestamp").value(NOW))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @RestController
    static final class ValidationProbeController {
        private final Validator validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();

        @PostMapping(
                path = "/test/validation",
                consumes = MediaType.APPLICATION_JSON_VALUE
        )
        void body(@Valid @RequestBody ValidationProbe request) {
        }

        @GetMapping("/test/required-parameter")
        void requiredParameter(
                @RequestParam("accountId") String accountId
        ) {
        }

        @GetMapping("/test/required-header")
        void requiredHeader(
                @RequestHeader("X-Required") String value
        ) {
        }

        @GetMapping("/test/type-mismatch")
        void typeMismatch(@RequestParam("count") Long count) {
        }

        @GetMapping("/test/method-validation")
        void methodValidation(
                @RequestParam("amount") @Positive Integer amount
        ) {
        }

        @GetMapping("/test/constraint-violation")
        void constraintViolation() {
            Set<ConstraintViolation<ConstraintProbe>> violations =
                    validator.validate(new ConstraintProbe(0));
            throw new ConstraintViolationException(violations);
        }
    }

    private record ValidationProbe(
            @NotBlank String name,
            @Email String email,
            @Valid @NotNull NestedProbe nested,
            List<@NotBlank String> items
    ) {
    }

    private record NestedProbe(@Positive Integer amount) {
    }

    private record ConstraintProbe(@Positive Integer amount) {
    }
}

package com.project.optrabidz.common.api.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ValidationViolationMapperTest {
    private final ValidationViolationMapper mapper =
            new ValidationViolationMapper();

    @Test
    void mapsKnownAndUnknownFieldErrorsWithoutRejectedValues() {
        BeanPropertyBindingResult result =
                new BeanPropertyBindingResult(new Object(), "request");
        result.addError(new FieldError(
                "request",
                "email",
                "secret@example.test",
                false,
                new String[]{"Email.request.email", "Email"},
                null,
                "secret@example.test is not valid"
        ));
        result.addError(new FieldError(
                "request",
                "custom",
                "token-123",
                false,
                new String[]{"UnknownConstraint"},
                null,
                "token-123 failed internal rule"
        ));
        result.addError(new FieldError(
                "request",
                "email",
                "secret@example.test",
                false,
                new String[]{"Email"},
                null,
                "duplicate secret@example.test"
        ));

        List<ValidationViolation> violations =
                mapper.fromBindingResult(result);

        assertThat(violations).containsExactly(
                new ValidationViolation("custom", "is invalid"),
                new ValidationViolation(
                        "email",
                        "must be a well-formed email address"
                )
        );
        assertThat(violations.toString())
                .doesNotContain("secret@example.test")
                .doesNotContain("token-123")
                .doesNotContain("internal rule");
    }

    @Test
    void mapsRepaymentFilterSelectionToItsSafePublicMessage() {
        BeanPropertyBindingResult result =
                new BeanPropertyBindingResult(new Object(), "query");
        result.addError(new ObjectError(
                "query",
                new String[]{"ValidRepaymentInstallmentFilterSelection.query"},
                null,
                "internal validation detail"
        ));

        assertThat(mapper.fromBindingResult(result)).containsExactly(
                new ValidationViolation(
                        "_request",
                        "Use either installmentState or paymentView, not both"
                )
        );
    }

    @Test
    void mapsBeanConstraintPathsWithoutMethodOrRejectedValue() {
        Validator validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();
        ConstraintProbe probe = new ConstraintProbe(
                " ",
                0,
                List.of("valid", " ")
        );
        Set<ConstraintViolation<ConstraintProbe>> source =
                validator.validate(probe);

        List<ValidationViolation> violations =
                mapper.fromConstraintViolations(source);

        assertThat(violations).containsExactly(
                new ValidationViolation(
                        "amount",
                        "must be greater than zero"
                ),
                new ValidationViolation(
                        "items[1]",
                        "must not be blank"
                ),
                new ValidationViolation("name", "must not be blank")
        );
        assertThat(violations.toString())
                .doesNotContain("ConstraintProbe")
                .doesNotContain("valid");
    }

    @Test
    void mapsMethodValidationUsingThePublicParameterName() throws Exception {
        Method method = MethodProbe.class.getDeclaredMethod(
                "check",
                Integer.class
        );
        MethodParameter parameter = new MethodParameter(method, 0);
        ParameterValidationResult parameterResult =
                new ParameterValidationResult(
                        parameter,
                        -42,
                        List.of(new DefaultMessageSourceResolvable(
                                new String[]{"Positive.methodProbe.amount",
                                        "Positive"},
                                null,
                                "-42 must be positive"
                        )),
                        null,
                        null,
                        null
                );
        MethodValidationResult methodResult = MethodValidationResult.create(
                new MethodProbe(),
                method,
                List.of(parameterResult)
        );

        List<ValidationViolation> violations = mapper.fromMethodValidation(
                new HandlerMethodValidationException(methodResult)
        );

        assertThat(violations).containsExactly(new ValidationViolation(
                "amount",
                "must be greater than zero"
        ));
        assertThat(violations.toString())
                .doesNotContain("-42")
                .doesNotContain("MethodProbe")
                .doesNotContain("check");
    }

    @Test
    void createsFixedMissingAndTypeMismatchViolations() {
        assertThat(mapper.missing(" accountId ")).isEqualTo(
                new ValidationViolation("accountId", "is required")
        );
        assertThat(mapper.typeMismatch("count")).isEqualTo(
                new ValidationViolation("count", "has an invalid type")
        );
        assertThat(mapper.missing(" ")).isEqualTo(
                new ValidationViolation("_request", "is required")
        );
    }

    @Test
    void rejectsMissingMapperInputs() {
        assertThatNullPointerException()
                .isThrownBy(() -> mapper.fromBindingResult(null));
        assertThatNullPointerException()
                .isThrownBy(() -> mapper.fromConstraintViolations(null));
        assertThatNullPointerException()
                .isThrownBy(() -> mapper.fromMethodValidation(null));
    }

    private record ConstraintProbe(
            @NotBlank String name,
            @Positive Integer amount,
            List<@NotBlank String> items
    ) {
    }

    static final class MethodProbe {
        void check(@RequestParam("amount") @Positive Integer amount) {
        }
    }
}

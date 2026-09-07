package com.project.optrabidz.governance.api;

import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.governance.application.admin.AdminAuthorityTransferService;
import com.project.optrabidz.governance.application.admin.AdminRecoveryProperties;
import com.project.optrabidz.governance.application.admin.AdminTransferResponse;
import com.project.optrabidz.governance.application.admin.TransferAdminAuthorityCommand;
import com.project.optrabidz.governance.application.admin.TransferAdminAuthorityRequest;
import com.project.optrabidz.governance.application.admin.exception.AdminRecoveryAccessDeniedException;
import com.project.optrabidz.governance.application.error.GovernanceErrors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRecoveryControllerTest {

    private static final String CONFIGURED_TOKEN = "configured-test-recovery-token";
    private static final String SUBMITTED_TOKEN = "submitted-test-recovery-token";

    @Mock
    private AdminAuthorityTransferService transferService;

    private AdminRecoveryProperties properties;
    private AdminRecoveryController controller;

    @BeforeEach
    void setUp() {
        properties = new AdminRecoveryProperties();
        controller = new AdminRecoveryController(transferService, properties);
    }

    @Test
    void disabledRecoveryModeUsesDisclosureSafeDenial() {
        properties.setEnabled(false);
        properties.setToken(CONFIGURED_TOKEN);

        assertDenied(CONFIGURED_TOKEN, "GOVERNANCE.RECOVERY.MODE_DISABLED");
    }

    @Test
    void nullRecoveryTokenConfigurationUsesDisclosureSafeDenial() {
        properties.setEnabled(true);
        properties.setToken(null);

        assertDenied(SUBMITTED_TOKEN, "GOVERNANCE.RECOVERY.TOKEN_NOT_CONFIGURED");
    }

    @Test
    void blankRecoveryTokenConfigurationUsesDisclosureSafeDenial() {
        properties.setEnabled(true);
        properties.setToken("   ");

        assertDenied(SUBMITTED_TOKEN, "GOVERNANCE.RECOVERY.TOKEN_NOT_CONFIGURED");
    }

    @Test
    void missingSubmittedTokenUsesDisclosureSafeDenial() {
        configureRecovery();

        assertDenied(null, "GOVERNANCE.RECOVERY.TOKEN_MISSING");
    }

    @Test
    void blankSubmittedTokenUsesDisclosureSafeDenial() {
        configureRecovery();

        assertDenied("   ", "GOVERNANCE.RECOVERY.TOKEN_MISSING");
    }

    @Test
    void mismatchedSubmittedTokenUsesDisclosureSafeDenial() {
        configureRecovery();

        assertDenied(SUBMITTED_TOKEN, "GOVERNANCE.RECOVERY.TOKEN_REJECTED");
    }

    @Test
    void matchingTokenDelegatesTheExistingTransferCommandAndSuccessResponse() {
        configureRecovery();
        when(transferService.transferAuthority(any(TransferAdminAuthorityCommand.class), eq(true)))
                .thenReturn(88L);

        SuccessResponse<AdminTransferResponse> response = controller.transferAdminAuthority(
                CONFIGURED_TOKEN,
                request(),
                new MockHttpServletRequest()
        );

        assertThat(response.success()).isTrue();
        assertThat(response.data().newAdminAccountId()).isEqualTo(88L);
        assertThat(response.data().message())
                .isEqualTo("Admin authority transferred successfully");
        ArgumentCaptor<TransferAdminAuthorityCommand> command =
                ArgumentCaptor.forClass(TransferAdminAuthorityCommand.class);
        verify(transferService).transferAuthority(command.capture(), eq(true));
        assertThat(command.getValue()).isEqualTo(request().toCommand());
    }

    private void configureRecovery() {
        properties.setEnabled(true);
        properties.setToken(CONFIGURED_TOKEN);
    }

    private void assertDenied(String submittedToken, String diagnosticCode) {
        Throwable thrown = catchThrowable(() -> controller.transferAdminAuthority(
                submittedToken,
                request(),
                new MockHttpServletRequest()
        ));

        assertThat(thrown).isInstanceOf(AdminRecoveryAccessDeniedException.class);
        ApplicationException failure = (ApplicationException) thrown;
        assertThat(failure.descriptor())
                .isSameAs(GovernanceErrors.ADMIN_RECOVERY_ACCESS_DENIED);
        assertThat(failure.diagnosticCode()).isEqualTo(diagnosticCode);
        assertThat(failure.descriptor().publicMessage())
                .doesNotContain(CONFIGURED_TOKEN, SUBMITTED_TOKEN);
        assertThat(failure.getMessage())
                .doesNotContain(CONFIGURED_TOKEN, SUBMITTED_TOKEN);
        verifyNoInteractions(transferService);
    }

    private static TransferAdminAuthorityRequest request() {
        return new TransferAdminAuthorityRequest(
                "replacement-admin@example.test",
                "ReplacementPassword01",
                "Replacement Admin",
                "OptraBidz Test",
                "Controlled authority transfer",
                47L
        );
    }
}

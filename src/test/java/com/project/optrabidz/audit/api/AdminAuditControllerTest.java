package com.project.optrabidz.audit.api;

import com.project.optrabidz.audit.application.AuditService;
import com.project.optrabidz.audit.application.dto.response.AuditRecordResponse;
import com.project.optrabidz.common.api.pagination.PageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditControllerTest {
    @Mock
    private AuditService auditService;

    @Test
    void searchReturnsThePageContractDirectly() {
        AdminAuditController controller = new AdminAuditController(auditService);
        Instant from = Instant.parse("2000-01-01T00:00:00Z");
        Instant to = Instant.parse("2100-01-01T00:00:00Z");
        PageResponse<AuditRecordResponse> expected = new PageResponse<>(List.of(), 1, 20, 0, 0);
        when(auditService.search(1L, "SECURITY", "LOGIN_FAILED", "ACCOUNT", "1", "FAILED",
                from, to, 1, 20)).thenReturn(expected);

        PageResponse<AuditRecordResponse> response = controller.searchAuditRecords(
                1L, "SECURITY", "LOGIN_FAILED", "ACCOUNT", "1", "FAILED", from, to, 1, 20);

        assertThat(response).isSameAs(expected);
    }

    @Test
    void searchMethodDeclaresTheDirectPageResponseType() throws NoSuchMethodException {
        Method method = AdminAuditController.class.getDeclaredMethod(
                "searchAuditRecords",
                Long.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                Instant.class,
                Instant.class,
                int.class,
                int.class);

        assertThat(method.getReturnType()).isEqualTo(PageResponse.class);
    }
}

package com.project.optrabidz.audit.api;

import com.project.optrabidz.audit.application.AuditService;
import com.project.optrabidz.audit.application.dto.response.AuditRecordResponse;
import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/audit-records")
public class AdminAuditController {
    private final AuditService auditService;

    public AdminAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public SuccessResponse<PageResponse<AuditRecordResponse>> searchAuditRecords(
            @RequestParam(required = false) Long actorAccountId,
            @RequestParam(required = false) String sourceModule,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String objectId,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(
                auditService.search(actorAccountId, sourceModule, action, objectType, objectId, outcome, from, to, page, size),
                httpRequest
        );
    }
}

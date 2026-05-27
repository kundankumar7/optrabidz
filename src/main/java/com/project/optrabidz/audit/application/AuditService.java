package com.project.optrabidz.audit.application;

import com.project.optrabidz.audit.application.dto.response.AuditRecordResponse;
import com.project.optrabidz.audit.application.policy.AuditDescriptor;
import com.project.optrabidz.audit.application.policy.AuditPolicyRegistry;
import com.project.optrabidz.audit.infrastructure.entity.AuditRecord;
import com.project.optrabidz.audit.infrastructure.repository.JpaAuditRecordRepository;
import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuditService {
    private final JpaAuditRecordRepository auditRecordRepository;
    private final AuditRecordFactory auditRecordFactory;
    private final AuditPolicyRegistry auditPolicyRegistry;

    public AuditService(JpaAuditRecordRepository auditRecordRepository,
                        AuditRecordFactory auditRecordFactory,
                        AuditPolicyRegistry auditPolicyRegistry) {
        this.auditRecordRepository = auditRecordRepository;
        this.auditRecordFactory = auditRecordFactory;
        this.auditPolicyRegistry = auditPolicyRegistry;
    }

    @Transactional
    public void recordOutboxSuccess(OutboxEvent event) {
        AuditDescriptor descriptor = auditPolicyRegistry.describe(event);
        String action = auditRecordFactory.normalizeActionValue(descriptor.action());
        if (auditRecordRepository.existsByEventIdAndAction(event.getEventId(), action)) {
            return;
        }

        auditRecordRepository.save(auditRecordFactory.fromOutboxSuccess(event, descriptor));
    }

    @Transactional
    public void save(AuditRecord auditRecord) {
        auditRecordRepository.save(auditRecord);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditRecordResponse> search(Long actorAccountId,
                                                    String sourceModule,
                                                    String action,
                                                    String objectType,
                                                    String objectId,
                                                    String outcome,
                                                    Instant from,
                                                    Instant to,
                                                    int page,
                                                    int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);
        Page<AuditRecord> records = auditRecordRepository.search(
                actorAccountId,
                blankToNull(sourceModule),
                blankToNull(action),
                blankToNull(objectType),
                blankToNull(objectId),
                blankToNull(outcome),
                from,
                to,
                pageable
        );

        return new PageResponse<>(
                records.map(this::toResponse).getContent(),
                safePage,
                safeSize,
                records.getTotalElements(),
                records.getTotalPages()
        );
    }

    private AuditRecordResponse toResponse(AuditRecord auditRecord) {
        return new AuditRecordResponse(
                auditRecord.getAuditRecordId(),
                auditRecord.getEventId(),
                auditRecord.getEventType(),
                auditRecord.getSourceModule(),
                auditRecord.getAction(),
                auditRecord.getObjectType(),
                auditRecord.getObjectId(),
                auditRecord.getActorAccountId(),
                auditRecord.getActorRole(),
                auditRecord.getOutcome(),
                auditRecord.getRequestId(),
                auditRecord.getIpAddress(),
                auditRecord.getUserAgent(),
                auditRecord.getDetails(),
                auditRecord.getOccurredAt(),
                auditRecord.getRecordedAt()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

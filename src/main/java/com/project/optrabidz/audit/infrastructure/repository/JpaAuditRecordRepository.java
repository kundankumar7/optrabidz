package com.project.optrabidz.audit.infrastructure.repository;

import com.project.optrabidz.audit.infrastructure.entity.AuditRecord;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAuditRecordRepository extends JpaRepository<AuditRecord, Long>,
        JpaSpecificationExecutor<AuditRecord> {
    boolean existsByEventIdAndAction(String eventId, String action);
}

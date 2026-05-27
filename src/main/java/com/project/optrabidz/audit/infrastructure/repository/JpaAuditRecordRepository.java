package com.project.optrabidz.audit.infrastructure.repository;

import com.project.optrabidz.audit.infrastructure.entity.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface JpaAuditRecordRepository extends JpaRepository<AuditRecord, Long> {
    boolean existsByEventIdAndAction(String eventId, String action);

    @Query("""
            select a
            from AuditRecord a
            where (:actorAccountId is null or a.actorAccountId = :actorAccountId)
              and (:sourceModule is null or a.sourceModule = :sourceModule)
              and (:action is null or a.action = :action)
              and (:objectType is null or a.objectType = :objectType)
              and (:objectId is null or a.objectId = :objectId)
              and (:outcome is null or a.outcome = :outcome)
              and (:from is null or a.recordedAt >= :from)
              and (:to is null or a.recordedAt <= :to)
            order by a.recordedAt desc, a.auditRecordId desc
            """)
    Page<AuditRecord> search(@Param("actorAccountId") Long actorAccountId,
                             @Param("sourceModule") String sourceModule,
                             @Param("action") String action,
                             @Param("objectType") String objectType,
                             @Param("objectId") String objectId,
                             @Param("outcome") String outcome,
                             @Param("from") Instant from,
                             @Param("to") Instant to,
                             Pageable pageable);
}

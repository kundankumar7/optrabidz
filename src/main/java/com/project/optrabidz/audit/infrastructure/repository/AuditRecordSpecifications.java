package com.project.optrabidz.audit.infrastructure.repository;

import com.project.optrabidz.audit.infrastructure.entity.AuditRecord;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AuditRecordSpecifications {
    private AuditRecordSpecifications() {
    }

    public static Specification<AuditRecord> matching(Long actorAccountId,
                                                      String sourceModule,
                                                      String action,
                                                      String objectType,
                                                      String objectId,
                                                      String outcome,
                                                      Instant from,
                                                      Instant to) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (actorAccountId != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorAccountId"), actorAccountId));
            }
            if (sourceModule != null) {
                predicates.add(criteriaBuilder.equal(root.get("sourceModule"), sourceModule));
            }
            if (action != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }
            if (objectType != null) {
                predicates.add(criteriaBuilder.equal(root.get("objectType"), objectType));
            }
            if (objectId != null) {
                predicates.add(criteriaBuilder.equal(root.get("objectId"), objectId));
            }
            if (outcome != null) {
                predicates.add(criteriaBuilder.equal(root.get("outcome"), outcome));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("recordedAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("recordedAt"), to));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}

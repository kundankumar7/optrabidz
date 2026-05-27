package com.project.optrabidz.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    @Query(value = """
            select outbox_event_id
            from event_outbox
            where event_status = 'PENDING'
              and available_at <= :now
            order by available_at asc, outbox_event_id asc
            for update skip locked
            limit :batchSize
            """, nativeQuery = true)
    List<Long> lockDispatchableIds(@Param("now") Instant now, @Param("batchSize") int batchSize);
}

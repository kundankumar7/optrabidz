package com.project.optrabidz.notification.infrastructure.repository;

import com.project.optrabidz.notification.infrastructure.entity.NotificationDeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaNotificationDeliveryAttemptRepository extends JpaRepository<NotificationDeliveryAttempt, Long> {
}

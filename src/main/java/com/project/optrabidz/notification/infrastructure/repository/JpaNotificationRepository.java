package com.project.optrabidz.notification.infrastructure.repository;

import com.project.optrabidz.notification.infrastructure.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaNotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByEventIdAndNotificationName(String eventId, String notificationName);
}

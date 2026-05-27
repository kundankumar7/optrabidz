package com.project.optrabidz.notification.infrastructure.repository;

import com.project.optrabidz.notification.infrastructure.entity.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaNotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {
}

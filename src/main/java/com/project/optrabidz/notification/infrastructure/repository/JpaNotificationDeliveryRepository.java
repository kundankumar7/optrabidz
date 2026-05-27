package com.project.optrabidz.notification.infrastructure.repository;

import com.project.optrabidz.notification.infrastructure.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaNotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
}

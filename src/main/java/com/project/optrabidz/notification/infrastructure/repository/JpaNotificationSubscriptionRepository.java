package com.project.optrabidz.notification.infrastructure.repository;

import com.project.optrabidz.notification.infrastructure.entity.NotificationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaNotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {
}

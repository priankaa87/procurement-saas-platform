package com.procurementsaas.notification.repo;

import com.procurementsaas.notification.domain.Notification;
import com.procurementsaas.notification.domain.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(String recipient);
    List<Notification> findByStatusOrderByCreatedAtDesc(NotificationStatus status);
    boolean existsByEventKey(String eventKey);
}

package com.company.ecommerce.notification.infrastructure.persistence;

import com.company.ecommerce.notification.domain.NotificationLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link NotificationLog}. Internal to the {@code notification} module. */
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {}

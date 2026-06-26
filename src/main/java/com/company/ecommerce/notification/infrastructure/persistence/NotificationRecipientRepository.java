package com.company.ecommerce.notification.infrastructure.persistence;

import com.company.ecommerce.notification.domain.NotificationRecipient;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link NotificationRecipient}. Internal to the {@code notification} module. */
public interface NotificationRecipientRepository
        extends JpaRepository<NotificationRecipient, UUID> {

    Optional<NotificationRecipient> findByUserId(UUID userId);
}

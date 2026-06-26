package com.company.ecommerce.notification.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Local replica of a recipient's contact details, owned by the {@code notification} module.
 *
 * <p>Populated from {@code UserRegisteredEvent} (the auth {@code userId} and email) so the module can
 * address later order/payment/shipment notifications — whose {@code customerId} equals the same auth
 * {@code userId} — without depending on the {@code user} module. This is event-carried state transfer,
 * keeping notification dependency-free.
 */
@Entity
@Table(name = "notification_recipients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationRecipient extends AuditableEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    private NotificationRecipient(UUID userId, String email, String firstName) {
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
    }

    /** Creates a recipient replica for a newly registered user. */
    public static NotificationRecipient of(UUID userId, String email, String firstName) {
        return new NotificationRecipient(userId, email, firstName);
    }

    /** Updates the cached contact details (e.g. when the user changes their email). */
    public void update(String email, String firstName) {
        this.email = email;
        this.firstName = firstName;
    }
}

package com.company.ecommerce.auth.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Authentication aggregate root: a user's login credentials and role.
 *
 * <p>Owned exclusively by the {@code auth} module. The plaintext password is never stored — only a
 * BCrypt hash. Profile data (name, addresses) lives in the {@code user} module and is linked by this
 * aggregate's {@code id}, propagated via {@code UserRegisteredEvent}.
 */
@Entity
@Table(name = "auth_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCredential extends AuditableEntity {

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    private UserCredential(String email, String passwordHash, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = true;
    }

    /** Factory for registering a new user with an already-encoded password hash. */
    public static UserCredential register(String email, String passwordHash, Role role) {
        return new UserCredential(email, passwordHash, role);
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void disable() {
        this.enabled = false;
    }
}
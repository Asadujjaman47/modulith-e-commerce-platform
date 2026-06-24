package com.company.ecommerce.auth.infrastructure.persistence;

import com.company.ecommerce.auth.domain.UserCredential;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link UserCredential}. Internal to the {@code auth} module. */
public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {

    Optional<UserCredential> findByEmail(String email);

    boolean existsByEmail(String email);
}
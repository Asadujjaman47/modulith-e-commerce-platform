package com.company.ecommerce.user.infrastructure.persistence;

import com.company.ecommerce.user.domain.Address;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Address}. Internal to the {@code user} module. */
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByCustomerIdOrderByCreatedAtAsc(UUID customerId);

    Optional<Address> findByIdAndCustomerId(UUID id, UUID customerId);

    List<Address> findByCustomerIdAndDefaultAddressTrue(UUID customerId);
}
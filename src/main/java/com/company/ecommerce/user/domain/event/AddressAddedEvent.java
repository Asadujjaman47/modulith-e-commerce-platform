package com.company.ecommerce.user.domain.event;

import java.util.UUID;

/**
 * Published when an address is added to a customer.
 *
 * @param addressId the address aggregate id
 * @param customerId the owning customer id
 */
public record AddressAddedEvent(UUID addressId, UUID customerId) {}
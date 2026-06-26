package com.company.ecommerce.user.spi;

import java.util.UUID;

/**
 * Read-only projection of a customer address for cross-module consumers (e.g. {@code order}, which
 * snapshots it as the order's shipping address).
 */
public record AddressView(
        UUID id,
        String label,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country) {}

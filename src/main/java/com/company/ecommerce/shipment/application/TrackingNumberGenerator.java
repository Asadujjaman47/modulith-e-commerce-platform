package com.company.ecommerce.shipment.application;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Generates human-readable, unique-enough tracking numbers, e.g. {@code TRK-7F3K9Q2M}. */
@Component
public class TrackingNumberGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 10;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder suffix = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            suffix.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return "TRK-" + suffix;
    }
}

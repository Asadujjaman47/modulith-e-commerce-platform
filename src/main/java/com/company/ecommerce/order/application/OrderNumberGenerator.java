package com.company.ecommerce.order.application;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Generates human-readable, hard-to-guess order numbers of the form {@code ORD-yyyyMMdd-XXXXXX}.
 * Uniqueness is ultimately guaranteed by the unique constraint on {@code orders.order_number}.
 */
@Component
class OrderNumberGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int SUFFIX_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    String generate() {
        StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            suffix.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return "ORD-%s-%s".formatted(LocalDate.now(ZoneOffset.UTC).format(DATE), suffix);
    }
}

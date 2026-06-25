package com.company.ecommerce.catalog.application;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.util.StringUtils;

/** Generates URL-friendly slugs from human-readable names. */
final class Slugs {

    private Slugs() {}

    /**
     * Returns {@code explicitSlug} when provided, otherwise a slug derived from {@code name}
     * (lower-cased, accents stripped, non-alphanumerics collapsed to single hyphens).
     */
    static String resolve(String explicitSlug, String name) {
        if (StringUtils.hasText(explicitSlug)) {
            return slugify(explicitSlug);
        }
        return slugify(name);
    }

    private static String slugify(String input) {
        String normalized =
                Normalizer.normalize(input, Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
    }
}
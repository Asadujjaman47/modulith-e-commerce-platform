package com.company.ecommerce.review.domain;

/** Moderation status of a {@link Review}. Hidden reviews are excluded from public listings. */
public enum ReviewStatus {
    PUBLISHED,
    HIDDEN
}

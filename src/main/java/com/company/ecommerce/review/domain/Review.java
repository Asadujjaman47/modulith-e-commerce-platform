package com.company.ecommerce.review.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import com.company.ecommerce.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Product review aggregate root. Owned by the {@code review} module.
 *
 * <p>A customer may review a product at most once (enforced by a unique constraint). The rating is
 * constrained to 1–5 in the domain. The author's display name is snapshotted at creation time (read
 * from the user {@code spi}) so listings never need to call back into the user module. References the
 * product and customer by id value only — no cross-module FKs.
 */
@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends AuditableEntity {

    /** Minimum and maximum allowed star rating. */
    public static final int MIN_RATING = 1;

    public static final int MAX_RATING = 5;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "title")
    private String title;

    @Column(name = "comment")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReviewStatus status;

    private Review(
            UUID productId,
            UUID customerId,
            String authorName,
            int rating,
            String title,
            String comment) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new BusinessException(
                    "Rating must be between %d and %d".formatted(MIN_RATING, MAX_RATING));
        }
        this.productId = productId;
        this.customerId = customerId;
        this.authorName = authorName;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.status = ReviewStatus.PUBLISHED;
    }

    /** Creates a new published review. */
    public static Review create(
            UUID productId,
            UUID customerId,
            String authorName,
            int rating,
            String title,
            String comment) {
        return new Review(productId, customerId, authorName, rating, title, comment);
    }

    /** Hides the review from public listings (admin moderation). */
    public void hide() {
        this.status = ReviewStatus.HIDDEN;
    }

    /** Restores a hidden review to public listings (admin moderation). */
    public void publish() {
        this.status = ReviewStatus.PUBLISHED;
    }

    public boolean isPublished() {
        return status == ReviewStatus.PUBLISHED;
    }
}

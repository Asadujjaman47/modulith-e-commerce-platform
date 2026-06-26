package com.company.ecommerce.review.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import com.company.ecommerce.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Aggregate rating for a product, owned by the {@code review} module (table {@code ratings}). Holds the
 * running count and sum of published review ratings, with the average derived and stored to two
 * decimals. Maintained as reviews are created, deleted or moderated. References the product by id value
 * only — no cross-module FK.
 */
@Entity
@Table(name = "ratings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductRating extends AuditableEntity {

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "rating_sum", nullable = false)
    private long ratingSum;

    @Column(name = "average_rating", nullable = false)
    private BigDecimal averageRating = BigDecimal.ZERO;

    private ProductRating(UUID productId) {
        this.productId = productId;
        this.reviewCount = 0;
        this.ratingSum = 0L;
        this.averageRating = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /** Creates an empty rating for a product (no reviews yet). */
    public static ProductRating empty(UUID productId) {
        return new ProductRating(productId);
    }

    /** Adds a rating to the aggregate (a new or restored published review). */
    public void addRating(int rating) {
        this.reviewCount += 1;
        this.ratingSum += rating;
        recomputeAverage();
    }

    /** Removes a rating from the aggregate (a deleted or hidden published review). */
    public void removeRating(int rating) {
        if (reviewCount == 0) {
            throw new BusinessException("Cannot remove a rating from an empty product rating");
        }
        this.reviewCount -= 1;
        this.ratingSum -= rating;
        recomputeAverage();
    }

    private void recomputeAverage() {
        this.averageRating =
                reviewCount == 0
                        ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.valueOf(ratingSum)
                                .divide(BigDecimal.valueOf(reviewCount), 2, RoundingMode.HALF_UP);
    }
}

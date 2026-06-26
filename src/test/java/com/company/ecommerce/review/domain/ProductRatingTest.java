package com.company.ecommerce.review.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ecommerce.common.exception.BusinessException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductRatingTest {

    private final UUID productId = UUID.randomUUID();

    @Test
    void emptyRatingHasZeroAverage() {
        ProductRating rating = ProductRating.empty(productId);

        assertThat(rating.getReviewCount()).isZero();
        assertThat(rating.getRatingSum()).isZero();
        assertThat(rating.getAverageRating()).isEqualByComparingTo("0.00");
    }

    @Test
    void addingRatingsComputesRoundedAverage() {
        ProductRating rating = ProductRating.empty(productId);

        rating.addRating(5);
        rating.addRating(4);
        rating.addRating(4);

        assertThat(rating.getReviewCount()).isEqualTo(3);
        assertThat(rating.getRatingSum()).isEqualTo(13);
        // 13 / 3 = 4.333... -> 4.33
        assertThat(rating.getAverageRating()).isEqualByComparingTo(new BigDecimal("4.33"));
    }

    @Test
    void removingRatingUpdatesAverage() {
        ProductRating rating = ProductRating.empty(productId);
        rating.addRating(5);
        rating.addRating(1);

        rating.removeRating(1);

        assertThat(rating.getReviewCount()).isEqualTo(1);
        assertThat(rating.getAverageRating()).isEqualByComparingTo("5.00");
    }

    @Test
    void removingLastRatingResetsAverageToZero() {
        ProductRating rating = ProductRating.empty(productId);
        rating.addRating(3);

        rating.removeRating(3);

        assertThat(rating.getReviewCount()).isZero();
        assertThat(rating.getAverageRating()).isEqualByComparingTo("0.00");
    }

    @Test
    void removingFromEmptyRatingFails() {
        ProductRating rating = ProductRating.empty(productId);

        assertThatThrownBy(() -> rating.removeRating(3)).isInstanceOf(BusinessException.class);
    }
}

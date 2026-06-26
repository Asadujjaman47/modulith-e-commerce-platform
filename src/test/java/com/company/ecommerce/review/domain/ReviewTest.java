package com.company.ecommerce.review.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ecommerce.common.exception.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewTest {

    private final UUID productId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    private Review review(int rating) {
        return Review.create(productId, customerId, "Jane Doe", rating, "Title", "Comment");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 5})
    void createsWithValidRating(int rating) {
        Review review = review(rating);

        assertThat(review.getRating()).isEqualTo(rating);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
        assertThat(review.isPublished()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 6, 10})
    void rejectsOutOfRangeRating(int rating) {
        assertThatThrownBy(() -> review(rating))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("between");
    }

    @Test
    void hideAndPublishToggleStatus() {
        Review review = review(5);

        review.hide();
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        assertThat(review.isPublished()).isFalse();

        review.publish();
        assertThat(review.isPublished()).isTrue();
    }
}

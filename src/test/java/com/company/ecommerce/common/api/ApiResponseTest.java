package com.company.ecommerce.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void successWrapsPayloadWithDefaultMessage() {
        ApiResponse<String> response = ApiResponse.success("payload");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Success");
        assertThat(response.data()).isEqualTo("payload");
    }

    @Test
    void successAcceptsCustomMessage() {
        ApiResponse<Integer> response = ApiResponse.success("Created", 42);

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Created");
        assertThat(response.data()).isEqualTo(42);
    }

    @Test
    void errorResponseDefaultsToEmptyErrorList() {
        ErrorResponse response = ErrorResponse.of("Boom");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Boom");
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void errorResponseKeepsProvidedErrors() {
        ErrorResponse response = ErrorResponse.of("Validation failed", List.of("name: must not be blank"));

        assertThat(response.success()).isFalse();
        assertThat(response.errors()).containsExactly("name: must not be blank");
    }
}
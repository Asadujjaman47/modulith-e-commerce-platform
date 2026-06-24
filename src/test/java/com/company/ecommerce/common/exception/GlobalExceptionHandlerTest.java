package com.company.ecommerce.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.ecommerce.common.api.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundMapsTo404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new EntityNotFoundException("Product", "abc"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Product not found: abc");
    }

    @Test
    void businessExceptionMapsTo409() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBusiness(new BusinessException("Out of stock"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Out of stock");
    }

    @Test
    void accessDeniedMapsTo403() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAccessDenied(new AccessDeniedException("nope"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Access denied");
    }
}
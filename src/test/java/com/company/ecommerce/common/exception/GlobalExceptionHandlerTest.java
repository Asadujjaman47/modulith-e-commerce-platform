package com.company.ecommerce.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.ecommerce.common.api.ErrorResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @Test
    void unsupportedMethodMapsTo405() {
        ResponseEntity<ErrorResponse> response =
                handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("POST"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Request method 'POST' is not supported");
    }

    @Test
    void unreadableBodyMapsTo400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnreadableMessage(
                        new HttpMessageNotReadableException("bad json", (HttpInputMessage) null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Malformed or unreadable request body");
    }

    @Test
    void typeMismatchMapsTo400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleTypeMismatch(
                        new MethodArgumentTypeMismatchException(
                                "not-a-uuid",
                                UUID.class,
                                "orderId",
                                null,
                                new IllegalArgumentException("Invalid UUID string")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid value for parameter 'orderId'");
    }
}
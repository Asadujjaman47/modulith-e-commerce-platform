package com.company.ecommerce.common.exception;

/**
 * Raised when a domain/business rule is violated. Mapped to HTTP 409 by the global handler.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
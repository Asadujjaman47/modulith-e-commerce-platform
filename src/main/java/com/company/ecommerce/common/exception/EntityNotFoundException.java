package com.company.ecommerce.common.exception;

/**
 * Raised when a requested entity does not exist. Mapped to HTTP 404 by the global handler.
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entity, Object id) {
        super("%s not found: %s".formatted(entity, id));
    }
}
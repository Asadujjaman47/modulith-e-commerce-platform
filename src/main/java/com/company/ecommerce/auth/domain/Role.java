package com.company.ecommerce.auth.domain;

/**
 * Application roles. Persisted by name and surfaced as Spring Security authorities prefixed with
 * {@code ROLE_} (e.g. {@code ROLE_CUSTOMER}).
 */
public enum Role {
    ADMIN,
    CUSTOMER;

    /** Spring Security authority representation. */
    public String authority() {
        return "ROLE_" + name();
    }
}
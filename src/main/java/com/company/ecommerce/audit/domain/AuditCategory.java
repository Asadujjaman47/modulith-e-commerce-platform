package com.company.ecommerce.audit.domain;

/** High-level functional area an audit log entry belongs to. */
public enum AuditCategory {
    AUTH,
    USER,
    CATALOG,
    INVENTORY,
    COUPON,
    ORDER,
    PAYMENT,
    SHIPMENT,
    REVIEW,
    NOTIFICATION
}

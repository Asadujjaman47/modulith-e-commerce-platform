package com.company.ecommerce.payment.domain;

/** The kind of gateway interaction recorded by a {@link PaymentTransaction}. */
public enum PaymentTransactionType {
    CHARGE,
    REFUND
}

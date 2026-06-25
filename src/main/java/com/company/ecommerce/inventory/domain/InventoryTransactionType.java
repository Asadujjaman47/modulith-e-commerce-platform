package com.company.ecommerce.inventory.domain;

/** Category of an {@link InventoryTransaction} ledger entry. */
public enum InventoryTransactionType {
    /** Initial stock seeded for a newly created product. */
    RECEIPT,
    /** Manual on-hand correction by an administrator. */
    ADJUSTMENT,
    /** Stock reserved. */
    RESERVE,
    /** Previously reserved stock released. */
    RELEASE
}
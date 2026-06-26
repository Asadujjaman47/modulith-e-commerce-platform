package com.company.ecommerce.payment.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import com.company.ecommerce.common.exception.BusinessException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Payment aggregate root. Owned by the {@code payment} module.
 *
 * <p>Created as a {@code PENDING} intent for an order (one payment per order), then driven to
 * {@code SUCCESS}/{@code FAILED} by a gateway charge and optionally {@code REFUNDED} later. Status
 * changes are guarded by the {@link PaymentStatus} state machine and each gateway interaction is
 * appended to the {@link PaymentTransaction} ledger child. References the order and customer by id
 * value only — no cross-module FKs.
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends AuditableEntity {

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "method")
    private PaymentMethod method;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "gateway_reference")
    private String gatewayReference;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<PaymentTransaction> transactions = new ArrayList<>();

    private Payment(UUID orderId, UUID customerId, BigDecimal amount, String currency) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Payment amount must be positive");
        }
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentStatus.PENDING;
    }

    /** Creates a new {@code PENDING} payment intent for an order. */
    public static Payment createIntent(
            UUID orderId, UUID customerId, BigDecimal amount, String currency) {
        return new Payment(orderId, customerId, amount, currency);
    }

    /** Resets a {@code FAILED} payment back to {@code PENDING} so it can be charged again. */
    public void retry() {
        transitionTo(PaymentStatus.PENDING);
        this.failureReason = null;
    }

    /** Records a successful charge, moving the payment to {@code SUCCESS}. */
    public void markSucceeded(PaymentMethod method, String gatewayReference) {
        transitionTo(PaymentStatus.SUCCESS);
        this.method = method;
        this.gatewayReference = gatewayReference;
        this.failureReason = null;
        this.paidAt = Instant.now();
        addTransaction(PaymentTransactionType.CHARGE, true, gatewayReference, "Charge approved");
    }

    /** Records a failed charge, moving the payment to {@code FAILED}. */
    public void markFailed(PaymentMethod method, String reason) {
        transitionTo(PaymentStatus.FAILED);
        this.method = method;
        this.failureReason = reason;
        this.failedAt = Instant.now();
        addTransaction(PaymentTransactionType.CHARGE, false, null, reason);
    }

    /** Refunds a successful payment, moving it to {@code REFUNDED}. */
    public void refund(String gatewayReference) {
        transitionTo(PaymentStatus.REFUNDED);
        this.refundedAt = Instant.now();
        addTransaction(PaymentTransactionType.REFUND, true, gatewayReference, "Refund issued");
    }

    /** Whether the payment has been settled successfully (and not since refunded). */
    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCESS;
    }

    public void assignIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    private void transitionTo(PaymentStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessException(
                    "Illegal payment status transition: %s -> %s".formatted(status, target));
        }
        this.status = target;
    }

    private void addTransaction(
            PaymentTransactionType type, boolean succeeded, String gatewayReference, String message) {
        transactions.add(
                new PaymentTransaction(this, type, succeeded, amount, gatewayReference, message));
    }
}

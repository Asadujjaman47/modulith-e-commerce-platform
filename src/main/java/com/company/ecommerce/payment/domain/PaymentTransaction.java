package com.company.ecommerce.payment.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An append-only record of a single gateway interaction (charge or refund) for a {@link Payment}.
 * Child of the Payment aggregate — never referenced or persisted independently of its owning payment.
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransaction extends AuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PaymentTransactionType type;

    @Column(name = "succeeded", nullable = false)
    private boolean succeeded;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "gateway_reference")
    private String gatewayReference;

    @Column(name = "message")
    private String message;

    PaymentTransaction(
            Payment payment,
            PaymentTransactionType type,
            boolean succeeded,
            BigDecimal amount,
            String gatewayReference,
            String message) {
        this.payment = payment;
        this.type = type;
        this.succeeded = succeeded;
        this.amount = amount;
        this.gatewayReference = gatewayReference;
        this.message = message;
    }

    /** Convenience id accessor for mapping. */
    public UUID paymentId() {
        return payment.getId();
    }
}

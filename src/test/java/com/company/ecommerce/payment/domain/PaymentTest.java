package com.company.ecommerce.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ecommerce.common.exception.BusinessException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private Payment intent() {
        return Payment.createIntent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), "USD");
    }

    @Test
    void createsPendingIntent() {
        Payment payment = intent();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getAmount()).isEqualByComparingTo("100.00");
        assertThat(payment.getTransactions()).isEmpty();
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(
                        () ->
                                Payment.createIntent(
                                        UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO, "USD"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void marksSucceededAndRecordsCharge() {
        Payment payment = intent();

        payment.markSucceeded(PaymentMethod.CARD, "REF-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.isSuccessful()).isTrue();
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.getGatewayReference()).isEqualTo("REF-1");
        assertThat(payment.getTransactions()).hasSize(1);
        assertThat(payment.getTransactions().get(0).getType()).isEqualTo(PaymentTransactionType.CHARGE);
        assertThat(payment.getTransactions().get(0).isSucceeded()).isTrue();
    }

    @Test
    void marksFailedAndRecordsFailedCharge() {
        Payment payment = intent();

        payment.markFailed(PaymentMethod.CARD, "Declined");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Declined");
        assertThat(payment.getTransactions()).hasSize(1);
        assertThat(payment.getTransactions().get(0).isSucceeded()).isFalse();
    }

    @Test
    void retriesFromFailedThenSucceeds() {
        Payment payment = intent();
        payment.markFailed(PaymentMethod.CARD, "Declined");

        payment.retry();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getFailureReason()).isNull();

        payment.markSucceeded(PaymentMethod.CARD, "REF-2");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void refundsSuccessfulPayment() {
        Payment payment = intent();
        payment.markSucceeded(PaymentMethod.CARD, "REF-1");

        payment.refund("RFD-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAt()).isNotNull();
        assertThat(payment.getTransactions()).hasSize(2);
        assertThat(payment.getTransactions().get(1).getType()).isEqualTo(PaymentTransactionType.REFUND);
    }

    @Test
    void cannotRefundPendingPayment() {
        Payment payment = intent();

        assertThatThrownBy(() -> payment.refund("RFD-1")).isInstanceOf(BusinessException.class);
    }

    @Test
    void cannotChargeRefundedPayment() {
        Payment payment = intent();
        payment.markSucceeded(PaymentMethod.CARD, "REF-1");
        payment.refund("RFD-1");

        assertThatThrownBy(() -> payment.markSucceeded(PaymentMethod.CARD, "REF-2"))
                .isInstanceOf(BusinessException.class);
    }
}

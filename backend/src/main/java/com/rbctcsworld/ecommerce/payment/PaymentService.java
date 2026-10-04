package com.rbctcsworld.ecommerce.payment;

import com.rbctcsworld.ecommerce.common.exception.BusinessRuleException;
import com.rbctcsworld.ecommerce.common.exception.PaymentDeclinedException;
import com.rbctcsworld.ecommerce.payment.MockPaymentGateway.GatewayResult;
import com.rbctcsworld.ecommerce.payment.PaymentDtos.PayRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

/**
 * Charges and refunds through the gateway and records every attempt.
 * Runs inside the caller's (OrderService) transaction.
 *
 * noRollbackFor: a declined card throws PaymentDeclinedException (-> HTTP 402), but the FAILED
 * transaction row must still be committed for audit/fraud analysis. Without this setting the
 * exception would mark the whole transaction rollback-only and the failed attempt would vanish.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY, noRollbackFor = PaymentDeclinedException.class)
public class PaymentService {

    private final MockPaymentGateway gateway;
    private final PaymentTransactionRepository transactions;
    private final Clock clock;

    public PaymentService(MockPaymentGateway gateway, PaymentTransactionRepository transactions, Clock clock) {
        this.gateway = gateway;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * 400 for malformed/expired card (nothing recorded - the card never reached the gateway),
     * 402 for a declined card (FAILED transaction IS recorded), otherwise a SUCCEEDED transaction.
     */
    public PaymentTransaction charge(Long orderId, BigDecimal amount, PayRequest card) {
        String number = card.cardNumber().replace(" ", "");
        if (number.length() < 13 || number.length() > 19 || !MockPaymentGateway.luhnValid(number)) {
            throw new BusinessRuleException("Invalid card number");
        }
        if (YearMonth.of(card.expiryYear(), card.expiryMonth()).isBefore(YearMonth.now(clock))) {
            throw new BusinessRuleException("Card has expired");
        }
        String last4 = number.substring(number.length() - 4);

        GatewayResult result = gateway.charge(number, amount);
        if (!result.approved()) {
            transactions.save(new PaymentTransaction(orderId, PaymentTransaction.CHARGE, PaymentTransaction.FAILED,
                    amount, last4, result.reason()));
            throw new PaymentDeclinedException("Payment declined: " + result.reason());
        }
        return transactions.save(new PaymentTransaction(orderId, PaymentTransaction.CHARGE,
                PaymentTransaction.SUCCEEDED, amount, last4, null));
    }

    /** Refunds the full amount of the successful charge (used when a PAID order is cancelled). */
    public PaymentTransaction refund(Long orderId, BigDecimal amount) {
        String last4 = transactions.findByOrderIdOrderByIdAsc(orderId).stream()
                .filter(t -> PaymentTransaction.CHARGE.equals(t.getType()) && PaymentTransaction.SUCCEEDED.equals(t.getStatus()))
                .map(PaymentTransaction::getCardLast4)
                .findFirst().orElse(null);
        gateway.refund(amount);
        return transactions.save(new PaymentTransaction(orderId, PaymentTransaction.REFUND,
                PaymentTransaction.SUCCEEDED, amount, last4, null));
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<PaymentTransaction> history(Long orderId) {
        return transactions.findByOrderIdOrderByIdAsc(orderId);
    }
}

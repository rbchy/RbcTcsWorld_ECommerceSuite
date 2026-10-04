package com.rbctcsworld.ecommerce.common.exception;

/** The (mock) payment provider refused the charge. Maps to HTTP 402 Payment Required. */
public class PaymentDeclinedException extends RuntimeException {
    public PaymentDeclinedException(String message) {
        super(message);
    }
}

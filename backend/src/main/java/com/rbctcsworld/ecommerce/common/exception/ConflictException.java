package com.rbctcsworld.ecommerce.common.exception;

/** Request conflicts with current state (duplicate SKU/email, insufficient stock). Maps to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

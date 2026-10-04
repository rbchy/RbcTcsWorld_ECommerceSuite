package com.rbctcsworld.ecommerce.common.exception;

/** Requested resource does not exist (or does not belong to the caller). Maps to HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}

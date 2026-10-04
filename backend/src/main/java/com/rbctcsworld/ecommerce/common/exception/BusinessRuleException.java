package com.rbctcsworld.ecommerce.common.exception;

/** Input is well-formed but breaks a business rule (e.g. quantity limit). Maps to HTTP 400. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}

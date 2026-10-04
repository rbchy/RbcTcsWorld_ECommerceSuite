package com.rbctcsworld.ecommerce.common.exception;

/** Logged in, but this action is not allowed for this user (e.g. reviewing a product never received). HTTP 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}

package com.rbctcsworld.ecommerce.auth;

/** Role names stored in users.role (VARCHAR). Spring Security sees them as ROLE_CUSTOMER / ROLE_ADMIN. */
public final class Role {
    public static final String CUSTOMER = "CUSTOMER";
    public static final String ADMIN = "ADMIN";

    private Role() {
    }
}

package com.rbctcsworld.ecommerce.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/auth/register -> 201 {token,email,role} | 400 validation | 409 duplicate email
 * POST /api/auth/login    -> 200 {token,email,role} | 400 validation | 401 bad credentials
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    record AuthRequest(@Email @NotBlank String email, @NotBlank @Size(min = 8, max = 72) String password) {
    }

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthService.AuthResult register(@Valid @RequestBody AuthRequest r) {
        return service.register(r.email(), r.password());
    }

    @PostMapping("/login")
    AuthService.AuthResult login(@Valid @RequestBody AuthRequest r) {
        return service.login(r.email(), r.password());
    }
}

package com.rbctcsworld.ecommerce.auth;

import com.rbctcsworld.ecommerce.common.exception.ConflictException;
import com.rbctcsworld.ecommerce.common.exception.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository repo, PasswordEncoder encoder, JwtService jwt) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** New accounts are always CUSTOMER; admins are only created by DataSeeder. */
    public AuthResult register(String rawEmail, String password) {
        String email = normalize(rawEmail);
        if (repo.findByEmail(email).isPresent()) {
            throw new ConflictException("Email already registered");
        }
        AppUser user = repo.save(new AppUser(email, encoder.encode(password)));
        return new AuthResult(jwt.generate(email), email, user.getRole());
    }

    /** Same message for unknown email and wrong password, so attackers cannot discover accounts. */
    public AuthResult login(String rawEmail, String password) {
        String email = normalize(rawEmail);
        AppUser user = repo.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (!encoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return new AuthResult(jwt.generate(email), email, user.getRole());
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public record AuthResult(String token, String email, String role) {
    }
}

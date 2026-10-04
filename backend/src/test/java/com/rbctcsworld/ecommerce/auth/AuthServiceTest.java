package com.rbctcsworld.ecommerce.auth;

import com.rbctcsworld.ecommerce.common.exception.ConflictException;
import com.rbctcsworld.ecommerce.common.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository repo;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    @InjectMocks AuthService service;

    @Test
    void registerNormalizesEmailAndCreatesCustomer() {
        when(repo.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(encoder.encode("Password1!")).thenReturn("hash");
        when(repo.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwt.generate("new@test.com")).thenReturn("token");

        AuthService.AuthResult result = service.register("  New@Test.com ", "Password1!");

        assertThat(result.email()).isEqualTo("new@test.com");
        assertThat(result.role()).isEqualTo(Role.CUSTOMER);
        assertThat(result.token()).isEqualTo("token");
    }

    @Test
    void registerDuplicateEmailIsConflict() {
        when(repo.findByEmail("dup@test.com")).thenReturn(Optional.of(new AppUser("dup@test.com", "x")));

        assertThatThrownBy(() -> service.register("dup@test.com", "Password1!"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void loginWithWrongPasswordIsInvalidCredentials() {
        when(repo.findByEmail("a@test.com")).thenReturn(Optional.of(new AppUser("a@test.com", "hash")));
        when(encoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login("a@test.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginWithUnknownEmailGivesSameError() {
        when(repo.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("ghost@test.com", "whatever1"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}

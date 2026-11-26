package dev.enzo.giacpe.identity.infrastructure.security;

import dev.enzo.giacpe.identity.domain.model.ActivationStatus;
import dev.enzo.giacpe.identity.domain.model.User;
import dev.enzo.giacpe.identity.domain.model.UserRole;
import dev.enzo.giacpe.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationManagerTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new PasswordConfig().passwordEncoder();

        DatabaseUserDetailsService userDetailsService =
                new DatabaseUserDetailsService(userRepository);

        SecurityConfig config = new SecurityConfig();

        AuthenticationProvider provider =
                config.authenticationProvider(userDetailsService, passwordEncoder);

        authenticationManager = config.authenticationManager(provider);
    }

    @Test
    void shouldAuthenticateWithValidCredentials() {
        String email = "test@example.com";
        String password = "SecurePassword123!";

        User user = new User(
                UUID.randomUUID(),
                "Test User",
                email,
                passwordEncoder.encode(password),
                UserRole.FIELD_OFFICER,
                ActivationStatus.ACTIVATED,
                true,
                Instant.now(),
                Instant.now()
        );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        email,
                        password
                )
        );

        assertAll(
                () -> assertTrue(authentication.isAuthenticated()),
                () -> assertEquals(email, authentication.getName())
        );
    }

    @Test
    void shouldRejectInvalidCredentials() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(
                                "unknown@example.com",
                                "WrongPassword!"
                        )
                )
        );
    }
}

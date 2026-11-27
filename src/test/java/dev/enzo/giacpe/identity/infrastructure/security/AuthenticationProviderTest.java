package dev.enzo.giacpe.identity.infrastructure.security;

import dev.enzo.giacpe.identity.domain.model.ActivationStatus;
import dev.enzo.giacpe.identity.domain.model.User;
import dev.enzo.giacpe.identity.domain.model.UserRole;
import dev.enzo.giacpe.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationProviderTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationProvider authenticationProvider;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new PasswordConfig().passwordEncoder();

        DatabaseUserDetailsService userDetailsService =
                new DatabaseUserDetailsService(userRepository);

        authenticationProvider = new SecurityConfig()
                .authenticationProvider(userDetailsService, passwordEncoder);
    }

    private Authentication authenticate(String email, String password) {
        return authenticationProvider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        email,
                        password
                )
        );
    }

    private User createUser(ActivationStatus status, boolean enabled) {
        Instant now = Instant.parse("2026-11-25T10:00:00Z");

        String passwordHash = status == ActivationStatus.ACTIVATED
                ? passwordEncoder.encode("SecurePassword123!")
                : null;

        return new User(
                UUID.randomUUID(),
                "Test User",
                "test@example.com",
                passwordHash,
                UserRole.FIELD_OFFICER,
                status,
                enabled,
                now,
                now
        );
    }

    @Test
    void shouldAuthenticateActivatedAndEnabledUser() {
        User user = createUser(ActivationStatus.ACTIVATED, true);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        Authentication result = authenticate(
                user.getEmail(),
                "SecurePassword123!"
        );

        assertAll(
                () -> assertTrue(result.isAuthenticated()),
                () -> assertEquals(user.getEmail(), result.getName()),
                () -> assertTrue(result.getAuthorities().stream()
                        .anyMatch(authority ->
                                authority.getAuthority().equals("ROLE_FIELD_OFFICER")))
        );
    }

    @Test
    void shouldRejectIncorrectPassword() {
        User user = createUser(ActivationStatus.ACTIVATED, true);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        assertThrows(
                BadCredentialsException.class,
                () -> authenticate(user.getEmail(), "WrongPassword!")
        );
    }

    @Test
    void shouldRejectPendingUser() {
        User user = createUser(ActivationStatus.PENDING, false);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        assertThrows(
                DisabledException.class,
                () -> authenticate(user.getEmail(), "SecurePassword123!")
        );
    }

    @Test
    void shouldRejectDisabledUser() {
        User user = createUser(ActivationStatus.ACTIVATED, false);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        assertThrows(
                DisabledException.class,
                () -> authenticate(user.getEmail(), "SecurePassword123!")
        );
    }

    @Test
    void shouldRejectUnknownUser() {
        String email = "unknown@example.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authenticate(email, "SecurePassword123!")
        );
    }
}

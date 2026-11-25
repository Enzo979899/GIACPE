package dev.enzo.giacpe.identity.infrastructure.security;

import dev.enzo.giacpe.identity.domain.model.ActivationStatus;
import dev.enzo.giacpe.identity.domain.model.User;
import dev.enzo.giacpe.identity.domain.model.UserRole;
import dev.enzo.giacpe.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseUserDetailsServiceTest {

    private UserRepository userRepository;
    private DatabaseUserDetailsService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new DatabaseUserDetailsService(userRepository);
    }

    private User createUser(ActivationStatus status, boolean enabled) {
        Instant now = Instant.parse("2026-11-25T10:00:00Z");

        String passwordHash = status == ActivationStatus.ACTIVATED
                ? "$2a$10$exampleHash"
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
    void shouldLoadActivatedAndEnabledUser() {
        User user = createUser(ActivationStatus.ACTIVATED, true);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername(user.getEmail());

        assertAll(
                () -> assertEquals(user.getEmail(), details.getUsername()),
                () -> assertEquals(user.getPasswordHash(), details.getPassword()),
                () -> assertTrue(details.isEnabled()),
                () -> assertTrue(details.getAuthorities().stream()
                        .anyMatch(authority ->
                                authority.getAuthority().equals("ROLE_FIELD_OFFICER")))
        );
    }

    @Test
    void shouldDisablePendingUser() {
        User user = createUser(ActivationStatus.PENDING, false);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername(user.getEmail());

        assertAll(
                () -> assertFalse(details.isEnabled()),
                () -> assertEquals("", details.getPassword())
        );
    }

    @Test
    void shouldDisableDeactivatedUser() {
        User user = createUser(ActivationStatus.ACTIVATED, false);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername(user.getEmail());

        assertFalse(details.isEnabled());
    }

    @Test
    void shouldRejectUnknownUser() {
        String email = "unknown@example.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername(email)
        );
    }
}

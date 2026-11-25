package dev.enzo.giacpe.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private final Instant now = Instant.parse("2026-11-24T10:00:00Z");


    @Test
    void shouldCreateValidPendingUser() {
        UUID userId = UUID.randomUUID();

        User user = new User(
                userId,
                "Test User",
                "test@example.com",
                null,
                UserRole.FIELD_OFFICER,
                ActivationStatus.PENDING,
                false,
                now,
                now
        );

        assertAll(
                () -> assertEquals(userId, user.getUserId()),
                () -> assertEquals("Test User", user.getName()),
                () -> assertEquals("test@example.com", user.getEmail()),
                () -> assertNull(user.getPasswordHash()),
                () -> assertEquals(UserRole.FIELD_OFFICER, user.getUserRole()),
                () -> assertEquals(ActivationStatus.PENDING, user.getActivationStatus()),
                () -> assertFalse(user.isEnabled()),
                () -> assertEquals(now, user.getCreatedAt()),
                () -> assertEquals(now, user.getUpdatedAt())
        );
    }

    @Test
    void shouldRejectUserWithoutEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(
                        UUID.randomUUID(),
                        "Test User",
                        " ",
                        null,
                        UserRole.FIELD_OFFICER,
                        ActivationStatus.PENDING,
                        false,
                        now,
                        now
                )
        );
    }

    @Test
    void shouldRejectActivatedUserWithoutPasswordHash() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(
                        UUID.randomUUID(),
                        "Test User",
                        "test@example.com",
                        null,
                        UserRole.GLOBAL_ADMIN,
                        ActivationStatus.ACTIVATED,
                        true,
                        now,
                        now
                )
        );
    }

    @Test
    void shouldRejectUpdateTimestampBeforeCreation() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(
                        UUID.randomUUID(),
                        "Test User",
                        "test@example.com",
                        "hashed-password",
                        UserRole.GLOBAL_ADMIN,
                        ActivationStatus.ACTIVATED,
                        true,
                        now,
                        now.minusSeconds(60)
                )
        );
    }

    @Test
    void shouldCreatePendingUserWithDefaultValues() {
        User user = User.createPending(
                "Test User",
                "test@example.com",
                UserRole.FIELD_OFFICER,
                now
        );

        assertAll(
                () -> assertNotNull(user.getUserId()),
                () -> assertEquals("Test User", user.getName()),
                () -> assertEquals("test@example.com", user.getEmail()),
                () -> assertEquals(UserRole.FIELD_OFFICER, user.getUserRole()),
                () -> assertEquals(ActivationStatus.PENDING, user.getActivationStatus()),
                () -> assertFalse(user.isEnabled()),
                () -> assertNull(user.getPasswordHash()),
                () -> assertEquals(now, user.getCreatedAt()),
                () -> assertEquals(now, user.getUpdatedAt())
        );
    }

    @Test
    void shouldNormalizeEmailWhenCreatingUser() {
        User user = User.createPending(
                "Test User",
                "  Test.User@Example.COM  ",
                UserRole.FIELD_OFFICER,
                now
        );

        assertEquals(
                "test.user@example.com",
                user.getEmail()
        );
    }

    @Test
    void shouldRejectEnabledPendingUser() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new User(
                        UUID.randomUUID(),
                        "Test User",
                        "test@example.com",
                        null,
                        UserRole.FIELD_OFFICER,
                        ActivationStatus.PENDING,
                        true,
                        now,
                        now
                )
        );
    }
}

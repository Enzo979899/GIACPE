package dev.enzo.giacpe.identity.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class User {
    private final UUID userId;
    private String name;
    private String email;
    private String passwordHash;
    private UserRole userRole;
    private ActivationStatus activationStatus;
    private boolean enabled;
    private final Instant createdAt;
    private Instant updatedAt;

    public User(UUID userId, String name, String email, String passwordHash, UserRole userRole,
                ActivationStatus activationStatus, boolean enabled, Instant createdAt, Instant updatedAt) {
        this.userId = Objects.requireNonNull(userId, "User ID is required");
        this.name = requireText(name, "User name is required");
        this.email = normalizeEmail(email);
        this.userRole = Objects.requireNonNull(userRole, "User role is required");
        this.activationStatus = Objects.requireNonNull(activationStatus, "Activation status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation timestamp is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Update timestamp is required");

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Update timestamp cannot precede creation timestamp");
        }

        if (activationStatus == ActivationStatus.ACTIVATED && (passwordHash == null || passwordHash.isBlank())) {
            throw new IllegalArgumentException("Activated users must have a password hash");
        }

        if (activationStatus == ActivationStatus.PENDING && enabled) {
            throw new IllegalArgumentException("Pending users cannot be enabled");
        }


        this.passwordHash = passwordHash;
        this.enabled = enabled;
    }

    public static User createPending(String name, String email, UserRole role, Instant now) {
        return new User(UUID.randomUUID(), name, email, null, role, ActivationStatus.PENDING,
                false, now, now);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String normalizeEmail(String email) {
        return requireText(email, "User email is required").toLowerCase(Locale.ROOT);
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public ActivationStatus getActivationStatus() {
        return activationStatus;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}

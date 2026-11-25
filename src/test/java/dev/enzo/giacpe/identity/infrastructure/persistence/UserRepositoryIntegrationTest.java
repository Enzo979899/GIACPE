package dev.enzo.giacpe.identity.infrastructure.persistence;

import dev.enzo.giacpe.identity.domain.model.ActivationStatus;
import dev.enzo.giacpe.identity.domain.model.User;
import dev.enzo.giacpe.identity.domain.model.UserRole;
import dev.enzo.giacpe.identity.domain.repository.UserRepository;
import dev.enzo.giacpe.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SpringDataUserRepository springDataUserRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveAndFindUserById() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        User user = User.createPending(
                "Test User",
                "test-" + UUID.randomUUID() + "@example.com",
                UserRole.FIELD_OFFICER,
                now
        );

        User savedUser = userRepository.save(user);

        entityManager.flush();
        entityManager.clear();

        User foundUser = userRepository
                .findById(savedUser.getUserId())
                .orElseThrow();

        assertAll(
                () -> assertEquals(user.getUserId(), foundUser.getUserId()),
                () -> assertEquals("Test User", foundUser.getName()),
                () -> assertEquals(user.getEmail(), foundUser.getEmail()),
                () -> assertEquals(UserRole.FIELD_OFFICER, foundUser.getUserRole()),
                () -> assertEquals(ActivationStatus.PENDING, foundUser.getActivationStatus()),
                () -> assertFalse(foundUser.isEnabled()),
                () -> assertNull(foundUser.getPasswordHash()),
                () -> assertEquals(now, foundUser.getCreatedAt()),
                () -> assertEquals(now, foundUser.getUpdatedAt())
        );
    }

    @Test
    void shouldFindUserByEmailIgnoringCase() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        String email = "test-" + UUID.randomUUID() + "@example.com";

        User user = User.createPending(
                "Test User",
                email,
                UserRole.FIELD_OFFICER,
                now
        );

        userRepository.save(user);

        entityManager.flush();
        entityManager.clear();

        User foundUser = userRepository
                .findByEmail(email.toUpperCase())
                .orElseThrow();

        assertAll(
                () -> assertEquals(user.getUserId(), foundUser.getUserId()),
                () -> assertEquals(email, foundUser.getEmail())
        );
    }

    @Test
    void shouldCheckWhetherEmailExistsIgnoringCase() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        String email = "test-" + UUID.randomUUID() + "@example.com";

        User user = User.createPending(
                "Test User",
                email,
                UserRole.FIELD_OFFICER,
                now
        );

        userRepository.save(user);

        entityManager.flush();
        entityManager.clear();

        assertAll(
                () -> assertTrue(userRepository.existsByEmail(email.toUpperCase())),
                () -> assertFalse(userRepository.existsByEmail("missing-" + UUID.randomUUID() + "@example.com"))
        );
    }

    @Test
    void shouldRejectDuplicateEmailIgnoringCase() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        String email = "duplicate-" + UUID.randomUUID() + "@example.com";

        User firstUser = User.createPending(
                "First User",
                email,
                UserRole.FIELD_OFFICER,
                now
        );

        userRepository.save(firstUser);
        entityManager.flush();
        entityManager.clear();

        User secondUser = User.createPending(
                "Second User",
                email.toUpperCase(),
                UserRole.MANAGER,
                now
        );


        assertThrows(
                DataIntegrityViolationException.class,
                () -> springDataUserRepository.saveAndFlush(
                        UserPersistenceMapper.toEntity(secondUser)
                )
        );

    }

    @Test
    void shouldRejectEnabledPendingUserInDatabase() {
        UUID userId = UUID.randomUUID();
        String email = "invalid-" + userId + "@example.com";

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO users (
                            id, name, email, password_hash, role,
                            activation_status, enabled
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        userId,
                        "Test User",
                        email,
                        null,
                        "FIELD_OFFICER",
                        "PENDING",
                        true
                )
        );
    }

    @Test
    void shouldRejectActivatedUserWithBlankPasswordHash() {
        UUID userId = UUID.randomUUID();

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO users (
                            id, name, email, password_hash, role,
                            activation_status, enabled
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        userId,
                        "Test User",
                        "blank-" + userId + "@example.com",
                        "   ",
                        "FIELD_OFFICER",
                        "ACTIVATED",
                        true
                )
        );
    }

}

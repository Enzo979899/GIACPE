package dev.enzo.giacpe.identity.infrastructure.bootstrap;

import dev.enzo.giacpe.TestcontainersConfiguration;
import dev.enzo.giacpe.identity.domain.model.ActivationStatus;
import dev.enzo.giacpe.identity.domain.model.User;
import dev.enzo.giacpe.identity.domain.model.UserRole;
import dev.enzo.giacpe.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional(isolation = Isolation.SERIALIZABLE)
class InitialAdminBootstrapIntegrationTest {

    @Autowired
    private InitialAdminBootstrapService bootstrapService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    private boolean attemptBootstrap(
            String email,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {

        ready.countDown();

        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent test start timed out");
        }

        try {
            bootstrapService.createInitialAdmin(
                    "Initial Administrator",
                    email,
                    "SecurePassword123!"
            );

            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }


    @Test
    void shouldPersistInitialGlobalAdminInDatabase() {
        String email = "initial-admin-integration@example.com";
        String password = "SecurePassword123!";

        assertFalse(userRepository.existsByRole(UserRole.GLOBAL_ADMIN));

        bootstrapService.createInitialAdmin(
                "Initial Administrator",
                email,
                password
        );

        User savedAdmin = userRepository.findByEmail(email)
                .orElseThrow();

        assertAll(
                () -> assertEquals(
                        UserRole.GLOBAL_ADMIN,
                        savedAdmin.getUserRole()
                ),
                () -> assertEquals(
                        ActivationStatus.ACTIVATED,
                        savedAdmin.getActivationStatus()
                ),
                () -> assertTrue(savedAdmin.isEnabled()),
                () -> assertNotEquals(
                        password,
                        savedAdmin.getPasswordHash()
                ),
                () -> assertTrue(passwordEncoder.matches(
                        password,
                        savedAdmin.getPasswordHash()
                ))
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldPreventConcurrentInitialAdminCreation() throws Exception {
        assertFalse(userRepository.existsByRole(UserRole.GLOBAL_ADMIN));

        String firstEmail = "bootstrap-a-" + UUID.randomUUID() + "@example.com";
        String secondEmail = "bootstrap-b-" + UUID.randomUUID() + "@example.com";

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(() ->
                    attemptBootstrap(firstEmail, ready, start));

            var second = executor.submit(() ->
                    attemptBootstrap(secondEmail, ready, start));

            assertTrue(ready.await(10, TimeUnit.SECONDS));

            start.countDown();

            boolean firstCreated = first.get(20, TimeUnit.SECONDS);
            boolean secondCreated = second.get(20, TimeUnit.SECONDS);

            int successfulCreations =
                    (firstCreated ? 1 : 0) + (secondCreated ? 1 : 0);

            assertEquals(1, successfulCreations);

            Integer persistedUsers = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE email IN (?, ?)",
                    Integer.class,
                    firstEmail,
                    secondEmail
            );

            assertEquals(1, persistedUsers);
        } finally {
            start.countDown();
            executor.shutdownNow();

            jdbcTemplate.update(
                    "DELETE FROM users WHERE email IN (?, ?)",
                    firstEmail,
                    secondEmail
            );
        }
    }

}

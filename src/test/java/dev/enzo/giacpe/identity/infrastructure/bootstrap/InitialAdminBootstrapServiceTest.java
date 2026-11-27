package dev.enzo.giacpe.identity.infrastructure.bootstrap;

import dev.enzo.giacpe.identity.domain.model.ActivationStatus;
import dev.enzo.giacpe.identity.domain.model.User;
import dev.enzo.giacpe.identity.domain.model.UserRole;
import dev.enzo.giacpe.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InitialAdminBootstrapServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private InitialAdminBootstrapService bootstrapService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        bootstrapService = new InitialAdminBootstrapService(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void shouldCreateFirstGlobalAdmin() {
        String name = "Initial Admin";
        String email = "admin@example.com";
        String password = "SecurePassword123!";

        when(userRepository.existsByRole(UserRole.GLOBAL_ADMIN))
                .thenReturn(false);

        when(userRepository.existsByEmail(email))
                .thenReturn(false);

        when(passwordEncoder.encode(password))
                .thenReturn("encoded-password");

        bootstrapService.createInitialAdmin(name, email, password);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        User savedAdmin = captor.getValue();

        assertAll(
                () -> assertEquals(name, savedAdmin.getName()),
                () -> assertEquals(email, savedAdmin.getEmail()),
                () -> assertEquals(
                        UserRole.GLOBAL_ADMIN,
                        savedAdmin.getUserRole()
                ),
                () -> assertEquals(
                        ActivationStatus.ACTIVATED,
                        savedAdmin.getActivationStatus()
                ),
                () -> assertTrue(savedAdmin.isEnabled()),
                () -> assertEquals(
                        "encoded-password",
                        savedAdmin.getPasswordHash()
                )
        );

        verify(passwordEncoder).encode(password);
    }


    @Test
    void shouldRejectBootstrapWhenGlobalAdminAlreadyExists() {
        when(userRepository.existsByRole(UserRole.GLOBAL_ADMIN))
                .thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bootstrapService.createInitialAdmin(
                        "Another Admin",
                        "another@example.com",
                        "SecurePassword123!"
                )
        );

        assertEquals(
                "An initial global administrator already exists",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldRejectBootstrapWhenEmailAlreadyExists() {
        String email = "existing@example.com";

        when(userRepository.existsByRole(UserRole.GLOBAL_ADMIN))
                .thenReturn(false);

        when(userRepository.existsByEmail(email))
                .thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bootstrapService.createInitialAdmin(
                        "Initial Admin",
                        email,
                        "SecurePassword123!"
                )
        );

        assertEquals(
                "Email is already registered",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

}

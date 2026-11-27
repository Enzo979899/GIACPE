package dev.enzo.giacpe.identity.infrastructure.bootstrap;

import dev.enzo.giacpe.identity.domain.model.ActivationStatus;
import dev.enzo.giacpe.identity.domain.model.User;
import dev.enzo.giacpe.identity.domain.model.UserRole;
import dev.enzo.giacpe.identity.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class InitialAdminBootstrapService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialAdminBootstrapService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createInitialAdmin(
            String name,
            String email,
            String password) {

        if (userRepository.existsByRole(UserRole.GLOBAL_ADMIN)) {
            throw new IllegalStateException(
                    "An initial global administrator already exists"
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException(
                    "Email is already registered"
            );
        }

        Instant now = Instant.now();

        User admin = new User(
                UUID.randomUUID(),
                name,
                email,
                passwordEncoder.encode(password),
                UserRole.GLOBAL_ADMIN,
                ActivationStatus.ACTIVATED,
                true,
                now,
                now
        );

        userRepository.save(admin);
    }
}

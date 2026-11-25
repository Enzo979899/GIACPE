package dev.enzo.giacpe.identity.infrastructure.security;

import dev.enzo.giacpe.identity.domain.model.ActivationStatus;
import dev.enzo.giacpe.identity.domain.model.User;
import dev.enzo.giacpe.identity.domain.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static org.springframework.security.core.userdetails.User.withUsername;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean canAuthenticate =
                user.getActivationStatus() == ActivationStatus.ACTIVATED
                        && user.isEnabled();

        String passwordHash = user.getPasswordHash() != null
                ? user.getPasswordHash()
                : "";

        return withUsername(user.getEmail())
                .password(passwordHash)
                .authorities("ROLE_" + user.getUserRole().name())
                .disabled(!canAuthenticate)
                .build();
    }
}

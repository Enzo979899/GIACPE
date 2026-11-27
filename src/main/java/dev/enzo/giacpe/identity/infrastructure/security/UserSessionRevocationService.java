package dev.enzo.giacpe.identity.infrastructure.security;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class UserSessionRevocationService {

    private final SessionRegistry sessionRegistry;

    public UserSessionRevocationService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void revokeAllSessions(String email) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(UserDetails.class::isInstance)
                .map(UserDetails.class::cast)
                .filter(user -> user.getUsername().equals(email))
                .forEach(user -> sessionRegistry
                        .getAllSessions(user, false)
                        .forEach(SessionInformation::expireNow));
    }
}

package dev.enzo.giacpe.identity.infrastructure.security;

import dev.enzo.giacpe.TestcontainersConfiguration;
import dev.enzo.giacpe.identity.domain.model.ActivationStatus;
import dev.enzo.giacpe.identity.domain.model.User;
import dev.enzo.giacpe.identity.domain.model.UserRole;
import dev.enzo.giacpe.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ActiveAccountFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private UserSessionRevocationService sessionRevocationService;

    private String createActiveUser() {
        String email = "active-" + UUID.randomUUID() + "@example.com";
        Instant now = Instant.now();

        User user = new User(
                UUID.randomUUID(),
                "Test User",
                email,
                passwordEncoder.encode("SecurePassword123!"),
                UserRole.FIELD_OFFICER,
                ActivationStatus.ACTIVATED,
                true,
                now,
                now
        );

        userRepository.save(user);

        return email;
    }

    private MockHttpSession login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "SecurePassword123!"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession)
                result.getRequest().getSession(false);

        assertNotNull(session);

        return session;
    }

    @Test
    void shouldAllowActiveUserToKeepUsingSession() throws Exception {
        String email = createActiveUser();

        MockHttpSession session = login(email);

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isOk());

        assertFalse(session.isInvalid());
    }

    @Test
    void shouldInvalidateSessionWhenUserIsDisabled() throws Exception {
        String email = createActiveUser();

        MockHttpSession session = login(email);

        jdbcTemplate.update(
                "UPDATE users SET enabled = false WHERE email = ?",
                email
        );

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isUnauthorized());

        assertTrue(session.isInvalid());
    }


    @Test
    void shouldInvalidateSessionWhenUserRoleChanges() throws Exception {
        String email = createActiveUser();

        MockHttpSession session = login(email);

        jdbcTemplate.update(
                "UPDATE users SET role = ? WHERE email = ?",
                "MANAGER",
                email
        );

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isUnauthorized());

        assertTrue(session.isInvalid());
    }

    @Test
    void shouldInvalidateSessionWhenUserNoLongerExists() throws Exception {
        String email = createActiveUser();

        MockHttpSession session = login(email);

        jdbcTemplate.update(
                "DELETE FROM users WHERE email = ?",
                email
        );

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isUnauthorized());

        assertTrue(session.isInvalid());
    }

    @Test
    void shouldInvalidateAllUserSessionsAfterDisablingAccount() throws Exception {
        String email = createActiveUser();

        MockHttpSession firstSession = login(email);
        MockHttpSession secondSession = login(email);

        assertNotEquals(firstSession.getId(), secondSession.getId());

        jdbcTemplate.update(
                "UPDATE users SET enabled = false WHERE email = ?",
                email
        );

        mockMvc.perform(get("/api/auth/me")
                        .session(firstSession))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me")
                        .session(secondSession))
                .andExpect(status().isUnauthorized());

        assertTrue(firstSession.isInvalid());
        assertTrue(secondSession.isInvalid());
    }

    @Test
    void shouldRegisterMultipleSessionsForSameUser() throws Exception {
        String email = createActiveUser();

        MockHttpSession firstSession = login(email);
        MockHttpSession secondSession = login(email);

        UserDetails principal = sessionRegistry.getAllPrincipals()
                .stream()
                .filter(UserDetails.class::isInstance)
                .map(UserDetails.class::cast)
                .filter(user -> user.getUsername().equals(email))
                .findFirst()
                .orElseThrow();

        var sessions = sessionRegistry.getAllSessions(principal, false);

        assertEquals(2, sessions.size());

        assertTrue(sessions.stream()
                .anyMatch(session ->
                        session.getSessionId().equals(firstSession.getId())));

        assertTrue(sessions.stream()
                .anyMatch(session ->
                        session.getSessionId().equals(secondSession.getId())));
    }

    @Test
    void shouldRejectExpiredSession() throws Exception {
        String email = createActiveUser();

        MockHttpSession session = login(email);

        var sessionInformation = sessionRegistry
                .getSessionInformation(session.getId());

        assertNotNull(sessionInformation);

        sessionInformation.expireNow();

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isUnauthorized());

        assertTrue(session.isInvalid());
    }

    @Test
    void shouldRevokeAllSessionsForUser() throws Exception {
        String email = createActiveUser();

        MockHttpSession firstSession = login(email);
        MockHttpSession secondSession = login(email);

        assertNotEquals(firstSession.getId(), secondSession.getId());

        sessionRevocationService.revokeAllSessions(email);

        var firstSessionInfo =
                sessionRegistry.getSessionInformation(firstSession.getId());

        var secondSessionInfo =
                sessionRegistry.getSessionInformation(secondSession.getId());

        assertAll(
                () -> assertNotNull(firstSessionInfo),
                () -> assertNotNull(secondSessionInfo),
                () -> assertTrue(firstSessionInfo.isExpired()),
                () -> assertTrue(secondSessionInfo.isExpired())
        );

        mockMvc.perform(get("/api/auth/me")
                        .session(firstSession))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me")
                        .session(secondSession))
                .andExpect(status().isUnauthorized());

        assertTrue(firstSession.isInvalid());
        assertTrue(secondSession.isInvalid());
    }

    @Test
    void shouldRevokeOnlyTargetUserSessions() throws Exception {
        String firstUserEmail = createActiveUser();
        String secondUserEmail = createActiveUser();

        MockHttpSession firstUserSession1 = login(firstUserEmail);
        MockHttpSession firstUserSession2 = login(firstUserEmail);
        MockHttpSession secondUserSession = login(secondUserEmail);

        sessionRevocationService.revokeAllSessions(firstUserEmail);

        assertTrue(sessionRegistry
                .getSessionInformation(firstUserSession1.getId())
                .isExpired());

        assertTrue(sessionRegistry
                .getSessionInformation(firstUserSession2.getId())
                .isExpired());

        assertFalse(sessionRegistry
                .getSessionInformation(secondUserSession.getId())
                .isExpired());

        mockMvc.perform(get("/api/auth/me")
                        .session(firstUserSession1))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me")
                        .session(firstUserSession2))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me")
                        .session(secondUserSession))
                .andExpect(status().isOk());

        assertFalse(secondUserSession.isInvalid());
    }
}

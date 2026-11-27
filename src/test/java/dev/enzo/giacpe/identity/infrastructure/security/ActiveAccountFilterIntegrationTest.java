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
}

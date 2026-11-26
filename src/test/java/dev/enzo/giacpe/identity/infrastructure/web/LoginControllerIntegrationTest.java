package dev.enzo.giacpe.identity.infrastructure.web;

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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class LoginControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldLoginAndSaveAuthenticationInSession() throws Exception {
        String password = "SecurePassword123!";
        String email = "login-" + UUID.randomUUID() + "@example.com";

        Instant now = Instant.now();

        User user = new User(
                UUID.randomUUID(),
                "Test User",
                email,
                passwordEncoder.encode(password),
                UserRole.FIELD_OFFICER,
                ActivationStatus.ACTIVATED,
                true,
                now,
                now
        );

        userRepository.save(user);

        var result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        var session = result.getRequest().getSession(false);

        assertNotNull(session);

        SecurityContext context = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );

        assertAll(
                () -> assertNotNull(context),
                () -> assertTrue(context.getAuthentication().isAuthenticated()),
                () -> assertEquals(email, context.getAuthentication().getName())
        );
    }


    @Test
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "unknown@example.com",
                                "password": "WrongPassword123!"
                            }
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLoginWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "test@example.com",
                                "password": "SecurePassword123!"
                            }
                            """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectLoginForPendingUser() throws Exception {
        String email = "pending-" + UUID.randomUUID() + "@example.com";

        User user = User.createPending(
                "Pending User",
                email,
                UserRole.FIELD_OFFICER,
                Instant.now()
        );

        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "%s",
                                "password": "SecurePassword123!"
                            }
                            """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLoginForDisabledUser() throws Exception {
        String email = "disabled-" + UUID.randomUUID() + "@example.com";
        Instant now = Instant.now();

        User user = new User(
                UUID.randomUUID(),
                "Disabled User",
                email,
                passwordEncoder.encode("SecurePassword123!"),
                UserRole.FIELD_OFFICER,
                ActivationStatus.ACTIVATED,
                false,
                now,
                now
        );

        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "%s",
                                "password": "SecurePassword123!"
                            }
                            """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldChangeSessionIdAfterSuccessfulLogin() throws Exception {
        String email = "session-" + UUID.randomUUID() + "@example.com";
        String password = "SecurePassword123!";
        Instant now = Instant.now();

        User user = new User(
                UUID.randomUUID(),
                "Test User",
                email,
                passwordEncoder.encode(password),
                UserRole.FIELD_OFFICER,
                ActivationStatus.ACTIVATED,
                true,
                now,
                now
        );

        userRepository.save(user);

        MockHttpSession originalSession = new MockHttpSession();
        String originalSessionId = originalSession.getId();

        var result = mockMvc.perform(post("/api/auth/login")
                        .session(originalSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "%s",
                                "password": "%s"
                            }
                            """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        var authenticatedSession = result.getRequest().getSession(false);

        assertAll(
                () -> assertNotNull(authenticatedSession),
                () -> assertNotEquals(
                        originalSessionId,
                        authenticatedSession.getId()
                )
        );
    }

    @Test
    void shouldRenewCsrfTokenAfterSuccessfulLogin() throws Exception {
        String email = "csrf-" + UUID.randomUUID() + "@example.com";
        String password = "SecurePassword123!";
        Instant now = Instant.now();

        User user = new User(
                UUID.randomUUID(),
                "Test User",
                email,
                passwordEncoder.encode(password),
                UserRole.FIELD_OFFICER,
                ActivationStatus.ACTIVATED,
                true,
                now,
                now
        );

        userRepository.save(user);

        var csrfResponse = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession)
                csrfResponse.getRequest().getSession(false);

        assertNotNull(session);

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode initialTokenJson = objectMapper.readTree(
                csrfResponse.getResponse().getContentAsString()
        );

        String initialToken = initialTokenJson.get("token").asText();

        var loginResponse = mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .header("X-CSRF-TOKEN", initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "%s",
                                "password": "%s"
                            }
                            """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession authenticatedSession = (MockHttpSession)
                loginResponse.getRequest().getSession(false);

        assertNotNull(authenticatedSession);

        mockMvc.perform(post("/api/auth/login")
                        .session(authenticatedSession)
                        .header("X-CSRF-TOKEN", initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "%s",
                                "password": "%s"
                            }
                            """.formatted(email, password)))
                .andExpect(status().isForbidden());

        var renewedCsrfResponse = mockMvc.perform(get("/api/auth/csrf")
                        .session(authenticatedSession))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode renewedTokenJson = objectMapper.readTree(
                renewedCsrfResponse.getResponse().getContentAsString()
        );

        String renewedToken = renewedTokenJson.get("token").asText();

        assertAll(
                () -> assertFalse(initialToken.isBlank()),
                () -> assertFalse(renewedToken.isBlank()),
                () -> assertNotEquals(initialToken, renewedToken)
        );
    }

    @Test
    void shouldReturnCurrentAuthenticatedUser() throws Exception {
        String email = "current-" + UUID.randomUUID() + "@example.com";
        String password = "SecurePassword123!";
        Instant now = Instant.now();

        User user = new User(
                UUID.randomUUID(),
                "Test User",
                email,
                passwordEncoder.encode(password),
                UserRole.FIELD_OFFICER,
                ActivationStatus.ACTIVATED,
                true,
                now,
                now
        );

        userRepository.save(user);

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "%s",
                                "password": "%s"
                            }
                            """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession)
                loginResult.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_FIELD_OFFICER"));
    }

    @Test
    void shouldRejectCurrentUserRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCompleteLoginAndLogoutFlow() throws Exception {
        String email = "session-flow-" + UUID.randomUUID() + "@example.com";
        String password = "SecurePassword123!";
        Instant now = Instant.now();

        User user = new User(
                UUID.randomUUID(),
                "Test User",
                email,
                passwordEncoder.encode(password),
                UserRole.FIELD_OFFICER,
                ActivationStatus.ACTIVATED,
                true,
                now,
                now
        );

        userRepository.save(user);

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "%s",
                                "password": "%s"
                            }
                            """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession)
                loginResult.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }


}

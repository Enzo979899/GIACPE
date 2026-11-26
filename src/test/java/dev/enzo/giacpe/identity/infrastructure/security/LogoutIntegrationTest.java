package dev.enzo.giacpe.identity.infrastructure.security;

import dev.enzo.giacpe.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class LogoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldInvalidateSessionAfterLogout() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(user("test@example.com")
                                .roles("FIELD_OFFICER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());
    }

    @Test
    void shouldRejectLogoutWithoutCsrfToken() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(user("test@example.com")
                                .roles("FIELD_OFFICER")))
                .andExpect(status().isForbidden());

        assertFalse(session.isInvalid());
    }
}

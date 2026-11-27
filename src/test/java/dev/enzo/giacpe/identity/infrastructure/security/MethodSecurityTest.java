package dev.enzo.giacpe.identity.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.beans.factory.annotation.Autowired;

@SpringJUnitConfig(MethodSecurityTest.TestConfig.class)
class MethodSecurityTest {

    @Autowired
    private TestOperationService operationService;

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {

        @Bean
        TestOperationService testOperationService() {
            return new TestOperationService();
        }
    }

    static class TestOperationService {

        @PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'MANAGER')")
        public String confirmAlert() {
            return "confirmed";
        }
    }

    @Test
    @WithMockUser(roles = "GLOBAL_ADMIN")
    void shouldAllowGlobalAdmin() {
        assertEquals("confirmed", operationService.confirmAlert());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldAllowManager() {
        assertEquals("confirmed", operationService.confirmAlert());
    }

    @Test
    @WithMockUser(roles = "FIELD_OFFICER")
    void shouldRejectFieldOfficer() {
        assertThrows(
                AccessDeniedException.class,
                () -> operationService.confirmAlert()
        );
    }
}

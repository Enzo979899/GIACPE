package dev.enzo.giacpe.identity.infrastructure.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InitialAdminBootstrapRunnerTest {

    private final InitialAdminBootstrapService bootstrapService =
            mock(InitialAdminBootstrapService.class);

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(InitialAdminBootstrapRunner.class)
                    .withBean(
                            InitialAdminBootstrapService.class,
                            () -> bootstrapService
                    );

    @Test
    void shouldNotEnableBootstrapByDefault() {
        contextRunner.run(context -> {
            assertFalse(context.containsBean(
                    "initialAdminBootstrapRunner"
            ));

            verifyNoInteractions(bootstrapService);
        });
    }

    @Test
    void shouldEnableBootstrapWhenExplicitlyRequested() {
        contextRunner
                .withPropertyValues(
                        "giacpe.bootstrap-admin.enabled=true",
                        "GIACPE_BOOTSTRAP_ADMIN_NAME=Initial Admin",
                        "GIACPE_BOOTSTRAP_ADMIN_EMAIL=admin@example.com",
                        "GIACPE_BOOTSTRAP_ADMIN_PASSWORD=SecurePassword123!"
                )
                .run(context -> {
                    assertTrue(context.containsBean(
                            "initialAdminBootstrapRunner"
                    ));

                    InitialAdminBootstrapRunner runner =
                            context.getBean(
                                    InitialAdminBootstrapRunner.class
                            );

                    runner.run(new DefaultApplicationArguments());

                    verify(bootstrapService).createInitialAdmin(
                            "Initial Admin",
                            "admin@example.com",
                            "SecurePassword123!"
                    );
                });
    }
}

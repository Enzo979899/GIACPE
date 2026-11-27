package dev.enzo.giacpe.identity.infrastructure.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "giacpe.bootstrap-admin.enabled",
        havingValue = "true"
)
public class InitialAdminBootstrapRunner implements ApplicationRunner {

    private final InitialAdminBootstrapService bootstrapService;
    private final String name;
    private final String email;
    private final String password;

    public InitialAdminBootstrapRunner(
            InitialAdminBootstrapService bootstrapService,
            @Value("${GIACPE_BOOTSTRAP_ADMIN_NAME}") String name,
            @Value("${GIACPE_BOOTSTRAP_ADMIN_EMAIL}") String email,
            @Value("${GIACPE_BOOTSTRAP_ADMIN_PASSWORD}") String password) {

        this.bootstrapService = bootstrapService;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapService.createInitialAdmin(
                name,
                email,
                password
        );
    }
}

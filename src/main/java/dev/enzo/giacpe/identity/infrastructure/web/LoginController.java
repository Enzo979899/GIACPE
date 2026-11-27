package dev.enzo.giacpe.identity.infrastructure.web;

import dev.enzo.giacpe.identity.infrastructure.web.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfAuthenticationStrategy csrfAuthenticationStrategy;
    private final CsrfTokenRepository csrfTokenRepository;
    private final SessionRegistry sessionRegistry;

    public LoginController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            SessionRegistry sessionRegistry) {

        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.csrfAuthenticationStrategy =
                new CsrfAuthenticationStrategy(csrfTokenRepository);
        this.sessionRegistry = sessionRegistry;
    }

    @PostMapping("/api/auth/login")
    public void login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email(),
                            request.password()
                    )
            );

            httpRequest.getSession(true);
            httpRequest.changeSessionId();
            csrfAuthenticationStrategy.onAuthentication(
                    authentication,
                    httpRequest,
                    httpResponse
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);

            securityContextRepository.saveContext(
                    context,
                    httpRequest,
                    httpResponse
            );

            SecurityContextHolder.setContext(context);

            sessionRegistry.registerNewSession(
                    httpRequest.getSession().getId(),
                    authentication.getPrincipal()
            );

        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid credentials"
            );
        }
    }
}

package dev.enzo.giacpe.identity.infrastructure.security;

import dev.enzo.giacpe.identity.domain.model.ActivationStatus;
import dev.enzo.giacpe.identity.domain.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class ActiveAccountFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public ActiveAccountFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            filterChain.doFilter(request, response);
            return;
        }

        boolean accountValid = userRepository
                .findByEmail(authentication.getName())
                .filter(user ->
                        user.getActivationStatus() == ActivationStatus.ACTIVATED
                                && user.isEnabled()
                                && authentication.getAuthorities().stream()
                                .filter(authority -> authority.getAuthority()
                                        .startsWith("ROLE_"))
                                .map(authority -> authority.getAuthority())
                                .toList()
                                .equals(List.of("ROLE_" + user.getUserRole().name())
                                )
                )
                .isPresent();

        if (!accountValid) {
            var session = request.getSession(false);

            if (session != null) {
                session.invalidate();
            }

            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
